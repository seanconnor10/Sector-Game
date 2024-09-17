package com.disector.gameworld;

import com.badlogic.gdx.graphics.Pixmap;
import com.disector.renderer.sprites.WallSprite;

public class WallSpriteObject {
    float x1, y1, x2 = 100, y2;
    float z, height = 64;
    int texInd;

    boolean BLOCK_MOVE;
    boolean BLOCK_HITSCAN;

    public WallSpriteObject() {
    }

    public WallSpriteObject(float x1, float y1, float x2, float y2, float z, float height, int texInd) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.z = z;
        this.height = height;
        this.texInd = texInd;
    }

}
