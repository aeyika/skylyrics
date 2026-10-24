package dev.skylyrics.fabric;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.*;
import net.minecraft.resources.Identifier;

/** Backend-managed alpha blending; retains low-alpha blur/glow discarded by vanilla text. */
final class SoftTextPipeline {
    private static final RenderPipeline DEPTH=RenderPipelines.register(RenderPipeline.builder(RenderPipelines.TEXT_SNIPPET)
            .withVertexShader(Identifier.fromNamespaceAndPath("skylyrics","core/panel")).withSampler("Sampler0")
            .withLocation(Identifier.fromNamespaceAndPath("skylyrics","pipeline/panel_depth"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("skylyrics","core/panel"))
            .withCull(false).withDepthWrite(false).build());
    private static final RenderPipeline THROUGH=RenderPipelines.register(RenderPipeline.builder(RenderPipelines.TEXT_SNIPPET)
            .withVertexShader(Identifier.fromNamespaceAndPath("skylyrics","core/panel")).withSampler("Sampler0")
            .withLocation(Identifier.fromNamespaceAndPath("skylyrics","pipeline/panel_through"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("skylyrics","core/panel"))
            .withCull(false).withDepthWrite(false).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).build());
    static boolean prepareWorldDraw(){return IrisCompatibility.prepareWorldDraw(DEPTH,THROUGH);}
    static RenderType create(Identifier texture,boolean depth){
        return RenderType.create("skylyrics_panel_"+(depth?"depth":"through"),RenderSetup.builder(depth?DEPTH:THROUGH)
                .withTexture("Sampler0",texture).useLightmap().sortOnUpload().createRenderSetup());
    }
}
