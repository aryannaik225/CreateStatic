# CreateStatic — Project Context & Architecture

## Environment & Target Versions
- **Minecraft:** 1.20.1
- **Modloader:** Forge `47.4.28` (compatible range `[47,48)`)
- **Java:** JDK 17 (`JAVA_17` compatibility level)
- **Target Mod:** Create `6.0.8-289` (which bundles **Flywheel `1.0.5`** under package `dev.engine_room.flywheel`)
- **Dependencies:** Cloth Config API (for in-game F9 settings menu)
- **Mod ID:** `createstatic` | **Base Package:** `com.createstatic`
- **Side:** Client-side ONLY (`clientSideOnly=true` in `mods.toml`, all Mixins go in the `"client": []` array of `createstatic.mixins.json`).

## External Reference Directory
- Extracted assets, blockstates, models, and class files from `create-1.20.1-6.0.8.jar` are located at `../CreateReference/`.
- **Rule:** Never scan the entire `../CreateReference/` folder at once. Only read specific paths inside it when checking a blockstate, model JSON, or decompiled class signature.
- **CRITICAL RESTRICTION:** DO NOT run `find`, `grep -r`, or loop `javap` over the entire `../CreateReference` directory. It causes terminal hangs. If you need a class signature, run `javap` ONLY on the exact, specific file path you need. If unsure of the path, write the Mixin based on your best guess and let `./gradlew build` catch any signature errors.

## Core Architecture (v1.0 Static Freezing & v1.1 LOD Culling)
1. **Config States:** 
   - `CreateStaticConfig.MASTER_TOGGLE` (Forge BooleanValue, accessed via `CreateStaticConfig.isMasterEnabled()`. Toggles kinetic freezing).
   - `CreateStaticConfig.LOD_DISTANCE_THRESHOLD` (Forge IntValue, default 32. MUST call `.get()` when referencing it in Mixins to get the raw integer. Dictates when sub-component culling activates).
2. **Chunk Mesh Rebaking:** Toggling settings must call `Minecraft.getInstance().levelRenderer.allChanged()` so chunks re-evaluate `getRenderShape()` and bake static JSON models into the chunk mesh.

## CreateStatic Rendering Baseline (CRITICAL)
When writing Mixins for the CreateStatic mod, you MUST strictly follow this approach to avoid Indigo CPU culling bottlenecks or RAM leaks:

1. **Keep Flywheel Active:** Do NOT force `RenderShape.MODEL` or cancel generic BERs for Flywheel-rendered blocks. Let Flywheel handle the rendering on the GPU.
2. **v1.0 Freeze via Flywheel Math:** Do NOT use `.delete()`, recycle instances, or use `.setVisible(false)` for the entire block in Flywheel. Instead, inject into the Flywheel Visuals (e.g., `update()` or `tick()`) and set `rotationalSpeed`, `scrollSpeed`, or animation timers to `0` when `CreateStaticConfig.isMasterEnabled()`.
3. **v1.0 The Hard Logic Cull:** Inject at `@At("HEAD")` with `cancellable = true` into `SmartBlockEntity.tick()`. If `level.isClientSide()` AND `CreateStaticConfig.isMasterEnabled()`, call `info.cancel()` to kill client-side math.
4. **v1.1 LOD Sub-Component Culling:** Use `CameraDistanceUtil` to check the squared distance from the block to the camera. Inside Flywheel Visuals (like `EncasedFanVisual`), if the squared distance exceeds the squared `LOD_DISTANCE_THRESHOLD`, do not create or update the internal partial models (e.g., fan blades, inner shafts). Keep only the outer casing instance active.

## Build & Verification Commands
- Compile & verify Mixin refmaps: `./gradlew compileJava` or `./gradlew build`
- Production jar location: `build/libs/createstatic-1.1.0.jar` (must be reobfuscated via `reobfJar`).

## Coding Rules
- Always register every new Mixin class inside `src/main/resources/createstatic.mixins.json` under the `"client"` array.
- Never modify server-side logic or block entity ticking; this mod is strictly visual/client-side so it works on multiplayer servers without the server having the mod installed.