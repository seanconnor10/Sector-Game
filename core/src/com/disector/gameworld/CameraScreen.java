package com.disector.gameworld;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.Vector3;
import com.disector.Application;
import com.disector.Physics;
import com.disector.gameworld.components.HasWallSprite;
import com.disector.gameworld.components.Positionable;
import com.disector.renderer.SoftwareRenderer;
import com.disector.renderer.sprites.WallSprite;

public class CameraScreen implements Positionable, HasWallSprite {
    SoftwareRenderer renderer;
    Pixmap image;

    public CameraScreen(Application appInstance) {
        renderer = new SoftwareRenderer(appInstance);
        renderer.resizeFrame(128, 128);
        renderer.setFovFromDeg(100);
    }

    public void refreshImage(Vector3 pos, float r) {
        renderer.camX = pos.x;
        renderer.camY = pos.y;
        renderer.camZ = pos.z + 10;
        renderer.camR = r;
        renderer.camCurrentSector = Physics.findCurrentSectorBranching(-1, pos.x, pos.y);
        renderer.renderWorld();
        Pixmap rendering = renderer.getBufferReference();
        int w = rendering.getWidth(), h = rendering.getHeight();
        image = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        for (int x=0; x<w; x++) {
            for (int y = 0; y < h; y++) {
                image.drawPixel(x, y, rendering.getPixel(x, h - y));
            }
        }
    }

    @Override
    public WallSprite getInfo() {
        //DUNG
        // return new WallSprite(image, 0, 96, 15, 16, 96, 12);

        //OFFICE
        return new WallSprite(image, 163.8f, 62, 17, 163.8f, 46, 11);
    }

    @Override
    public Vector3 pos() {
        return null;
    }

    @Override
    public float getHeight() {
        return 0;
    }

    @Override
    public float getRadius() {
        return 0;
    }

    @Override
    public int getCurrentSector() {
        return 0;
    }

    @Override
    public void setCurrentSector(int sInd) {

    }
}
