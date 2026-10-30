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

### Chain Drive (`kinetics/chainDrive`) — feeds chain conveyors
- [ ] `ChainDriveBlock` (extends `RotatedPillarKineticBlock`)
- [ ] `ChainGearshiftBlock` (extends `ChainDriveBlock`)
- [ ] `ChainGearshiftBlockEntity` (extends `KineticBlockEntity`)

## Transmission (`kinetics/transmission`)
- [ ] `GearboxBlock` (extends `RotatedPillarKineticBlock`)
- [ ] `GearboxBlockEntity` (extends `DirectionalShaftHalvesBlockEntity`)
- [ ] `ClutchBlock` (extends `GearshiftBlock`)
- [ ] `ClutchBlockEntity` (extends `SplitShaftBlockEntity`)
- [ ] `GearshiftBlock` (extends `AbstractEncasedShaftBlock`)
- [ ] `GearshiftBlockEntity` (extends `SplitShaftBlockEntity`)
- [ ] `SplitShaftBlockEntity` (extends `DirectionalShaftHalvesBlockEntity`)
- [ ] `sequencer/SequencedGearshiftBlock` (extends `HorizontalAxisKineticBlock`)
- [ ] `sequencer/SequencedGearshiftBlockEntity` (extends `SplitShaftBlockEntity`)

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
