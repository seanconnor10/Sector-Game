package com.disector.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.disector.Application;

public abstract class MapRenderer extends Renderer{
    FrameBuffer buffer;

    public MapRenderer(Application app) {
        super(app);
        camFOV = 1.f;
        resizeFrame(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    @Override
    public void resizeFrame(int w, int h) {
        if (buffer != null) {
            buffer.getColorBufferTexture().dispose();
            buffer.dispose();
        }
        //w and h should probably always be Gdx.graphics.getWidth/Height?
        if (w<1) w = 1;
        if (h<1) h = 1;
        buffer = new FrameBuffer(app.pixelFormat, w, h,false);
        frameWidth = w;
        frameHeight = h;
        halfWidth = frameWidth / 2.f;
        halfHeight = frameHeight /2.f;
    }

    @Override
    public TextureRegion copyPixels() {
        return new TextureRegion(buffer.getColorBufferTexture());
    }

    @Override
    public void drawFrame() {
        TextureRegion frame = copyPixels();
        frame.flip(false, true);
        batch.begin();
        batch.draw(frame,0 , 0);
        batch.end();
    }

}
