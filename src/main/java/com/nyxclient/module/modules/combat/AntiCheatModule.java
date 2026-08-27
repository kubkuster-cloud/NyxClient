package com.nyxclient.module.modules.combat;

import com.nyxclient.NyxClient;
import com.nyxclient.module.Category;
import com.nyxclient.module.Module;
import com.nyxclient.setting.BoolSetting;
import com.nyxclient.setting.DoubleSetting;
import com.nyxclient.setting.IntSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

/**
 * Makes the rotations and attack timing the other combat modules produce look like they came from a
 * mouse instead of from code, and backs off when the server pushes back.
 *
 * <p>Three things give a synthetic rotation away, and this module addresses each:
 *
 * <ul>
 *   <li><b>Quantization.</b> Vanilla turns raw cursor counts into degrees through a fixed chain -
 *       {@code f = sensitivity * 0.6 + 0.2}, then {@code yaw += counts * f^3 * 8.0 * 0.15}. Because
 *       the count is an integer, every rotation a real mouse can produce is an exact multiple of
 *       {@code f^3 * 1.2}. Code that writes an arbitrary float lands between those steps, which is
 *       cheap to check server-side. {@link #applyLook} rounds every delta it writes onto that same
 *       lattice, so the deltas stay shaped like mouse output.
 *   <li><b>Instantaneity.</b> Killaura's "Face target" snaps the full angle in one tick. The turn is
 *       capped here at a degrees-per-tick budget instead; since Killaura recomputes an absolute
 *       target every tick, the remainder is picked up on the following ticks without any carry-over
 *       bookkeeping, and the turn arrives over several ticks the way a hand movement does.
 *   <li><b>Perfect aim and perfect periodicity.</b> A small Gaussian offset keeps the view from
 *       resting exactly on the target's center, and {@link #jitterCooldown} spreads Killaura's fixed
 *       tick cooldown so attacks stop arriving on an exact period.
 * </ul>
 *
 * <p>The capped turn creates a problem of its own: for the first few ticks of a turn the view has
 * not reached the target yet, and swinging at something you are not looking at is itself a flag. So
 * Killaura asks {@link #isAimedAt} before attacking and waits instead of swinging early.
 *
 * <p>Finally, a PlayerPositionLookS2CPacket that lands near where the player already is (as opposed
 * to far away, which is an ordinary teleport) means the server rejected the client's movement and
 * dragged it back. {@link #notifySetback} pauses the combat modules for a configurable window and
 * can switch the movement modules off outright.
 *
 * <p>With "Auto tune" on, that same signal also walks the settings themselves: each setback moves
 * every parameter a third of the way toward its cautious end (slower turns, more tremor, a stricter
 * aim gate, looser attack timing) and quiet time bleeds it back toward whatever was configured. The
 * module therefore settles wherever the server has most recently tolerated it rather than at a
 * number guessed in advance.
 *
 * <p>Note what that loop can and cannot see. A setback is the only detection signal a vanilla client
 * receives, and most rotation and combat checks do not produce one - they raise an alert on the
 * server for staff to look at, which is invisible here. So quiet means "no movement check has
 * rejected us", never "nothing has noticed us", and the tuning level should be read as a record of
 * server pushback rather than as a measure of how well anything is working.
 */
public class AntiCheatModule extends Module {

	private static final Random RANDOM = new Random();

	/** Set in the constructor; the static entry points below read settings through it. */
	private static AntiCheatModule instance;

	public static volatile boolean active = false;

	private final BoolSetting snapToGcd = addSetting(new BoolSetting("Mouse GCD", true));
	private final DoubleSetting maxTurn = addSetting(new DoubleSetting("Max turn", 22.0, 1.0, 180.0));
	private final DoubleSetting jitter = addSetting(new DoubleSetting("Aim jitter", 0.8, 0.0, 5.0));
	private final DoubleSetting aimTolerance = addSetting(new DoubleSetting("Attack tolerance", 8.0, 1.0, 60.0));
	private final IntSetting attackJitter = addSetting(new IntSetting("Attack jitter", 3, 0, 10));
	private final BoolSetting backOff = addSetting(new BoolSetting("Back off on setback", true));
	private final IntSetting backOffTicks = addSetting(new IntSetting("Backoff ticks", 60, 0, 200));
	private final BoolSetting disableMovement = addSetting(new BoolSetting("Kill movement on setback", true));
	private final BoolSetting announce = addSetting(new BoolSetting("Announce setbacks", true));
	private final BoolSetting autoTune = addSetting(new BoolSetting("Auto tune", true));
	private final IntSetting relaxSeconds = addSetting(new IntSetting("Relax seconds", 45, 5, 300));

