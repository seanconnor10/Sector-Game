package com.disector.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PixmapIO;
import com.disector.Application;

public class AnimationSavingSoftwareRenderer extends SoftwareRenderer {
    private boolean writing;
    private int animFrame = 0;
    private int pixelsDrawn = 0;
    private static final int FRAME_LIMIT_FOR_ANIM_OUTPUT = 500;
    private static int saveAnimFrameEveryNPixels = 100;

    public AnimationSavingSoftwareRenderer(Application app) {
        super(app);
    }

    @Override
    protected void resetDrawData() {
        super.resetDrawData();
        writing = Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) && Gdx.input.isKeyJustPressed(Input.Keys.P);
        animFrame = 0;
        pixelsDrawn = 0;
        saveAnimFrameEveryNPixels = frameWidth * frameHeight / FRAME_LIMIT_FOR_ANIM_OUTPUT;
        buffer.fill();
    }

    @Override
    protected void setPixel(int x, int y, Color color) {
        super.setPixel(x, y, color);
        saveImg();
    }

    private void saveImg() {
        pixelsDrawn++;
        if (!writing || (pixelsDrawn % saveAnimFrameEveryNPixels != 0) ) return;
        FileHandle file = new FileHandle( "OUTPUT/" + animFrame + ".png");
        PixmapIO.writePNG(file, buffer);
        animFrame++;
    }
}
