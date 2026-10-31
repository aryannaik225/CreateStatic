# CreateStatic — Project Context & Architecture

## Environment & Target Versions
- **Minecraft:** 1.20.1
- **Modloader:** Forge `47.4.28` (compatible range `[47,48)`)
- **Java:** JDK 17 (`JAVA_17` compatibility level)
- **Target Mod:** Create `6.0.8-289` (which bundles **Flywheel `1.0.5`** under package `dev.engine_room.flywheel`)
- **Mod ID:** `createstatic` | **Base Package:** `com.createstatic`
- **Side:** Client-side ONLY (`clientSideOnly=true` in `mods.toml`, all Mixins go in the `"client": []` array of `createstatic.mixins.json`).

## External Reference Directory
- Extracted assets, blockstates, models, and class files from `create-1.20.1-6.0.8.jar` are located at `../CreateReference/`.
- **Rule:** Never scan the entire `../CreateReference/` folder at once. Only read specific paths inside it when checking a blockstate, model JSON, or decompiled class signature (e.g., `javap -cp` or reading specific paths).
- **CRITICAL RESTRICTION:** DO NOT run `find`, `grep -r`, or loop `javap` over the entire `../CreateReference` directory. It causes terminal hangs. If you need a class signature, run `javap` ONLY on the exact, specific file path you need (e.g., `javap -p com/simibubi/create/.../BeltRenderer.class`). If you are unsure of the exact path, just write the Mixin based on your best guess and let `./gradlew build` catch any signature errors—the Gradle compiler is much faster than brute-force scanning.

## Core Architecture (How Static Rendering Works)
1. **Toggle State:** `CreateStaticConfig.STATIC_MODE_ENABLED` (boolean, toggled in-game via `F9` keybind).
2. **Chunk Mesh Rebaking:** Toggling `STATIC_MODE_ENABLED` must call `Minecraft.getInstance().levelRenderer.allChanged()` so chunks re-evaluate `getRenderShape()` and bake static JSON models into the chunk mesh.
3. **3-Part Static Override for Target Blocks:**
   - **Block Shape (`KineticBlockMixin`):** Force `getRenderShape(BlockState)` to return `RenderShape.MODEL` instead of `RenderShape.ENTITYBLOCK_ANIMATED` so Minecraft bakes the blockstate's JSON model.
   - **BER Cancellation (`KineticBlockEntityRendererMixin`):** Cancel `renderSafe()` at `@At("HEAD")` for targeted blocks so the fallback BlockEntityRenderer does not draw spinning models.
   - **Flywheel 1.0 Cancellation (`FlywheelVisualMixin`):** Intercept Flywheel `1.0.5` visuals (`SingleAxisRotatingVisual`, `BracketedKineticBlockEntityVisual`, etc.) so GPU-instanced rotating models are hidden or skipped when `STATIC_MODE_ENABLED` is true.

## CreateStatic Rendering Baseline (CRITICAL)
When writing Mixins to freeze kinetic blocks for the CreateStatic mod, you MUST strictly follow this 3-part approach. Do not deviate, or you will cause Vanilla/Indigo CPU culling bottlenecks or RAM leaks:

1. **Keep Flywheel Active:** Do NOT force `RenderShape.MODEL` or cancel generic BERs. Let Flywheel handle the rendering on the GPU.
2. **Freeze via Flywheel Math:** Do NOT use `.delete()`, recycle instances, hijack `beginFrame` for recycling, or use `.setVisible(false)` in Flywheel. Instead, inject into the Flywheel Visuals (e.g., `update()` or `tick()`) and set `rotationalSpeed`, `scrollSpeed`, or animation timers to `0` when `STATIC_MODE_ENABLED == true`. 
3. **The Hard Logic Cull:** Inject at `@At("HEAD")` with `cancellable = true` into `SmartBlockEntity.tick()` (or the block's specific tick method if it overrides). If `level.isClientSide()` AND `STATIC_MODE_ENABLED == true`, call `info.cancel()` to kill client-side math.

## Build & Verification Commands
- Compile & verify Mixin refmaps: `./gradlew compileJava` or `./gradlew build`
- Production jar location: `build/libs/createstatic-1.0.0.jar` (must be reobfuscated via `reobfJar`).

## Coding Rules
- Always register every new Mixin class inside `src/main/resources/createstatic.mixins.json` under the `"client"` array.
- Never modify server-side logic or block entity ticking; this mod is strictly visual/client-side so it works on multiplayer servers without the server having the mod installed.