package com.createstatic;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.Set;

/**
 * Client-side {@link ForgeConfigSpec} for CreateStatic: a master toggle (also flipped in-game via
 * F9, see {@link CreateStaticClientEvents}) plus granular per-category toggles. Every Mixin that
 * freezes a kinetic block checks one of the {@code isXFrozen()} helpers below instead of a raw
 * boolean, so disabling a category in the config (or the Cloth Config screen) leaves that category's
 * blocks rendering/ticking exactly like vanilla Create while every other category stays frozen.
 *
 * The three base-class "Hard Logic Cull" mixins (KineticBlockEntityMixin, SmartBlockEntityMixin,
 * SmartBlockEntityTickerMixin) deliberately check only {@link #isMasterEnabled()}, not a category:
 * they're injected into shared ancestor classes (KineticBlockEntity/SmartBlockEntity) that cover
 * every category at once, and resolving "which category does this instance belong to" there would
 * require an `instanceof` check against a concrete subclass symbol baked into that shared ancestor's
 * own merged bytecode - the exact ClassCircularityError shape documented below for
 * STATIC_UNTIL_IN_USE_CLASS_NAMES. Category granularity instead lives at the Flywheel Visual layer
 * and the dedicated per-block BlockEntity mixins, which each target one concrete class and can safely
 * call a specific isXFrozen() helper.
 */
public class CreateStaticConfig {

    public static final String MOD_ID = "createstatic";

    public static final ForgeConfigSpec CLIENT_SPEC;

    public static final ForgeConfigSpec.BooleanValue MASTER_TOGGLE;
    public static final ForgeConfigSpec.BooleanValue FREEZE_BELTS;
    public static final ForgeConfigSpec.BooleanValue FREEZE_RELAYS;
    public static final ForgeConfigSpec.BooleanValue FREEZE_TRANSMISSIONS;
    public static final ForgeConfigSpec.BooleanValue FREEZE_PROCESSING;
    public static final ForgeConfigSpec.BooleanValue FREEZE_GENERATORS;
    public static final ForgeConfigSpec.BooleanValue FREEZE_MISC;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("general");
        MASTER_TOGGLE = builder
                .comment("Master switch for CreateStatic. Also bound to the F9 key in-game.",
                        "When false, every kinetic block renders and ticks exactly like vanilla Create.")
                .define("masterToggle", true);
        builder.pop();

        builder.push("categories");
        FREEZE_BELTS = builder
                .comment("Freeze Belts, Chain Conveyors and Frogport Chains.")
                .define("freezeBelts", true);
        FREEZE_RELAYS = builder
                .comment("Freeze Shafts, Cogwheels and other plain rotation relays.")
                .define("freezeRelays", true);
        FREEZE_TRANSMISSIONS = builder
                .comment("Freeze Gearboxes, Gearshifts, Clutches and Encased Chain Drives.")
                .define("freezeTransmissions", true);
        FREEZE_PROCESSING = builder
                .comment("Freeze processing machines: Mechanical Mixers, Saws and Millstones.")
                .define("freezeProcessing", true);
        FREEZE_GENERATORS = builder
                .comment("Freeze power generators: Water Wheels and Windmill Bearings.")
                .define("freezeGenerators", true);
        FREEZE_MISC = builder
                .comment("Freeze everything else: Fans, Gauges, Hand Cranks, Flywheels,",
                        "Mechanical/Clockwork Bearings and Item Vaults.")
                .define("freezeMisc", true);
        builder.pop();

