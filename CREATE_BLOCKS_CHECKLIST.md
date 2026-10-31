# Create 6.0.8 Kinetic Blocks — Static Rendering Checklist

Source: decompiled `create-1.20.1-6.0.8.jar` at `../CreateReference/com/simibubi/create/content/kinetics/`.
Scanned via `javap -p` against every top-level `*Block.class` / `*BlockEntity.class` in that package (inner/anonymous classes excluded).

Legend:
- [x] Static override implemented (Block Shape + BER cancel + Flywheel visual cancel, as needed)
- [ ] Not yet handled
- (n/a) Block or BE does not inherit from the kinetic base chain — likely doesn't need the 3-part override, verify case-by-case

## Base Classes (`kinetics/base`)
Abstract classes other blocks build on. No direct registration/blockstate — document only, no override needed themselves.

- `KineticBlock` — extends `Block`, implements `IRotate` (root of the kinetic block hierarchy)
- `KineticBlockEntity` — extends `SmartBlockEntity` (root of the kinetic BE hierarchy)
- `DirectionalKineticBlock` — extends `KineticBlock`
- `DirectionalAxisKineticBlock` — extends `DirectionalKineticBlock`
- `HorizontalKineticBlock` — extends `KineticBlock`
- `HorizontalAxisKineticBlock` — extends `KineticBlock`
- `RotatedPillarKineticBlock` — extends `KineticBlock`
- `AbstractEncasedShaftBlock` — extends `RotatedPillarKineticBlock`
- `GeneratingKineticBlockEntity` — extends `KineticBlockEntity`
- `BlockBreakingKineticBlockEntity` — extends `KineticBlockEntity`
- `DirectionalShaftHalvesBlockEntity` — extends `KineticBlockEntity`

## Simple Relays (`kinetics/simpleRelays`) — DONE
- [x] `ShaftBlock` (extends `AbstractSimpleShaftBlock` → `AbstractShaftBlock` → `RotatedPillarKineticBlock`)
- [x] `CogWheelBlock` (extends `AbstractSimpleShaftBlock`)
- [x] `SimpleKineticBlockEntity` (extends `KineticBlockEntity`)
- [x] `BracketedKineticBlockEntity` (extends `SimpleKineticBlockEntity`) — includes Large Cogwheel. **Bugfix:** `BracketedKineticBlockEntityVisual.create()` builds a private `LargeCogVisual` inner class for large-cog states, whose outer `rotatingModel` was frozen for free by `FlywheelVisualMixin` — but it overrides `update(float)` itself and, after the (frozen) `super.update()` call, unconditionally re-derives a *separate* `additionalShaft` RotatingInstance (the visible center shaft) straight from the live block entity every call, so it kept spinning. Fixed via dedicated `LargeCogVisualMixin` zeroing `additionalShaft.rotationalSpeed` on init and unconditionally on every `update()` call
- [x] `encased/EncasedShaftBlock` (extends `AbstractEncasedShaftBlock`)
- [x] `encased/EncasedCogwheelBlock` (extends `RotatedPillarKineticBlock`) — includes Large Encased Cogwheel. **Bugfix:** `EncasedCogVisual` extends `KineticBlockEntityVisual` directly (not `SingleAxisRotatingVisual`), so none of the generic Flywheel mixins ever reached it — it was spinning completely untouched (its own `rotatingModel` casing stub plus up to two internal `rotatingTopShaft`/`rotatingBottomShaft` half-shaft stubs). Fixed via dedicated `EncasedCogVisualMixin` zeroing all three instances' `rotationalSpeed` on init and unconditionally on every `update()` call
- [x] `AbstractShaftBlock` / `AbstractSimpleShaftBlock` (shared base, covered by above)

## Conveyor Systems — DONE (biggest lag source)

