package dev.skylyrics.core;
import java.io.*;
import java.nio.charset.StandardCharsets;
public final class Demo {
    private Demo(){}
    public static Lyrics lyrics(){
        try(InputStream in=Demo.class.getResourceAsStream("/demo.json")) {
            if(in==null)throw new IllegalStateException("Missing original demo fixture");
            return Protocol.lyrics(Protocol.parse(new String(in.readAllBytes(),StandardCharsets.UTF_8)));
        } catch(IOException e){throw new UncheckedIOException(e);}
    }
}
