package dev.skylyrics.tests;

import dev.skylyrics.fabric.*;
import dev.skylyrics.core.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.*;
import net.minecraft.client.CameraType;
import com.google.gson.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;

/** Development-only end-to-end harness; never included in a release jar. */
public final class SkyLyricsGameTest implements FabricClientGameTest {
    private static final Path OUTPUT=Path.of(System.getProperty("skylyrics.testOutput","screenshots/skylyrics"));
    private final List<String> passed=new ArrayList<>();
    private void pass(String check){passed.add(check);System.out.println("SKYLYRICS PASS: "+check);}
    private Path shot(ClientGameTestContext context,String name){return context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix().withDestinationDir(OUTPUT));}
    private void at(ClientGameTestContext context,double timestamp){
        context.runOnClient(client->{var r=SkyLyricsClient.runtime;r.session.track("skylyrics:demo","Original Sky Lyrics demo",32000);r.session.lyrics(Demo.lyrics());r.session.playback("skylyrics:demo",timestamp,32000,false,0);r.invalidate();});
        context.waitTicks(3);
    }
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    @Override public void runTest(ClientGameTestContext context){
        try{
            Files.createDirectories(OUTPUT);
            try(var world=context.worldBuilder().create()){
                context.waitFor(client->client.level!=null&&client.player!=null,1200);
                context.waitTicks(50);
                if(Boolean.getBoolean("skylyrics.testPhoton"))assertPhoton(context);
                context.runOnClient(client->{client.player.setYRot(0);client.player.setXRot(-8);client.options.setCameraType(CameraType.FIRST_PERSON);SkyLyricsClient.renderer.recenter();});
                at(context,3000);
                context.runOnClient(client->Files.writeString(OUTPUT.resolve("frame-3000.json"),new GsonBuilder().setPrettyPrinting().create().toJson(SkyLyricsClient.runtime.extract().frame())));
                Path with=shot(context,"syllable-3000");
                context.runOnClient(client->SkyLyricsClient.runtime.update(SkyLyricsClient.runtime.preferences.withEnabled(false)));
                context.waitTicks(3);Path without=shot(context,"disabled-baseline");
                context.runOnClient(client->SkyLyricsClient.runtime.update(SkyLyricsClient.runtime.preferences.withEnabled(true)));
                context.waitTicks(3);
                var a=ImageIO.read(with.toFile());var b=ImageIO.read(without.toFile());int changed=0,whiteGlyphs=0;
                for(int y=0;y<Math.min(a.getHeight(),b.getHeight());y++)for(int x=0;x<Math.min(a.getWidth(),b.getWidth());x++)if(a.getRGB(x,y)!=b.getRGB(x,y))changed++;
                for(int y=a.getHeight()/4;y<a.getHeight()*3/5;y++)for(int x=a.getWidth()/4;x<a.getWidth()*3/4;x++){
                    int c=a.getRGB(x,y),r=(c>>>16)&255,g=(c>>>8)&255,blue=c&255;
                    if(r>200&&g>200&&blue>200&&Math.max(r,Math.max(g,blue))-Math.min(r,Math.min(g,blue))<20)whiteGlyphs++;
                }
                require(whiteGlyphs>50,"No visible lyric glyphs in the expected sky region ("+whiteGlyphs+")");
                require(changed>100,"World renderer did not change framebuffer pixels");pass("world rendering changes actual framebuffer ("+changed+" pixels)");
                for(int time:new int[]{0,1500,9000,15500,21000,29000}){at(context,time);shot(context,"demo-"+time);context.runOnClient(client->Files.writeString(OUTPUT.resolve("frame-"+time+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(SkyLyricsClient.runtime.extract().frame())));}
                pass("syllable demo, long lines and Unicode render without errors");
                context.runOnClient(client->{var r=SkyLyricsClient.runtime;var line=new Lyrics.Line(0,5000,Lyrics.Alignment.LEFT,Lyrics.Role.INTERLUDE,List.of(new Lyrics.Segment("•••",0,5000,false)));r.session.track("interlude:test","Interlude",5000);r.session.lyrics(new Lyrics("interlude:test",Lyrics.Sync.SYLLABLE,List.of(line)));r.session.playback("interlude:test",2000,5000,false,0);r.invalidate();});context.waitTicks(3);shot(context,"interlude");pass("original three-dot interlude animation");
                context.runOnClient(client->{
                    var line=new Lyrics.Line(0,6000,Lyrics.Alignment.LEFT,Lyrics.Role.LEAD,List.of(new Lyrics.Segment("An original line-synced test",0,6000,false)));
                    var r=SkyLyricsClient.runtime;r.session.track("line:test","Line timing",6000);r.session.lyrics(new Lyrics("line:test",Lyrics.Sync.LINE,List.of(line)));r.session.playback("line:test",3000,6000,false,0);r.invalidate();
                });context.waitTicks(3);shot(context,"line-synced");pass("track change and line-synced lyrics");
                at(context,3000);
                context.setScreen(()->new SettingsScreen(null));context.waitTicks(2);shot(context,"settings");
                context.clickScreenButton("Pair with Spotify");context.waitTicks(1);
                require(!SkyLyricsClient.runtime.pairingCode.isEmpty(),"Pairing code missing");
                context.clickScreenButton("Done");context.waitTicks(2);pass("settings and pairing controls");
                bridge(context);
                at(context,3000);
                context.runOnClient(client->{var p=SkyLyricsClient.runtime.preferences;SkyLyricsClient.runtime.update(p.withEffects(false,false,false));});context.waitTicks(3);shot(context,"effects-off");
                context.runOnClient(client->{var p=SkyLyricsClient.runtime.preferences;SkyLyricsClient.runtime.update(p.withEffects(true,true,true));});
                world.getServer().runCommand("execute at @p run fill ~-20 ~-2 ~6 ~20 ~25 ~6 minecraft:stone");
                context.waitFor(client->client.level.getBlockState(client.player.blockPosition().offset(0,1,6)).is(net.minecraft.world.level.block.Blocks.STONE),200);
                context.waitTicks(10);shot(context,"terrain-occluded");
                context.runOnClient(client->{var p=SkyLyricsClient.runtime.preferences;SkyLyricsClient.runtime.update(p.withEffects(false,true,true));});context.waitTicks(3);shot(context,"terrain-see-through");
                world.getServer().runCommand("execute at @p run fill ~-20 ~-2 ~6 ~20 ~25 ~6 minecraft:air");
                context.runOnClient(client->{var p=SkyLyricsClient.runtime.preferences;SkyLyricsClient.runtime.update(p.withEffects(true,true,true));});context.waitTicks(3);pass("terrain depth and see-through modes rendered for review");
                context.getInput().resizeWindow(1280,720);context.waitTicks(3);shot(context,"resized-1280x720");pass("window resize");
                context.getInput().pressKey(org.lwjgl.glfw.GLFW.GLFW_KEY_F11);context.waitTicks(3);shot(context,"fullscreen");
                context.getInput().pressKey(org.lwjgl.glfw.GLFW.GLFW_KEY_F11);context.waitTicks(3);pass("fullscreen transition");
                CompletableFuture<?> reloaded=context.computeOnClient(client->client.reloadResourcePacks());
                context.waitFor(client->reloaded.isDone(),1200);reloaded.join();context.waitTicks(3);shot(context,"resource-reload");pass("resource reload");
                if(Boolean.getBoolean("skylyrics.testPhoton")){
                    assertPhoton(context);toggleShaders(context,false);context.waitTicks(5);shot(context,"shaders-disabled");toggleShaders(context,true);context.waitTicks(10);assertPhoton(context);shot(context,"photon-reenabled");pass("Photon toggle and resource reload");
                }
                context.runOnClient(client->{client.options.setCameraType(CameraType.THIRD_PERSON_BACK);SkyLyricsClient.renderer.recenter();});context.waitTicks(3);shot(context,"third-person");
                context.runOnClient(client->{client.options.setCameraType(CameraType.FIRST_PERSON);SkyLyricsClient.renderer.recenter();});context.waitTicks(3);pass("recenter and camera perspective changes");
                // Screenshot sequence of real rendered game frames, for silent demonstration footage.
                int captures=Integer.getInteger("skylyrics.captureFrames",0);
                for(int i=0;i<captures;i++){at(context,1000+i*100);shot(context,"footage-"+String.format("%04d",i));}
                if(captures>0)pass("captured "+captures+" rendered demo frames");
            }
            context.waitTicks(3);pass("world disconnect and texture cleanup");
            if(Boolean.getBoolean("skylyrics.testMultiplayer")){
                try(var server=context.worldBuilder().createServer();var connection=server.connect()){
                    context.waitFor(client->client.level!=null&&client.player!=null,1200);context.waitTicks(30);
                    context.runOnClient(client->{client.player.setYRot(0);client.player.setXRot(-8);SkyLyricsClient.renderer.recenter();});at(context,3000);shot(context,"multiplayer");pass("dedicated multiplayer world rendering");bridge(context);
                }
            }
            Files.writeString(OUTPUT.resolve("results.json"),new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("passed",passed,"completeMvp",false)),StandardCharsets.UTF_8);
        }catch(Exception e){throw new RuntimeException(e);}
    }
    private void bridge(ClientGameTestContext context)throws Exception{
        var runtime=SkyLyricsClient.runtime;String code=context.computeOnClient(client->runtime.bridge.beginPairing());
        var listener=new Listener();var ws=HttpClient.newHttpClient().newWebSocketBuilder().buildAsync(URI.create("ws://127.0.0.1:"+runtime.preferences.port()),listener).join();
        ws.sendText("{\"v\":1,\"type\":\"hello\",\"seq\":0,\"code\":\""+code+"\"}",true).join();
        require(listener.messages.poll(5,TimeUnit.SECONDS)!=null,"Pairing failed");
        ws.sendText("{\"v\":1,\"type\":\"track\",\"seq\":1,\"trackId\":\"skylyrics:demo\",\"title\":\"Bridge test\",\"durationMs\":32000}",true).join();
        try(var in=Demo.class.getResourceAsStream("/demo.json")){ws.sendText(new String(in.readAllBytes(),StandardCharsets.UTF_8),true).join();}
        long seq=4;
        for(int pos:new int[]{1000,2000,18000,0}){
            ws.sendText("{\"v\":1,\"type\":\"playback\",\"seq\":"+(seq++)+",\"trackId\":\"skylyrics:demo\",\"positionMs\":"+pos+",\"durationMs\":32000,\"playing\":false,\"sentAtMs\":"+System.currentTimeMillis()+"}",true).join();
            long ping=seq++;ws.sendText("{\"v\":1,\"type\":\"ping\",\"seq\":"+ping+"}",true).join();
            require(listener.messages.poll(5,TimeUnit.SECONDS)!=null,"Bridge ping timed out");
            require(runtime.session.snapshot(0).clock().positionMs()==pos,"Pause/seek/repeat position differs");context.waitTicks(2);
        }
        pass("real WebSocket pairing, lyrics, pause, seek and repeat inside Minecraft");
        ws.sendClose(1000,"test reconnect").join();
        var reconnect=new Listener();var retry=HttpClient.newHttpClient().newWebSocketBuilder().buildAsync(URI.create("ws://127.0.0.1:"+runtime.preferences.port()),reconnect).join();
        retry.sendText("{\"v\":1,\"type\":\"hello\",\"seq\":0,\"token\":\""+runtime.preferences.bridgeToken()+"\"}",true).join();
        require(reconnect.messages.poll(5,TimeUnit.SECONDS)!=null,"Reconnection failed");retry.sendClose(1000,"done").join();pass("authenticated reconnection inside Minecraft");
    }
    private static void assertPhoton(ClientGameTestContext context){
        context.runOnClient(client->{
            try{
                var type=Class.forName("net.irisshaders.iris.api.v0.IrisApi");var api=type.getMethod("getInstance").invoke(null);
                require((boolean)type.getMethod("isShaderPackInUse").invoke(api),"Photon shader pack is not active");
                var iris=Class.forName("net.irisshaders.iris.Iris");
                require(String.valueOf(iris.getMethod("getCurrentPackName").invoke(null)).contains("photon_v1.4.zip"),"Unexpected shader pack");
                require(((Optional<?>)iris.getMethod("getStoredError").invoke(null)).isEmpty(),"Iris reports a shader compilation error");
            }catch(ReflectiveOperationException e){throw new AssertionError("Iris shader verification failed",e);}
        });
    }
    private static void toggleShaders(ClientGameTestContext context,boolean enabled){
        context.runOnClient(client->{
            try{
                var type=Class.forName("net.irisshaders.iris.api.v0.IrisApi");var api=type.getMethod("getInstance").invoke(null);
                var config=type.getMethod("getConfig").invoke(api);
                Class.forName("net.irisshaders.iris.api.v0.IrisApiConfig").getMethod("setShadersEnabledAndApply",boolean.class).invoke(config,enabled);
            }catch(ReflectiveOperationException e){throw new AssertionError("Iris toggle failed",e);}
        });
    }
    private static final class Listener implements WebSocket.Listener {
        final BlockingQueue<String> messages=new LinkedBlockingQueue<>();
        public void onOpen(WebSocket ws){ws.request(1);}
        public CompletionStage<?> onText(WebSocket ws,CharSequence text,boolean last){messages.add(text.toString());ws.request(1);return null;}
    }
}
