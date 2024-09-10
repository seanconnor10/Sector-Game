package com.disector.renderer.sprites;

import com.badlogic.gdx.graphics.Pixmap;

public class WallSprite extends Sprite{
    public float x2, y2, height;

    public WallSprite(Pixmap img, float x, float y, float z, float x2, float y2, float height) {
        super(img, x, y, z);
        this.x2 = x2;
        this.y2 = y2;
        this.height = height;
    }
}
