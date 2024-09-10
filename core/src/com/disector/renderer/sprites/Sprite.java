package com.disector.renderer.sprites;

import com.badlogic.gdx.graphics.Pixmap;

public class Sprite {
    public Pixmap img;
    public float x,y,z;

    public float depth;

    public boolean blocksMovement, blocksProjectile, blocksHitscan;

    public enum TYPE {
        FACING, WALL;
    }

    public TYPE type;

    public Sprite(Pixmap img, float x, float y, float z) {
        this.img = img;
        this.x = x;
        this.y = y;
        this.z = z;

        switch (this.getClass().getSimpleName()) {
            case "FacingSprite":
                type = TYPE.FACING;
                break;
            case "WallSprite":
                type = TYPE.WALL;
                break;
            default:
                break;
        }
    }
}