### Belt (`kinetics/belt`)
- [x] `BeltBlock` (extends `HorizontalKineticBlock`) — no RenderShape change; the uncased blockstate model is a bare particle stub with no real geometry, so unlike Shaft/Cogwheel this one is frozen in place rather than swapped to a static JSON model
- [x] `BeltBlockEntity` (extends `KineticBlockEntity`) — drives `BeltVisual` (Flywheel instance, frozen via `BeltVisualMixin`) and `BeltRenderer` (BER, items cancelled via `BeltRendererMixin`)

### Chain Conveyor (`kinetics/chainConveyor`)
- [x] `ChainConveyorBlock` (extends `KineticBlock`) — no RenderShape change; its blockstate model already bakes the static casing normally under `ENTITYBLOCK_ANIMATED`, only the wheel/shaft/chain/packages needed freezing
- [x] `ChainConveyorBlockEntity` (extends `KineticBlockEntity`) — drives `ChainConveyorVisual` (frozen via `ChainConveyorVisualMixin` + `SingleAxisRotatingVisualAccessor`) and `ChainConveyorRenderer` (BER, frozen via `ChainConveyorRendererMixin`)
  - Required carving out an exclusion in the existing `FlywheelVisualMixin` (`SingleAxisRotatingVisual.update()`): `ChainConveyorVisual` is the one subclass of that base, and the pre-existing delete-on-update logic would have wiped its sprocket/boxes/rigging wholesale instead of freezing them in place

### Chain Drive (`kinetics/chainDrive`) — feeds chain conveyors — DONE
- [x] `ChainDriveBlock` (extends `RotatedPillarKineticBlock`) — no dedicated Renderer/Visual class in the package, so it rides the generic `KineticBlockEntityRenderer`/`SingleAxisRotatingVisual` path, same as `ShaftBlock`; doesn't override `getRenderShape()` itself but its casing model (`encased_chain_drive/*`, 24 variants across axis/part) already bakes statically, matching the `EncasedShaftBlock` precedent (no `KineticBlockMixin` entry needed) — cancelled via the generic `KineticBlockEntityRendererMixin` (`instanceof ChainDriveBlock`) + the existing generic `FlywheelVisualMixin`
- [x] `ChainGearshiftBlock` (extends `ChainDriveBlock`) — covered by the same `instanceof ChainDriveBlock` check (subclass) and casing (`adjustable_chain_gearshift_*`, 48 variants incl. powered state)
- [x] `ChainGearshiftBlockEntity` (extends `KineticBlockEntity`) — no dedicated renderer/visual, covered as above

