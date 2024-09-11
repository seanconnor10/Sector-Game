package com.disector.renderer;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector4;
import com.badlogic.gdx.utils.Array;

import com.disector.Application;
import com.disector.Sector;
import com.disector.Wall;

public abstract class Renderer {
    final Pixmap.Format pixelFormat;

    final Application app;
    final Array<Wall> walls;
    final Array<Sector> sectors;
    final SpriteBatch batch;

    int frameWidth, frameHeight;
    float halfWidth, halfHeight;

    public float camX, camY, camZ, camR, camVLook;
    public float baseFOV = 175f;
    public float camFOV = 175.f;
    public int camCurrentSector;

    public Renderer(Application app) {
        this.app = app;
        this.pixelFormat = app.pixelFormat;
        this.batch = app.batch;
        this.walls = app.walls;
        this.sectors = app.sectors;
    }

    public abstract void renderWorld();

    public abstract void resizeFrame(int w, int h);

    public abstract TextureRegion copyPixels();

    public abstract void drawFrame();

    public void setFov(float val) {
        baseFOV = val;
    }

    public void placeCamera(Vector4 pos, float vLook, int camCurrentSector) {
        camX = pos.x; camY = pos.y; camZ = pos.z; camR = pos.w; camVLook = vLook;
        this.camCurrentSector = camCurrentSector;
    }

    public void placeCamera(Vector4 pos, int camCurrentSector) {
        camX = pos.x; camY = pos.y; camZ = pos.z; camR = pos.w;
        this.camCurrentSector = camCurrentSector;
    }

    public void placeCamera(int camCurrentSector) {
        this.camCurrentSector = camCurrentSector;
    }

    public void placeCamera(Vector4 pos) {
        camX = pos.x; camY = pos.y; camZ = pos.z; camR = pos.w;
    }

    public void placeCamera(float x, float y) {
        camX = x;
        camY = y;
    }

    public void placeCamera(float x, float y, float r) {
        camX = x;
        camY = y;
        camR = r;
    }

    public void shiftCamera(Vector2 pos) {
        camX += pos.x;
        camY += pos.y;
    }
}
