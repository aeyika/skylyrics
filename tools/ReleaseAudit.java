import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import java.nio.charset.StandardCharsets;

/** Audits actual nested release bytes, rather than relying on Gradle task status. */
class ReleaseAudit {
    static void check(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
    static int major(byte[] bytes)throws IOException{var in=new DataInputStream(new ByteArrayInputStream(bytes));check(in.readInt()==0xcafebabe,"Invalid class header");in.readUnsignedShort();return in.readUnsignedShort();}
    public static void main(String[] args)throws Exception{
        Path root=Path.of(args.length==0?".":args[0]);
        for(String target:List.of("1.21.11","26.2")){
            Path file=root.resolve("dist/skylyrics-0.1.0+mc"+target+".jar");
            try(var zip=new ZipFile(file.toFile())){
                String metadata=new String(zip.getInputStream(zip.getEntry("fabric.mod.json")).readAllBytes(),StandardCharsets.UTF_8);
                metadata=java.util.regex.Pattern.compile("\\\\u([0-9a-fA-F]{4})").matcher(metadata).replaceAll(m->String.valueOf((char)Integer.parseInt(m.group(1),16)));
                check(java.util.regex.Pattern.compile("\"minecraft\"\\s*:\\s*\""+java.util.regex.Pattern.quote(target)+"\"").matcher(metadata).find(),"Minecraft metadata mismatch");
                check(java.util.regex.Pattern.compile("\"java\"\\s*:\\s*\">="+(target.equals("26.2")?25:21)+"\"").matcher(metadata).find(),"Java metadata mismatch");
                int expected=target.equals("26.2")?69:65;
                for(String name:List.of("SkyLyricsClient","SkyRenderer","SettingsScreen","SoftTextPipeline")){
                    var e=zip.getEntry("dev/skylyrics/fabric/"+name+".class");check(e!=null,"Missing adapter "+name);
                    check(major(zip.getInputStream(e).readAllBytes())==expected,"Wrong bytecode target for "+name);
                }
                check(zip.getEntry("assets/skylyrics/shaders/core/panel.fsh")!=null,"Missing soft-alpha shader");
                check(zip.getEntry("assets/skylyrics/fonts/beautiful-lyrics-bold.ttf")!=null,"Missing preserved font");
                check(zip.getEntry("dev/skylyrics/tests/SkyLyricsGameTest.class")==null,"Test harness leaked into release");
                var core=zip.stream().filter(e->e.getName().startsWith("META-INF/jars/")&&e.getName().contains("core")&&e.getName().endsWith(".jar")).findFirst().orElseThrow();
                int classes=0;
                try(var nested=new ZipInputStream(zip.getInputStream(core))){ZipEntry e;while((e=nested.getNextEntry())!=null){check(!e.getName().contains("client-test")&&!e.getName().endsWith(".java"),"Test source leaked into nested core");if(e.getName().endsWith(".class")){check(major(nested.readAllBytes())==65,"Shared core is not Java 21");classes++;}}}
                check(classes>10,"Nested core is empty");System.out.println(target+": metadata, Java "+(expected-44)+" adapters, Java 21 core, shaders, font and test isolation verified");
            }
        }
    }
}