## Transmission (`kinetics/transmission`) — DONE
- [x] `GearboxBlock` (extends `RotatedPillarKineticBlock`) — casing model (`gearbox/block`, 3 axis variants) already bakes the full andesite-cased body statically, so no `KineticBlockMixin` entry needed; `GearboxRenderer` declares its own `renderSafe(GearboxBlockEntity, ...)` override (draws only the two perpendicular `SHAFT_HALF` connector pegs) which shadows the generic injection in `KineticBlockEntityRendererMixin` — cancelled instead via dedicated `GearboxRendererMixin`
- [x] `GearboxBlockEntity` (extends `DirectionalShaftHalvesBlockEntity`) — visualized by `GearboxVisual`, which extends `KineticBlockEntityVisual` (not `SingleAxisRotatingVisual`) so the generic `FlywheelVisualMixin` never sees it; its `keys` `EnumMap<Direction, RotatingInstance>` (the same two connector pegs) is hidden via dedicated `GearboxVisualMixin`
- [x] `ClutchBlock` (extends `GearshiftBlock`) — casing (`clutch/block`, `clutch/block_powered`) already bakes statically; shares `SplitShaftRenderer`/`SplitShaftVisual` with Gearshift/SequencedGearshift below
- [x] `ClutchBlockEntity` (extends `SplitShaftBlockEntity`) — covered by the shared `SplitShaftRendererMixin`/`SplitShaftVisualMixin`
- [x] `GearshiftBlock` (extends `AbstractEncasedShaftBlock`) — casing (`gearshift/block`, `gearshift/block_powered`) already bakes statically, matching `EncasedShaftBlock`
- [x] `GearshiftBlockEntity` (extends `SplitShaftBlockEntity`) — `SplitShaftRenderer` declares its own `renderSafe(SplitShaftBlockEntity, ...)` override (same shadowing issue as `GearboxRenderer`) — cancelled via dedicated `SplitShaftRendererMixin`
- [x] `SplitShaftBlockEntity` (extends `DirectionalShaftHalvesBlockEntity`) — abstract base shared by Clutch/Gearshift/SequencedGearshift, no direct registration; `SplitShaftVisual` extends `KineticBlockEntityVisual` (not `SingleAxisRotatingVisual`), so its `keys` `ArrayList<RotatingInstance>` (the internal half-shaft stub) is hidden via dedicated `SplitShaftVisualMixin`
- [x] `sequencer/SequencedGearshiftBlock` (extends `HorizontalAxisKineticBlock`) — casing (`sequenced_gearshift/idle`, `seq_1`..`seq_5`) already bakes statically; no dedicated renderer/visual of its own
- [x] `sequencer/SequencedGearshiftBlockEntity` (extends `SplitShaftBlockEntity`) — reuses `SplitShaftRenderer`/`SplitShaftVisual`, so it's covered for free by the same two dedicated mixins used for Clutch/Gearshift

