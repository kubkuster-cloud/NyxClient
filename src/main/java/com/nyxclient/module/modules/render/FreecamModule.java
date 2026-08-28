package com.nyxclient.module.modules.render;

import com.nyxclient.module.Category;
import com.nyxclient.module.Module;
import com.nyxclient.setting.DoubleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MarkerEntity;

/**
 * Detaches the view onto a camera the player flies around while their body stays put.
 *
 * <p>The camera is a marker entity that is never added to the world: nothing ticks it, nothing
 * renders it, and no server ever hears about it - it exists only to be something
 * {@link MinecraftClient#setCameraEntity} can point at. Because it is outside the world it also
 * ignores collision, which is the whole point of a freecam.
 *
 * <p>A marker specifically, and not an armor stand or a fake player: Camera reads its rotation
 * through getYaw(tickDelta), and LivingEntity overrides that to return headYaw - a field
 * changeLookDirection never writes. A living camera therefore takes pitch from the mouse and leaves
 * yaw stuck on whatever headYaw happened to hold. Marker extends Entity directly, so both axes come
 * from the fields the mouse actually moves.
 *
 * <p>Two mixins do what this class cannot reach from a tick callback: KeyboardInputMixin blanks the
 * player's movement input so walking keys drive the camera instead of the body, and MouseMixin
 * steers the camera rather than the player, so no rotation is sent to the server either. What the
 * server sees is a player standing still and looking one way.
 */
public class FreecamModule extends Module {

	// Read from the render thread (MouseMixin) and the input tick (KeyboardInputMixin), written from
	// the client tick thread.
	public static volatile boolean active = false;

	private static volatile Entity camera;

	private final DoubleSetting speed = addSetting(new DoubleSetting("Speed", 0.5, 0.05, 3.0));
	private final DoubleSetting boost = addSetting(new DoubleSetting("Boost", 3.0, 1.0, 10.0));

	public FreecamModule() {
		super("Freecam", "Flies the camera away from your body, which stays where it was and keeps standing still to the server. Movement keys fly the camera, jump/sneak raise and lower it, sprint applies the boost multiplier.", Category.RENDER);
	}

	/** Called by MouseMixin in place of rotating the player. */
	public static void steer(double cursorDeltaX, double cursorDeltaY) {
		Entity cam = camera;
		if (cam != null) {
			// Entity's own method keeps prevYaw/prevPitch in step with the new rotation, so the
			// camera doesn't smear a tick behind the mouse.
			cam.changeLookDirection(cursorDeltaX, cursorDeltaY);
		}
	}

	@Override
	protected void onEnable() {
		attach();
	}

	@Override
	protected void onDisable() {
		detach();
	}

	@Override
	public void onTick() {
		// A config loaded at startup can switch this on before any world exists, and changing worlds
		// strands the camera in the old one - both are handled by attaching late rather than
		// insisting onEnable had a world to work with.
		Entity cam = camera;
		MinecraftClient client = MinecraftClient.getInstance();
		if (cam == null || client.world == null || cam.getWorld() != client.world) {
			detach();
			attach();
			return;
		}

		fly(client, cam);
	}

	private void attach() {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity player = client.player;
		if (player == null || client.world == null) return;

		MarkerEntity cam = new MarkerEntity(EntityType.MARKER, client.world);

		// A marker has no size and so no eye offset: its position is the viewpoint outright, which
		// is why it starts at the player's eyes rather than their feet - otherwise switching in
		// would drop the view by the player's eye height.
		cam.setPosition(player.getX(), player.getEyeY(), player.getZ());
		cam.setYaw(player.getYaw());
		cam.setPitch(player.getPitch());
		markStartOfTick(cam);

		camera = cam;
		active = true;
		client.setCameraEntity(cam);
	}

	private void detach() {
		active = false;
		camera = null;

		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player != null) {
			client.setCameraEntity(client.player);
		}
	}

	private void fly(MinecraftClient client, Entity cam) {
		GameOptions options = client.options;
		double forward = axis(options.forwardKey.isPressed(), options.backKey.isPressed());
		double strafe = axis(options.rightKey.isPressed(), options.leftKey.isPressed());
		double vertical = axis(options.jumpKey.isPressed(), options.sneakKey.isPressed());

		// Yaw 0 faces +Z, so forward is (-sin, cos) and the player's right hand is a further quarter
		// turn clockwise at (-cos, -sin).
		double yawRadians = Math.toRadians(cam.getYaw());
		double sin = Math.sin(yawRadians);
		double cos = Math.cos(yawRadians);

		double dx = -forward * sin - strafe * cos;
		double dz = forward * cos - strafe * sin;

		// Holding two directions at once shouldn't move the camera faster than holding one.
		double horizontal = Math.sqrt(dx * dx + dz * dz);
		if (horizontal > 1.0) {
			dx /= horizontal;
			dz /= horizontal;
		}

		double step = speed.get() * (options.sprintKey.isPressed() ? boost.get() : 1.0);

		markStartOfTick(cam);
		cam.setPosition(cam.getX() + dx * step, cam.getY() + vertical * step, cam.getZ() + dz * step);
	}

	private static double axis(boolean positive, boolean negative) {
		return (positive ? 1.0 : 0.0) - (negative ? 1.0 : 0.0);
	}

	/**
	 * Camera renders between an entity's previous and current position, and nothing ticks this one
	 * into recording where it was, so each move records it here first - otherwise every frame in the
	 * tick interpolates from wherever the camera last happened to be left.
	 */
	private static void markStartOfTick(Entity cam) {
		cam.prevX = cam.getX();
		cam.prevY = cam.getY();
		cam.prevZ = cam.getZ();
		cam.prevYaw = cam.getYaw();
		cam.prevPitch = cam.getPitch();
	}
}