        CLIENT_SPEC = builder.build();
    }

    public static boolean isMasterEnabled() {
        return MASTER_TOGGLE.get();
    }

    public static boolean isBeltsFrozen() {
        return MASTER_TOGGLE.get() && FREEZE_BELTS.get();
    }

    public static boolean isRelaysFrozen() {
        return MASTER_TOGGLE.get() && FREEZE_RELAYS.get();
    }

    public static boolean isTransmissionsFrozen() {
        return MASTER_TOGGLE.get() && FREEZE_TRANSMISSIONS.get();
    }

    public static boolean isProcessingFrozen() {
        return MASTER_TOGGLE.get() && FREEZE_PROCESSING.get();
    }

    public static boolean isGeneratorsFrozen() {
        return MASTER_TOGGLE.get() && FREEZE_GENERATORS.get();
    }

    public static boolean isMiscFrozen() {
        return MASTER_TOGGLE.get() && FREEZE_MISC.get();
    }

    /**
     * "Static Until In Use" machines: Mechanical Press, Mechanical Mixer, Deployer, Mechanical
     * Crafter and Mechanical Arm. Their own tick() (and, for Mixer/Press, the shared
     * BasinOperatingBlockEntity/KineticBlockEntity tick() they ride through) must NOT be cancelled
     * by the generic SmartBlockEntityTickerMixin/SmartBlockEntityMixin/KineticBlockEntityMixin
     * client-side freeze, unlike every other kinetic block entity - the recipe/movement state that
     * drives their Flywheel visuals (press head, whisk, deployer hand, arm segments) is advanced
     * inside tick() itself, and the trigger that flips them from idle into an active recipe
     * (DeferralBehaviour-driven basin/recipe scanning) also only ever runs there. Freezing tick()
     * unconditionally - as every other kinetic block entity still is - would both desync their
     * client-side animation from server state (causing the reported twitching) and permanently
     * strand them idle, since nothing would ever re-check for a recipe to start. Their *rotating*
     * kinetic parts (shafts/cogs) stay frozen regardless, purely at the Flywheel visual layer
     * (FlywheelVisualMixin and friends), independent of this tick exemption.
     *
     * Also covers four blocks whose own working animation would otherwise glitch: Steam Engine
     * (piston/linkage, driven by SteamEngineVisual#animate() off live tick() state), Hand Crank and
     * Valve Handle (the crank handle only swings while independentAngle is advancing during use), and
     * Cuckoo Clock (hourHand/minuteHand/door animation - a stopped clock is itself the bug). Their
     * generic rotating shaft stubs, where present, are still frozen at the Flywheel visual layer
     * exactly like everything else.
     *
     * CRASH/DESYNC POSTMORTEM (EncasedFanBlockEntity): originally given its own dedicated
     * EncasedFanBlockEntityMixin cancelling tick() unconditionally, same shape as Millstone/Saw - but
     * unlike those, EncasedFanBlockEntity#tick() isn't just driving a cosmetic countdown. It's what
     * (re)builds AirCurrent every tick, and Create's player-pushing/item-pushing is done via *client-
     * side physics prediction* reading that AirCurrent locally, not purely server-authoritative motion
     * - freezing tick() stopped the client from ever recomputing it, so the client no longer predicted
     * the push at all (players stopped feeling pushed) while the server kept applying it, and dropped
     * items visibly fought between the two every tick (the "violent twitch" correction). That dedicated
     * mixin has been deleted and EncasedFanBlockEntity is exempted here instead, so its tick() (and
     * thus AirCurrent) runs identically to vanilla on the client. NozzleBlockEntity is exempted
     * alongside it for the same reason - it redirects/reshapes the fan's AirCurrent into its own
     * pushing-entities list every tick, the same client-prediction-critical computation. Neither block
     * has a rotating kinetic part gated by this exemption: the fan's rotor/shaft stay frozen
     * regardless, purely at the Flywheel visual layer (FanVisualMixin), and NozzleBlockEntity has no
     * RotatingInstance at all.
     *
     * CRITICAL: this check used to be `blockEntity instanceof MechanicalPressBlockEntity || ...`,
     * which crashed the game on boot with a ClassCircularityError. SmartBlockEntityMixin and
     * KineticBlockEntityMixin are @Mixin(SmartBlockEntity.class)/@Mixin(KineticBlockEntity.class) -
     * their injected method bodies get merged directly into SmartBlockEntity's/KineticBlockEntity's
     * own classfiles. An `instanceof MechanicalPressBlockEntity` reference inside that merged
     * bytecode forces the JVM to resolve and load MechanicalPressBlockEntity while
     * SmartBlockEntity/KineticBlockEntity is itself still in the middle of loading - but
     * MechanicalPressBlockEntity extends BasinOperatingBlockEntity extends KineticBlockEntity
     * extends SmartBlockEntity, so that load immediately needs SmartBlockEntity/KineticBlockEntity
     * again, which the classloader is already in the process of defining. That's a textbook
     * circular class load, not just a style nit - using `getClass().getSimpleName()` string
     * comparison instead means SmartBlockEntity/KineticBlockEntity's merged bytecode (and this
     * class, which they transitively load) never contains a symbolic reference to any of these
     * subclasses at all, so loading them never re-enters the base class's own loading.
     */
    private static final Set<String> STATIC_UNTIL_IN_USE_CLASS_NAMES = Set.of(
            "MechanicalPressBlockEntity",
            "MechanicalMixerBlockEntity",
            "DeployerBlockEntity",
            "MechanicalCrafterBlockEntity",
            "ArmBlockEntity",
            "SteamEngineBlockEntity",
            "HandCrankBlockEntity",
            "ValveHandleBlockEntity",
            "CuckooClockBlockEntity",
            "EncasedFanBlockEntity",
            "NozzleBlockEntity"
    );

    public static boolean isStaticUntilInUseMachine(BlockEntity blockEntity) {
        if (blockEntity == null) {
            return false;
        }
        return STATIC_UNTIL_IN_USE_CLASS_NAMES.contains(blockEntity.getClass().getSimpleName());
    }
}
