package dev.skylyrics.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class PanelAnchorTest {
    @Test void skyboxFollowsTranslationWhileWorldAnchorStaysAtItsWorldPoint(){
        var anchor=new PanelAnchor(100,64,-50,0,5,12);
        assertEquals(new PanelAnchor.Offset(0,5,12),anchor.relativeTo(100,64,-50,true));
        assertEquals(new PanelAnchor.Offset(0,5,12),anchor.relativeTo(350,90,100,true));
        assertEquals(new PanelAnchor.Offset(-250,-21,-138),anchor.relativeTo(350,90,100,false));
    }
    @Test void oldPreferencesLoadAndSkyboxChoiceSurvivesOtherSettings(@TempDir Path directory)throws Exception{
        Path file=directory.resolve("preferences.json");Preferences.defaults().save(file);
        Files.writeString(file,Files.readString(file).replaceAll(",\\s*\"skyboxAnchor\"\\s*:\\s*false",""));
        assertFalse(Preferences.load(file).skyboxAnchor());
        var preferences=Preferences.load(file).withSkyboxAnchor(true).withOffset(50).withEnabled(false)
                .withEffects(false,false,false).withPlacement(15,400,1.25,.8);
        preferences.save(file);assertEquals(preferences,Preferences.load(file));
        assertTrue(Preferences.load(file).skyboxAnchor());
        assertTrue(new AnimationEngine().extract(Demo.lyrics(),3000,0,preferences.withEnabled(true),text->6).panel().skyboxAnchor());
    }
}
