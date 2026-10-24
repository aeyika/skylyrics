package dev.skylyrics.fabric;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.*;
import net.minecraft.resources.Identifier;
import java.util.Optional;

/** 26.2 backend-managed pipeline, with optional depth-stencil state for through-terrain mode. */
final class SoftTextPipeline {
    private static final RenderPipeline DEPTH=RenderPipelines.register(RenderPipeline.builder(RenderPipelines.WORLD_TEXT_SNIPPET)
            .withVertexShader(Identifier.fromNamespaceAndPath("skylyrics","core/panel"))
            .withLocation(Identifier.fromNamespaceAndPath("skylyrics","pipeline/panel_depth"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("skylyrics","core/panel"))
            .withCull(false).withDepthStencilState(new DepthStencilState(DepthStencilState.DEFAULT.depthTest(),false)).build());
    private static final RenderPipeline THROUGH=RenderPipelines.register(RenderPipeline.builder(RenderPipelines.WORLD_TEXT_SNIPPET)
            .withVertexShader(Identifier.fromNamespaceAndPath("skylyrics","core/panel"))
            .withLocation(Identifier.fromNamespaceAndPath("skylyrics","pipeline/panel_through"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("skylyrics","core/panel"))
            .withCull(false).withDepthStencilState(Optional.empty()).build());
    static boolean prepareWorldDraw(){return IrisCompatibility.prepareWorldDraw(DEPTH,THROUGH);}
    static RenderType create(Identifier texture,boolean depth){
        return RenderType.create("skylyrics_panel_"+(depth?"depth":"through"),RenderSetup.builder(depth?DEPTH:THROUGH)
                .withTexture("Sampler0",texture).useLightmap().sortOnUpload().createRenderSetup());
    }
}
