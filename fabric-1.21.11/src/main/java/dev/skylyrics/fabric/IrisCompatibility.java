package dev.skylyrics.fabric;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Optional integration with Iris' public API v0.3. No Iris classes are linked when
 * Iris is absent, so the same jar also runs on vanilla Fabric and Sodium alone.
 */
final class IrisCompatibility {
    private static final Logger LOGGER = LoggerFactory.getLogger("skylyrics/iris");
    private static final Access ACCESS = discover();
    private static final Set<RenderPipeline> ASSIGNED =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private static boolean assignmentFailed;

    private IrisCompatibility() {}

    /** Called on the render thread before adding this panel to the world buffers. */
    static boolean prepareWorldDraw(RenderPipeline... pipelines) {
        if (ACCESS == null) return true;
        try {
            // A lyrics panel is emissive UI and must never enter a shadow map.
            if ((boolean) ACCESS.shadow.invoke(ACCESS.api)) return false;
            if (!(boolean) ACCESS.active.invoke(ACCESS.api) || assignmentFailed) return true;
            for (RenderPipeline pipeline : pipelines) {
                if (ASSIGNED.contains(pipeline)) continue;
                /*
                 * Register while Iris is rendering the world. Its public API
                 * resolves the appropriate shader from the *current* vertex
                 * format: Iris extends vanilla's text format to GLYPH here.
                 * Registering at mod initialization instead can select ENTITY,
                 * which expects a different vertex layout.
                 */
                ACCESS.assign.invoke(ACCESS.api, pipeline, ACCESS.translucent);
                ASSIGNED.add(pipeline);
                LOGGER.info("Registered panel pipeline {} with Iris translucent text rendering",
                        pipeline.getLocation());
            }
        } catch (ReflectiveOperationException | LinkageError exception) {
            assignmentFailed = true;
            LOGGER.warn("Iris panel integration failed; retaining the Fabric rendering path", exception);
        }
        return true;
    }

    private static Access discover() {
        if (!FabricLoader.getInstance().isModLoaded("iris")) return null;
        try {
            Class<?> apiType = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object api = apiType.getMethod("getInstance").invoke(null);
            int revision = (int) apiType.getMethod("getMinorApiRevision").invoke(api);
            if (revision < 3) {
                LOGGER.warn("Iris API revision {} does not support custom panel pipelines; revision 3 is required",
                        revision);
                return null;
            }
            Class<?> programType = Class.forName("net.irisshaders.iris.api.v0.IrisProgram");
            Object translucent = programType.getField("ENTITIES_TRANSLUCENT").get(null);
            return new Access(api, apiType.getMethod("isShaderPackInUse"),
                    apiType.getMethod("isRenderingShadowPass"),
                    apiType.getMethod("assignPipeline", RenderPipeline.class, programType), translucent);
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.warn("Iris public API is unavailable; retaining the Fabric rendering path", exception);
            return null;
        }
    }

    private record Access(Object api, Method active, Method shadow, Method assign, Object translucent) {}
}

