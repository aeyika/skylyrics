package dev.skylyrics.core;

/** Captures a heading-relative offset once; later camera rotation does not change it. */
public record PanelAnchor(double originX,double originY,double originZ,double offsetX,double offsetY,double offsetZ) {
    public record Offset(double x,double y,double z) {}
    public Offset relativeTo(double cameraX,double cameraY,double cameraZ,boolean skyboxAnchor) {
        return skyboxAnchor ? new Offset(offsetX,offsetY,offsetZ)
                : new Offset(originX+offsetX-cameraX,originY+offsetY-cameraY,originZ+offsetZ-cameraZ);
    }
}