	/**
	 * How far the module has walked its own settings toward the cautious end: 0 uses the values above
	 * exactly as configured, 1 uses {@link #SAFE_MAX_TURN} and friends. Each setback pushes it up,
	 * quiet ticks bleed it back down, so the module ends up sitting at whatever the server has most
	 * recently tolerated instead of at a number picked by hand.
	 */
	private static volatile double tuning = 0.0;

	/** The cautious end of each range: slow turns, visible tremor, strict aim gate, loose timing. */
	private static final double SAFE_MAX_TURN = 6.0;
	private static final double SAFE_JITTER = 2.0;
	private static final double SAFE_TOLERANCE = 2.5;
	private static final int SAFE_ATTACK_JITTER = 6;

	/** One setback moves a third of the way to cautious, so three in a row pin it there. */
	private static final double SETBACK_STEP = 0.34;

	/** Ticks left in a post-setback pause. Written from the packet handler, read from the tick loop. */
	private static volatile int suppressTicks = 0;

	/** Last position seen by the tick loop, used to tell a setback apart from a real teleport. */
	private static volatile Vec3d lastTickPos = Vec3d.ZERO;

	public AntiCheatModule() {
		super("AntiCheat", "Shapes the rotations and attack timing of the other combat modules to match what a real mouse produces, and pauses them when the server sends a setback. Intended for private servers you have permission to test on.", Category.COMBAT);
		instance = this;
	}

	@Override
	protected void onEnable() {
		active = true;
		suppressTicks = 0;
		// Start from the configured values again: what the last server tolerated says nothing about
		// this one, and re-enabling by hand is the clearest signal of a fresh start available here.
		tuning = 0.0;
	}

	@Override
	protected void onDisable() {
		active = false;
		suppressTicks = 0;
	}

	@Override
	public void onTick() {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		if (player == null) return;

		lastTickPos = player.getPos();
		if (suppressTicks > 0) suppressTicks--;

		// Bleed back toward the configured values while nothing is pushing back, so a single bad
		// moment does not leave the module crawling for the rest of the session.
		if (autoTune.get() && tuning > 0.0) {
			tuning = Math.max(0.0, tuning - 1.0 / (relaxSeconds.get() * 20.0));
		}
	}

	/** Interpolates between the configured value and the cautious one by the current tuning level. */
	private double blend(double configured, double safe) {
		if (!autoTune.get()) return configured;
		return configured + (safe - configured) * tuning;
	}

	private float effectiveMaxTurn() {
		return (float) blend(maxTurn.get(), SAFE_MAX_TURN);
	}

	private double effectiveJitter() {
		return blend(jitter.get(), SAFE_JITTER);
	}

	private double effectiveTolerance() {
		return blend(aimTolerance.get(), SAFE_TOLERANCE);
	}

	private int effectiveAttackJitter() {
		return (int) Math.round(blend(attackJitter.get(), SAFE_ATTACK_JITTER));
	}

	/** 0-100 readout of how far into the cautious end the module has tuned itself. */
	public static int tuningPercent() {
		return (int) Math.round(tuning * 100.0);
	}

	/**
	 * The smallest rotation step the player's current sensitivity can produce. Vanilla scales raw
	 * cursor counts by {@code f^3 * 8.0} and then by {@code 0.15} inside changeLookDirection, so a
	 * cursor count of 1 moves the view exactly this far.
	 */
	private static double mouseGcd() {
		MinecraftClient client = MinecraftClient.getInstance();
		double sensitivity = client.options.getMouseSensitivity().getValue();
		double f = sensitivity * 0.6 + 0.2;
		return f * f * f * 8.0 * 0.15;
	}

	/** Rounds a delta onto the mouse lattice. Deltas below half a step round to zero, as a real mouse's would. */
	private static float snap(float delta, double gcd) {
		if (gcd <= 1.0e-6) return delta;
		return (float) (Math.round(delta / gcd) * gcd);
	}

