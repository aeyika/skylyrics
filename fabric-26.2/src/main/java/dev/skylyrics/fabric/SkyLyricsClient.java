package dev.skylyrics.fabric;

import dev.skylyrics.presentation.SkyRuntime;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.InvalidateRenderStateCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class SkyLyricsClient implements ClientModInitializer {
    public static SkyRuntime runtime;
    public static SkyRenderer renderer;
    @Override public void onInitializeClient(){
        try{runtime=new SkyRuntime();}catch(Exception e){throw new IllegalStateException("Sky Lyrics could not initialize",e);}
        renderer=new SkyRenderer(runtime);
        var category=KeyMapping.Category.register(Identifier.fromNamespaceAndPath("skylyrics","controls"));
        var settings=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.skylyrics.settings",GLFW.GLFW_KEY_K,category));
        var recenter=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.skylyrics.recenter",GLFW.GLFW_KEY_J,category));
        var demo=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.skylyrics.demo",GLFW.GLFW_KEY_F8,category));
        LevelExtractionEvents.END_EXTRACTION.register(renderer::extract);
        LevelRenderEvents.COLLECT_SUBMITS.register(renderer::draw);
        InvalidateRenderStateCallback.EVENT.register(()->{renderer.close();runtime.invalidate();});
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            while(settings.consumeClick())client.gui.setScreen(new SettingsScreen(client.gui.screen()));
            while(recenter.consumeClick())renderer.recenter();
            while(demo.consumeClick())runtime.toggleDemo();
            if(client.level==null)renderer.leaveWorld();
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client->{renderer.close();runtime.close();});
    }
}
