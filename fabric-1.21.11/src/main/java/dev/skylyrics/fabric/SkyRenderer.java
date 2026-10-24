package dev.skylyrics.fabric;

import dev.skylyrics.presentation.*;
import dev.skylyrics.core.PanelAnchor;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.*;
import net.fabricmc.fabric.api.client.rendering.v1.*;
import net.fabricmc.fabric.api.client.rendering.v1.world.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.*;

/** 1.21.11 adapter: prepare at extraction, submit the immutable texture quad through vanilla buffers. */
public final class SkyRenderer implements AutoCloseable {
    private static final RenderStateDataKey<Prepared> FRAME=RenderStateDataKey.create();
    private final SkyRuntime runtime;
    private final Identifier textureId=Identifier.fromNamespaceAndPath("skylyrics","panel/1_21_11");
    private DynamicTexture texture;
    private java.awt.image.BufferedImage uploaded;
    private PanelAnchor anchor;
    private Quaternionf orientation;
    private int windowWidth,windowHeight;
    private final net.minecraft.client.renderer.rendertype.RenderType depthType=SoftTextPipeline.create(textureId,true),throughType=SoftTextPipeline.create(textureId,false);
    SkyRenderer(SkyRuntime runtime){this.runtime=runtime;}
    public void extract(WorldExtractionContext context){
        var frame=runtime.extract();
        if(frame==null){context.worldState().setData(FRAME,null);return;}
        var camera=context.camera();
        if(anchor==null){
            var forward=camera.forwardVector();
            var position=camera.position();double distance=frame.frame().panel().distance();
            anchor=new PanelAnchor(position.x,position.y,position.z,forward.x()*distance,forward.y()*distance+2,forward.z()*distance);
            orientation=new Quaternionf(camera.rotation());
        }
        var position=camera.position();
        var delta=anchor.relativeTo(position.x,position.y,position.z,frame.frame().panel().skyboxAnchor());
        context.worldState().setData(FRAME,new Prepared(frame,delta.x(),delta.y(),delta.z(),orientation.x,orientation.y,orientation.z,orientation.w));
    }
    public void draw(WorldRenderContext context){
        if(!SoftTextPipeline.prepareWorldDraw())return;
        Prepared data=context.worldState().getData(FRAME);if(data==null)return;
        var client=Minecraft.getInstance();var window=client.getWindow();
        if(windowWidth!=window.getWidth()||windowHeight!=window.getHeight()){close();windowWidth=window.getWidth();windowHeight=window.getHeight();}
        upload(data.content.pixels());
        var matrices=context.matrices();matrices.pushPose();
        matrices.translate(data.x,data.y,data.z);matrices.mulPose(new Quaternionf(data.qx,data.qy,data.qz,data.qw));
        float size=(float)(data.content.frame().panel().distance()*.0024);matrices.scale(size,-size,size);
        var type=data.content.frame().panel().occlusion()?depthType:throughType;
        var vertices=context.consumers().getBuffer(type);
        quad(matrices.last().pose(),vertices,(float)data.content.pixels().getWidth()/PanelRasterizer.DENSITY,PanelRasterizer.HEIGHT);
        matrices.popPose();
    }
    private void upload(java.awt.image.BufferedImage pixels){
        if(uploaded==pixels)return;
        if(texture==null||texture.getPixels().getWidth()!=pixels.getWidth()||texture.getPixels().getHeight()!=pixels.getHeight()){
            close();texture=new DynamicTexture(()->"Sky Lyrics prepared text mask",new NativeImage(pixels.getWidth(),pixels.getHeight(),false));
            Minecraft.getInstance().getTextureManager().register(textureId,texture);
        }
        NativeImage target=texture.getPixels();
        int[] argb=pixels.getRGB(0,0,pixels.getWidth(),pixels.getHeight(),null,0,pixels.getWidth());
        for(int y=0;y<pixels.getHeight();y++)for(int x=0;x<pixels.getWidth();x++)target.setPixel(x,y,argb[y*pixels.getWidth()+x]);
        texture.upload();uploaded=pixels;
    }
    private static void quad(Matrix4f pose,VertexConsumer v,float width,float height){
        float x=-width/2,y=-110;
        v.addVertex(pose,x,y,0).setColor(-1).setUv(0,0).setLight(0xf000f0);
        v.addVertex(pose,x,y+height,0).setColor(-1).setUv(0,1).setLight(0xf000f0);
        v.addVertex(pose,x+width,y+height,0).setColor(-1).setUv(1,1).setLight(0xf000f0);
        v.addVertex(pose,x+width,y,0).setColor(-1).setUv(1,0).setLight(0xf000f0);
    }
    public void recenter(){anchor=null;orientation=null;}
    public void leaveWorld(){recenter();close();runtime.invalidate();}
    @Override public void close(){if(texture!=null){Minecraft.getInstance().getTextureManager().release(textureId);texture=null;}uploaded=null;}
    private record Prepared(SkyRuntime.Prepared content,double x,double y,double z,float qx,float qy,float qz,float qw){}
}
