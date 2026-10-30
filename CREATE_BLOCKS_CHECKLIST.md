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
- [x] `BracketedKineticBlockEntity` (extends `SimpleKineticBlockEntity`) — includes Large Cogwheel
- [x] `encased/EncasedShaftBlock` (extends `AbstractEncasedShaftBlock`)
- [x] `encased/EncasedCogwheelBlock` (extends `RotatedPillarKineticBlock`)
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

## Processing Machines
- [ ] `millstone/MillstoneBlock` (extends `KineticBlock`)
- [ ] `millstone/MillstoneBlockEntity` (extends `KineticBlockEntity`)
- [ ] `crusher/CrushingWheelBlock` (extends `RotatedPillarKineticBlock`)
- [ ] `crusher/CrushingWheelBlockEntity` (extends `KineticBlockEntity`)
- [ ] `crusher/CrushingWheelControllerBlock` (n/a — extends `DirectionalBlock`, not kinetic)
- [ ] `crusher/CrushingWheelControllerBlockEntity` (n/a — extends `SmartBlockEntity`, not kinetic)
- [ ] `mixer/MechanicalMixerBlock` (extends `KineticBlock`)
- [ ] `mixer/MechanicalMixerBlockEntity` (n/a? — extends `BasinOperatingBlockEntity`, not directly `KineticBlockEntity`; verify if it still spins visually)
- [ ] `press/MechanicalPressBlock` (extends `HorizontalKineticBlock`)
- [ ] `press/MechanicalPressBlockEntity` (n/a? — extends `BasinOperatingBlockEntity`; verify)
- [ ] `saw/SawBlock` (extends `DirectionalAxisKineticBlock`)
- [ ] `saw/SawBlockEntity` (extends `BlockBreakingKineticBlockEntity`)
- [ ] `drill/DrillBlock` (extends `DirectionalKineticBlock`)
- [ ] `drill/DrillBlockEntity` (extends `BlockBreakingKineticBlockEntity`)
- [ ] `deployer/DeployerBlock` (extends `DirectionalAxisKineticBlock`)
- [ ] `deployer/DeployerBlockEntity` (extends `KineticBlockEntity`)
- [ ] `crafter/MechanicalCrafterBlock` (extends `HorizontalKineticBlock`)
- [ ] `crafter/MechanicalCrafterBlockEntity` (extends `KineticBlockEntity`)
- [ ] `mechanicalArm/ArmBlock` (extends `KineticBlock`)
- [ ] `mechanicalArm/ArmBlockEntity` (extends `KineticBlockEntity`)

## Fans / Air
- [ ] `fan/EncasedFanBlock` (extends `DirectionalKineticBlock`)
- [ ] `fan/EncasedFanBlockEntity` (extends `KineticBlockEntity`)
- [ ] `fan/NozzleBlock` (n/a — extends `WrenchableDirectionalBlock`, not kinetic)
- [ ] `fan/NozzleBlockEntity` (n/a — extends `SmartBlockEntity`, not kinetic)

## Power Generation / Motors
- [ ] `waterwheel/WaterWheelBlock` (extends `DirectionalKineticBlock`)
- [ ] `waterwheel/WaterWheelBlockEntity` (extends `GeneratingKineticBlockEntity`)
- [ ] `waterwheel/LargeWaterWheelBlock` (extends `RotatedPillarKineticBlock`)
- [ ] `waterwheel/LargeWaterWheelBlockEntity` (extends `WaterWheelBlockEntity`)
- [ ] `waterwheel/WaterWheelStructuralBlock` (n/a — extends `DirectionalBlock`, not kinetic)
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
