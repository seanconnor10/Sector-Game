package com.disector.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.disector.Application;
import com.disector.Material;

public abstract class DimensionalRenderer extends Renderer{
    Pixmap buffer;
    Array<Material> materials;

    public boolean drawParallax = true;
    public boolean fullBright = false;
    public boolean drawFog = true;

    public DimensionalRenderer(Application app) {
        super(app);
        materials = app.materials;
        resizeFrame(app.frameWidth, app.frameHeight);
    }

    @Override
    public void resizeFrame(int w, int h) {
        if (buffer != null) buffer.dispose();
        buffer = new Pixmap(w, h, pixelFormat);
        buffer.setColor(0x000000FF);
        frameWidth = w;
        frameHeight = h;
        halfWidth = w / 2.f;
        halfHeight = h /2.f;
        //batch.setProjectionMatrix( new Matrix4().setToOrtho2D(0,0,frameWidth,frameHeight) );
    }

    @Override
    public void drawFrame() {
        TextureRegion frame = copyPixels();
        frame.flip(false, true);
        batch.begin();
        batch.draw(frame,0 , 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.end();
        frame.getTexture().dispose();
    }

    @Override
    public TextureRegion copyPixels() {
        //Must dispose() the Texture sometime after calling this ( because of new Texture() )
        return new TextureRegion(new Texture((buffer)), buffer.getWidth(), buffer.getHeight());
    }

    public abstract boolean screenHasEmptySpace();
}
