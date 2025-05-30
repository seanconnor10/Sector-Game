package com.disector.editor;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

public class Scene2DTexture implements Drawable {
    Texture tex;
    int size = 64;

    public Scene2DTexture(Pixmap pix) {
        this.tex = new Texture(pix);
    }

    @Override
    public void draw(Batch batch, float x, float y, float width, float height) {
        batch.draw(tex, x, y, width, height);
    }

    @Override
    public float getLeftWidth() {
        return (float) size /2;
    }

    @Override
    public void setLeftWidth(float leftWidth) {

    }

    @Override
    public float getRightWidth() {
        return (float) size /2;
    }

    @Override
    public void setRightWidth(float rightWidth) {

    }

    @Override
    public float getTopHeight() {
        return (float) size /2;
    }

    @Override
    public void setTopHeight(float topHeight) {

    }

    @Override
    public float getBottomHeight() {
        return (float) size /2;
    }

    @Override
    public void setBottomHeight(float bottomHeight) {

    }

    @Override
    public float getMinWidth() {
        return size;
    }

    @Override
    public void setMinWidth(float minWidth) {

    }

    @Override
    public float getMinHeight() {
        return size;
    }

    @Override
    public void setMinHeight(float minHeight) {

    }
}