## Processing Machines — DONE
- [x] `millstone/MillstoneBlock` (extends `KineticBlock`) — no `getRenderShape()` override, stays `ENTITYBLOCK_ANIMATED`; no dedicated Block mixin needed
- [x] `millstone/MillstoneBlockEntity` (extends `KineticBlockEntity`) — overrides `tick()` and, after calling the (already-cancelled) `super.tick()`, falls into `process()`/`spawnParticles()` with no `isClientSide` check of its own — cancelled via dedicated `MillstoneBlockEntityMixin`; no `MillstoneVisual` class exists (no Flywheel instance beyond the generic connecting-shaft stub, already covered)
- [x] `crusher/CrushingWheelBlock` (extends `RotatedPillarKineticBlock`) — no override needed
- [x] `crusher/CrushingWheelBlockEntity` (extends `KineticBlockEntity`) — no `tick()` override of its own (only `onSpeedChanged`/`fixControllers`/`lazyTick`), so it rides the already-cancelled `KineticBlockEntity#tick()` for free; no dedicated Visual class, its cogwheel-style rotating instance is covered generically by `FlywheelVisualMixin`
- [x] `crusher/CrushingWheelControllerBlock` (n/a — extends `DirectionalBlock`, not kinetic)
- [x] `crusher/CrushingWheelControllerBlockEntity` (n/a — extends `SmartBlockEntity`, not kinetic; entity-crushing/loot logic left untouched, out of scope)
- [x] `mixer/MechanicalMixerBlock` (extends `KineticBlock`) — no override needed
- [x] `mixer/MechanicalMixerBlockEntity` (extends `BasinOperatingBlockEntity`) — **"Static Until In Use" machine (revised architecture, see note below):** fully cancelling `tick()` made the whisk invisible/frozen mid-animation and desynced it from server state. `MechanicalMixerBlockEntityMixin` deleted; `MechanicalMixerBlockEntity` is now exempted from `SmartBlockEntityTickerMixin`/`SmartBlockEntityMixin`/`KineticBlockEntityMixin` (`CreateStaticConfig#isStaticUntilInUseMachine`), so its `tick()` always runs, same as vanilla — `running`/`runningTicks` advance normally and the recipe-start trigger still fires. `MixerVisual`'s own `rotatingModel` (SHAFTLESS_COGWHEEL stub) still stays frozen forever for free via `FlywheelVisualMixin`. Its `mixerHead`/`mixerPole` are now left to position every frame via `animate()`/`beginFrame()` exactly like vanilla; `MixerVisualMixin` only injects at the TAIL of `animate()` to stomp `mixerHead.rotationalSpeed` back to 0 when `mixer.running == false` (vanilla's idle creep), letting it spin freely the instant a recipe starts
- [x] `press/MechanicalPressBlock` (extends `HorizontalKineticBlock`) — no override needed
- [x] `press/MechanicalPressBlockEntity` (extends `BasinOperatingBlockEntity`) — **"Static Until In Use" machine:** does not override `tick()` itself, but was still being blocked by the *generic* `KineticBlockEntityMixin` it rides through — combined with `PressVisualMixin` cancelling `transformModels()`, this left `pressHead` stuck at whatever pose it first captured. `PressVisualMixin` deleted; `MechanicalPressBlockEntity` is now exempted from the generic tick freeze (`CreateStaticConfig#isStaticUntilInUseMachine`), so `PressingBehaviour`'s own ticking (and thus `pressHead`'s live position via `transformModels()`, left completely untouched) runs normally. `PressVisual`'s inherited shaft stub (`ShaftVisual`) still stays frozen forever for free via `FlywheelVisualMixin`
- [x] `saw/SawBlock` (extends `DirectionalAxisKineticBlock`) — no override needed
- [x] `saw/SawBlockEntity` (extends `BlockBreakingKineticBlockEntity`) — overrides `tick()` with no `isClientSide` check anywhere in the override, unconditionally decrementing `ProcessingInventory#remainingTime` every tick — cancelled via dedicated `SawBlockEntityMixin`. `SawVisual` extends `KineticBlockEntityVisual` directly (not `SingleAxisRotatingVisual`, same situation as Gearbox/SplitShaft) with its own `rotatingModel`/`update()` — frozen via dedicated `SawVisualMixin`. **Bugfix:** hovering the crosshair over a placed saw (goggle-overlay lookup triggering a block entity resync, which re-enters `update()` via Flywheel's instance manager) respun it — the original mixin only re-zeroed `rotationalSpeed` on the *first* `update()` call after the static-mode transition (gated behind a `frozen` flag) and just cancelled every call after that without touching the field again, so any re-entry that re-ran `RotatingInstance#setup()` on it before the HEAD injection fired left a nonzero speed uncorrected. `SawVisualMixin` now re-applies the zero unconditionally on every single `update()` call, same shape as `LargeCogVisualMixin`/`EncasedCogVisualMixin`
- [x] `drill/DrillBlock` (extends `DirectionalKineticBlock`) — no override needed
- [x] `drill/DrillBlockEntity` (extends `BlockBreakingKineticBlockEntity`) — **no new mixin needed at all**: doesn't override `tick()` itself, and `BlockBreakingKineticBlockEntity#tick()` calls the already-cancelled `KineticBlockEntity#tick()` then returns early on the client before any breaking-progress logic runs; no dedicated Visual class either (generic shaft stub, covered)
- [x] `deployer/DeployerBlock` (extends `DirectionalAxisKineticBlock`) — no override needed
- [x] `deployer/DeployerBlockEntity` (extends `KineticBlockEntity`) — **"Static Until In Use" machine:** fully cancelling `tick()` froze its State/Mode machine (`timer`/`reach`/`animatedOffset`) mid-motion while the server kept ticking and resyncing, producing the reported twitch. `DeployerBlockEntityMixin` deleted; `DeployerBlockEntity` is now exempted from the generic tick freeze, so it ticks normally (both sides) exactly like vanilla. `DeployerVisual` still needs **no dedicated mixin**: its `beginFrame()`/`tick()` just read that now-correctly-advancing state every frame like before; its inherited shaft stub (`ShaftVisual`) stays frozen forever for free via `FlywheelVisualMixin`
- [x] `crafter/MechanicalCrafterBlock` (extends `HorizontalKineticBlock`) — no override needed
- [x] `crafter/MechanicalCrafterBlockEntity` (extends `KineticBlockEntity`) — **"Static Until In Use" machine:** fully cancelling `tick()` froze its Phase/countdown state machine client-side while the server kept advancing it, so item icons only ever jumped on the sparse NBT resync instead of interpolating - the reported violent twitching during recipes. `MechanicalCrafterBlockEntityMixin` deleted; `MechanicalCrafterBlockEntity` is now exempted from the generic tick freeze, so it ticks normally. No dedicated Visual class exists (the user-reported `MechanicalCrafterVisual` does not exist in `create-6.0.8` - only `MechanicalCrafterRenderer`, a plain BER); grid item icons are drawn directly from the now-correctly-advancing phase/inventory state, and the generic shaft stub remains frozen forever via `FlywheelVisualMixin`
- [x] `mechanicalArm/ArmBlock` (extends `KineticBlock`) — no override needed
- [x] `mechanicalArm/ArmBlockEntity` (extends `KineticBlockEntity`) — **"Static Until In Use" machine:** fully cancelling `tick()` stopped the five `LerpedFloat` joint angles from ever chasing a target, and `ArmVisualMixin` additionally cancelled `animate()` outright - together these made the arm segments invisible/frozen instead of swinging during pickup/deposit. `ArmBlockEntityMixin` and `ArmVisualMixin` both deleted; `ArmBlockEntity` is now exempted from the generic tick freeze, so the joint angles chase normally and `ArmVisual#animate()` is left completely untouched to pose the segments every frame exactly like vanilla (including the jukebox-synced `animateRave()` path). `ArmVisual`'s own `update(float)` override still calls `super.update()` first, so its inherited shaft stub stays frozen forever for free via `FlywheelVisualMixin` regardless of this exemption

> **Architecture note — "Static Until In Use" (Press/Mixer/Deployer/Crafter/Arm):** these five machines' recipe/movement state (and, for Press/Mixer, the very trigger that starts a recipe) is driven from inside their own `tick()`, which also happens to be what the generic `KineticBlockEntityMixin`/`SmartBlockEntityMixin`/`SmartBlockEntityTickerMixin` freeze client-side for every other kinetic block. Freezing it for these five desynced their Flywheel visuals from server state (twitching) and could permanently strand them idle. `CreateStaticConfig#isStaticUntilInUseMachine(BlockEntity)` now exempts all five from those three generic mixins, so their `tick()` always runs, same as vanilla Create - only their *rotating* kinetic parts (shafts/cogs, still handled purely at the Flywheel visual layer) stay frozen forever; their working parts (press head, whisk, deployer hand, arm segments, crafter grid items) animate normally whenever an actual recipe/movement is in progress, and simply hold still (or, for the mixer whisk specifically, get its idle creep zeroed by `MixerVisualMixin`) the rest of the time because nothing is moving.
>
> **Crash postmortem:** the first version of `isStaticUntilInUseMachine` used `blockEntity instanceof MechanicalPressBlockEntity || ...`, which crashed the game on the loading screen with a `ClassCircularityError`. `SmartBlockEntityMixin`/`KineticBlockEntityMixin` are `@Mixin(SmartBlockEntity.class)`/`@Mixin(KineticBlockEntity.class)` - Mixin merges their injected method bodies directly into `SmartBlockEntity`'s/`KineticBlockEntity`'s own classfiles. An `instanceof MechanicalPressBlockEntity` reference baked into that merged bytecode forces the JVM to resolve `MechanicalPressBlockEntity` *while `SmartBlockEntity`/`KineticBlockEntity` is itself still mid-load* - but `MechanicalPressBlockEntity extends BasinOperatingBlockEntity extends KineticBlockEntity extends SmartBlockEntity`, so resolving it immediately re-enters the very class currently being defined. `CreateStaticConfig` now checks `blockEntity.getClass().getSimpleName()` against a `Set<String>` instead, with no import of or symbolic reference to any of the five subclasses anywhere in the base mixins or in `CreateStaticConfig` itself, which breaks the cycle.

## Fans / Air
- [ ] `fan/EncasedFanBlock` (extends `DirectionalKineticBlock`)
- [ ] `fan/EncasedFanBlockEntity` (extends `KineticBlockEntity`)
- [ ] `fan/NozzleBlock` (n/a — extends `WrenchableDirectionalBlock`, not kinetic)
- [ ] `fan/NozzleBlockEntity` (n/a — extends `SmartBlockEntity`, not kinetic)

## Power Generation / Motors
- [x] `waterwheel/WaterWheelBlock` (extends `DirectionalKineticBlock`) — no `getRenderShape()`/tick() override, stays `ENTITYBLOCK_ANIMATED`; no dedicated BE or Block mixin needed
- [x] `waterwheel/WaterWheelBlockEntity` (extends `GeneratingKineticBlockEntity`) — no tick() override, covered for free by `KineticBlockEntityMixin`/`SmartBlockEntityMixin`/`SmartBlockEntityTickerMixin`; its `WaterWheelVisual` (shared with Large, single `rotatingModel` field) frozen via dedicated `WaterWheelVisualMixin`
- [x] `waterwheel/LargeWaterWheelBlock` (extends `RotatedPillarKineticBlock`) — same as WaterWheelBlock, no override needed
- [x] `waterwheel/LargeWaterWheelBlockEntity` (extends `WaterWheelBlockEntity`) — no tick() override of its own either; shares `WaterWheelVisual` (constructed via the `large` factory) with the standard entity, so covered by the same `WaterWheelVisualMixin`
- [ ] `waterwheel/WaterWheelStructuralBlock` (n/a — extends `DirectionalBlock`, not kinetic)
## Bearings (`contraptions/bearing`) — partial
- [x] `WindmillBearingBlock` (extends `BearingBlock`) — no `getRenderShape()` override, stays `ENTITYBLOCK_ANIMATED`; no dedicated Block mixin needed
- [x] `WindmillBearingBlockEntity` (extends `MechanicalBearingBlockEntity`) — declares its own `tick()` (inherited from `MechanicalBearingBlockEntity`, which itself overrides `GeneratingKineticBlockEntity#tick()` without calling up through it), so the generic `KineticBlockEntityMixin`/`SmartBlockEntityMixin` bases never fire for it — cancelled via dedicated `WindmillBearingBlockEntityMixin`; its `BearingVisual` pivot shaft (inherited `rotatingModel` from `OrientedRotatingVisual`, not overridden) frozen via dedicated `OrientedRotatingVisualMixin`. **Bugfix:** `BearingVisual`'s separate `topInstance` (the cap/top plate) is *not* actually left alone by the frozen angle — its `beginFrame()` recomputes the top plate's rotation every render frame from `IBearingBlockEntity#getInterpolatedAngle(partialTick - 1)`, a previous/current lerp across the last tick boundary; with the tick cancelled mid-rotation (or nudged by a server resync packet independent of the frozen client tick), that pair can stay mismatched, and re-interpolating it every frame against a live `partialTick` produced a visible twitch instead of a clean stop. Fixed via dedicated `BearingVisualMixin`: lets `beginFrame()` run normally exactly once after the transition into static mode (capturing wherever the interpolated angle actually is, per the "freeze in place" rule), then cancels every call after that.
- [ ] `MechanicalBearingBlock` / `MechanicalBearingBlockEntity` — shares `WindmillBearingBlockEntityMixin`'s target class ancestor and the same `BearingVisual`/`OrientedRotatingVisualMixin` freeze, but not yet verified/requested directly
- [ ] `ClockworkBearingBlock` / `ClockworkBearingBlockEntity` — separate `tick()` override and its own visual, not yet handled
- [ ] `SailBlock` / `BlankSailBlockItem` (n/a? — the sails themselves move as part of the assembled contraption, not as a standalone kinetic BE; re-verify once Mechanical/Clockwork bearing are handled)

## Motors / Steam Engine / Flywheel / Hand Crank
- [ ] `motor/CreativeMotorBlock` (extends `DirectionalKineticBlock`)
- [ ] `motor/CreativeMotorBlockEntity` (extends `GeneratingKineticBlockEntity`)
- [ ] `steamEngine/SteamEngineBlock` (n/a — extends `FaceAttachedHorizontalDirectionalBlock`, not kinetic)
- [ ] `steamEngine/SteamEngineBlockEntity` (n/a — extends `SmartBlockEntity`, not kinetic)
- [ ] `steamEngine/PoweredShaftBlock` (extends `AbstractShaftBlock`)
- [ ] `steamEngine/PoweredShaftBlockEntity` (extends `GeneratingKineticBlockEntity`)
- [ ] `flywheel/FlywheelBlock` (extends `RotatedPillarKineticBlock`) — *note: this is the Create "Flywheel" machine block, unrelated to the Flywheel (Engine) rendering mod*
- [ ] `flywheel/FlywheelBlockEntity` (extends `KineticBlockEntity`)
- [ ] `crank/HandCrankBlock` (extends `DirectionalKineticBlock`)
- [ ] `crank/HandCrankBlockEntity` (extends `GeneratingKineticBlockEntity`)
- [ ] `crank/ValveHandleBlock` (extends `HandCrankBlock`)
- [ ] `crank/ValveHandleBlockEntity` (extends `HandCrankBlockEntity`)

## Gantry
- [ ] `gantry/GantryShaftBlock` (extends `DirectionalKineticBlock`)
- [ ] `gantry/GantryShaftBlockEntity` (extends `KineticBlockEntity`)

## Speed / Gauges / Misc
- [ ] `speedController/SpeedControllerBlock` (extends `HorizontalAxisKineticBlock`)
- [ ] `speedController/SpeedControllerBlockEntity` (extends `KineticBlockEntity`)
- [ ] `gauge/GaugeBlock` (extends `DirectionalAxisKineticBlock`)
- [ ] `gauge/GaugeBlockEntity` (extends `KineticBlockEntity`, abstract)
- [ ] `gauge/SpeedGaugeBlockEntity` (extends `GaugeBlockEntity`)
- [ ] `gauge/StressGaugeBlockEntity` (extends `GaugeBlockEntity`)
- [ ] `turntable/TurntableBlock` (extends `KineticBlock`)
- [ ] `turntable/TurntableBlockEntity` (extends `KineticBlockEntity`)
- [ ] `clock/CuckooClockBlock` (extends `HorizontalKineticBlock`)
- [ ] `clock/CuckooClockBlockEntity` (extends `KineticBlockEntity`)

---

## Progress Order (recommended)
1. ~~Shaft / Cogwheel~~ ✅ done
2. ~~Belt~~ ✅ done
3. ~~Chain Conveyor~~ ✅ done — **Chain Drive / Chain Gearshift** (the sprockets feeding conveyors) still open, up next
4. Transmission (Gearbox, Clutch, Gearshift, Sequenced Gearshift)
5. Processing machines (Millstone, Crusher, Mixer, Press, Saw, Drill, Deployer, Crafter, Mechanical Arm)
6. Fans, Water Wheels, Motors, Steam Engine, Flywheel, Hand Crank
7. Gantry, Speed Controller, Gauges, Turntable, Cuckoo Clock

Blocks marked `(n/a)` don't inherit the kinetic base chain directly — they may still visually spin via a child BE or Flywheel visual (e.g. `NozzleBlockEntity`, basin-operating machines), so re-verify them individually once their companion block is handled rather than assuming no work is needed.
