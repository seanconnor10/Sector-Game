package com.disector.renderer.sprites;

import com.badlogic.gdx.graphics.Pixmap;

public class FacingSprite extends Sprite{
    public float width;
    public float height;

    public FacingSprite(Pixmap img, float x, float y, float z, float width, float height) {
        super(img, x, y, z);
        this.width = width;
        this.height = height;
    }
}
