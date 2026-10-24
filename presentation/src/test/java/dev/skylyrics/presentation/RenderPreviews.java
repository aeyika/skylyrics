package dev.skylyrics.presentation;
import dev.skylyrics.core.*;
import com.google.gson.GsonBuilder;
import java.nio.file.*;
import javax.imageio.ImageIO;
public final class RenderPreviews {
    public static void main(String[] args)throws Exception{
        Path dir=Path.of(args[0]);Files.createDirectories(dir);
        var rasterizer=new PanelRasterizer();var engine=new AnimationEngine();var lyrics=Demo.lyrics();var p=Preferences.defaults();
        for(int time:new int[]{0,1500,3000,9000,15500,21000,29000}){
            var frame=engine.extract(lyrics,time,1d/60,p,(text,role)->rasterizer.width(text,role));
            var image=rasterizer.rasterize(frame);
            ImageIO.write(image,"png",dir.resolve("demo-"+time+".png").toFile());
            var composite=new java.awt.image.BufferedImage(image.getWidth(),image.getHeight(),java.awt.image.BufferedImage.TYPE_INT_RGB);
            var graphics=composite.createGraphics();graphics.setColor(new java.awt.Color(22,31,43));graphics.fillRect(0,0,image.getWidth(),image.getHeight());graphics.drawImage(image,0,0,null);graphics.dispose();
            ImageIO.write(composite,"png",dir.resolve("demo-"+time+"-background.png").toFile());
            Files.writeString(dir.resolve("demo-"+time+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(frame));
        }
        System.out.println("Prepared original-demo previews at "+dir.toAbsolutePath());
    }
}