	/**
	 * Points the view at (yaw, pitch), rate-limited, jittered and quantized. Callers pass the angle
	 * they actually want; what gets written is as much of it as this tick's budget allows. With the
	 * module off this is a plain setYaw/setPitch, so callers need no branch of their own.
	 */
	public static void applyLook(ClientPlayerEntity player, float targetYaw, float targetPitch) {
		float clampedTarget = MathHelper.clamp(targetPitch, -90.0f, 90.0f);

		AntiCheatModule m = instance;
		if (m == null || !active) {
			player.setYaw(targetYaw);
			player.setPitch(clampedTarget);
			return;
		}

		float currentYaw = player.getYaw();
		float currentPitch = player.getPitch();

		float deltaYaw = MathHelper.wrapDegrees(targetYaw - currentYaw);
		float deltaPitch = clampedTarget - currentPitch;

		double spread = m.effectiveJitter();
		if (spread > 0.0) {
			// Applied to the delta rather than to the stored target, so it stays a per-tick tremor
			// instead of accumulating into a drift away from the target.
			deltaYaw += (float) (RANDOM.nextGaussian() * spread);
			deltaPitch += (float) (RANDOM.nextGaussian() * spread * 0.5);
		}

		float cap = m.effectiveMaxTurn();
		deltaYaw = MathHelper.clamp(deltaYaw, -cap, cap);
		deltaPitch = MathHelper.clamp(deltaPitch, -cap, cap);

		if (m.snapToGcd.get()) {
			double gcd = mouseGcd();
			deltaYaw = snap(deltaYaw, gcd);
			deltaPitch = snap(deltaPitch, gcd);
		}

		player.setYaw(currentYaw + deltaYaw);
		player.setPitch(MathHelper.clamp(currentPitch + deltaPitch, -90.0f, 90.0f));
	}

	/**
	 * Whether the view has converged closely enough on the target to justify an attack. Always true
	 * with the module off, so Killaura keeps its original behaviour.
	 */
	public static boolean isAimedAt(ClientPlayerEntity player, Entity target) {
		AntiCheatModule m = instance;
		if (m == null || !active) return true;

		double dx = target.getX() - player.getX();
		double dy = target.getBodyY(0.5) - player.getEyeY();
		double dz = target.getZ() - player.getZ();
		double horizontal = Math.sqrt(dx * dx + dz * dz);

		float wantYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
		float wantPitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));

		double yawOff = MathHelper.wrapDegrees(wantYaw - player.getYaw());
		double pitchOff = wantPitch - player.getPitch();

		return Math.sqrt(yawOff * yawOff + pitchOff * pitchOff) <= m.effectiveTolerance();
	}

	/** Spreads a fixed tick cooldown so repeated actions stop landing on an exact period. */
	public static int jitterCooldown(int baseTicks) {
		AntiCheatModule m = instance;
		if (m == null || !active) return baseTicks;

		int spread = m.effectiveAttackJitter();
		if (spread <= 0) return baseTicks;

		return Math.max(1, baseTicks + RANDOM.nextInt(spread * 2 + 1) - spread);
	}

	/** True while a post-setback pause is running; the combat modules sit out these ticks. */
	public static boolean isSuppressed() {
		return active && suppressTicks > 0;
	}

	/**
	 * Called from ClientPlayNetworkHandlerMixin for every position-look packet. Distance is what
	 * separates the two cases: a rejected-movement correction lands the player near where they
	 * already were, while a portal, a respawn or an operator teleport moves them a long way.
	 */
	public static void notifySetback(Vec3d correctedPos) {
		AntiCheatModule m = instance;
		if (m == null || !active) return;

		if (correctedPos.squaredDistanceTo(lastTickPos) > 64.0) return;

		// Tuning tracks setbacks whether or not the pause is switched on: it is a measurement of how
		// the server is reacting, not part of the reaction.
		if (m.autoTune.get()) {
			tuning = Math.min(1.0, tuning + SETBACK_STEP);
		}

		if (!m.backOff.get()) return;

		suppressTicks = m.backOffTicks.get();

		if (m.disableMovement.get()) {
			disable("Fly");
			disable("Speed");
			disable("NoFall");
			disable("Scaffold");
			disable("Jesus");
		}

		if (m.announce.get()) {
			ClientPlayerEntity player = MinecraftClient.getInstance().player;
			if (player != null) {
				player.sendMessage(Text.literal("[Nyx] Setback received - pausing for " + suppressTicks
						+ " ticks, tuning at " + tuningPercent() + "%"), false);
			}
		}
	}

	private static void disable(String moduleName) {
		Module module = NyxClient.moduleManager.getModule(moduleName);
		if (module != null && module.isEnabled()) {
			module.setEnabled(false);
		}
	}
}
