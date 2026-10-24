package dev.skylyrics.fabric;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.function.Supplier;

public final class SettingsScreen extends Screen {
    private final Screen parent;
    public SettingsScreen(Screen parent){super(Component.literal("Sky Lyrics"));this.parent=parent;}
    @Override protected void init(){
        var r=SkyLyricsClient.runtime;int left=width/2-155,top=Math.max(60,height/2-80);
        button(left,top,150,()->"Lyrics: "+(r.preferences.enabled()?"On":"Off"),()->r.update(r.preferences.withEnabled(!r.preferences.enabled())));
        button(left+160,top,150,()->"Demo: "+(r.demo()?"On":"Off"),r::toggleDemo);
        button(left,top+26,150,()->"Pair with Spotify",r::pair);
        button(left+160,top+26,150,()->"Recenter panel",()->SkyLyricsClient.renderer.recenter());
        button(left,top+52,150,()->"Offset −50 ms",()->r.update(r.preferences.withOffset(Math.max(-10000,r.preferences.offsetMs()-50))));
        button(left+160,top+52,150,()->"Offset +50 ms",()->r.update(r.preferences.withOffset(Math.min(10000,r.preferences.offsetMs()+50))));
        button(left,top+78,100,()->"Depth: "+r.preferences.occlusion(),()->r.update(r.preferences.withEffects(!r.preferences.occlusion(),r.preferences.blur(),r.preferences.glow())));
        button(left+105,top+78,100,()->"Blur: "+r.preferences.blur(),()->r.update(r.preferences.withEffects(r.preferences.occlusion(),!r.preferences.blur(),r.preferences.glow())));
        button(left+210,top+78,100,()->"Glow: "+r.preferences.glow(),()->r.update(r.preferences.withEffects(r.preferences.occlusion(),r.preferences.blur(),!r.preferences.glow())));
        button(left,top+104,150,()->"Smaller text",()->r.update(r.preferences.withPlacement(r.preferences.distance(),r.preferences.width(),Math.max(.25,r.preferences.scale()-.25),r.preferences.opacity())));
        button(left+160,top+104,150,()->"Larger text",()->r.update(r.preferences.withPlacement(r.preferences.distance(),r.preferences.width(),Math.min(4,r.preferences.scale()+.25),r.preferences.opacity())));
        button(left,top+130,150,()->"Skybox anchor: "+(r.preferences.skyboxAnchor()?"On":"Off"),()->{r.update(r.preferences.withSkyboxAnchor(!r.preferences.skyboxAnchor()));SkyLyricsClient.renderer.recenter();});
        button(left+160,top+130,150,()->"Done",this::onClose);
    }
    private void button(int x,int y,int w,Supplier<String> label,Runnable action){
        addRenderableWidget(Button.builder(Component.literal(label.get()),b->{action.run();b.setMessage(Component.literal(label.get()));}).bounds(x,y,w,20).build());
    }
    @Override public void render(GuiGraphics g,int mx,int my,float delta){
        super.render(g,mx,my,delta);int top=Math.max(60,height/2-80);
        g.drawCenteredString(font,title,width/2,18,0xffffffff);
        g.drawCenteredString(font,"Beautiful Lyrics by surfbryce · adapted for Minecraft",width/2,34,0xffa5c9ba);
        var r=SkyLyricsClient.runtime;
        g.drawCenteredString(font,"Timing offset: "+(int)r.preferences.offsetMs()+" ms",width/2,top-15,0xffdddddd);
        String status=r.pairingCode.isEmpty()?r.status:"Pairing code: "+r.pairingCode+" (valid for 2 minutes)";
        g.drawCenteredString(font,status,width/2,top+170,0xffa5f3cd);
    }
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
