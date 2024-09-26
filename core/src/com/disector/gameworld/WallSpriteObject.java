package com.disector.gameworld;

public class WallSpriteObject {
    public float x1, y1, x2 = 100, y2;
    public float z, height = 64;
    public int texInd;

    public boolean BLOCK_MOVE;
    public boolean BLOCK_HITSCAN;
    public boolean BLOCK_PROJECTILE;

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

    public WallSpriteObject(WallSpriteObject cpysrc) {
        this.x1 = cpysrc.x1;
        this.y1 = cpysrc.y1;
        this.x2 = cpysrc.x2;
        this.y2 = cpysrc.y2;
        this.z  = cpysrc.z;
        this.height = cpysrc.height;
        this.texInd = cpysrc.texInd;
        this.BLOCK_HITSCAN = cpysrc.BLOCK_HITSCAN;
        this.BLOCK_PROJECTILE = cpysrc.BLOCK_PROJECTILE;
        this.BLOCK_MOVE = cpysrc.BLOCK_MOVE;
    }
}