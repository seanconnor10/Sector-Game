package com.disector.gameworld.objects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.Vector3;

import com.disector.gameworld.components.HasWallSprite;
import com.disector.gameworld.components.Positionable;
import com.disector.renderer.sprites.WallSprite;

public class PaintSplotch implements Positionable, HasWallSprite {
    private static final float DIST = 2f;
    private static final float SIZE = 10f;
    private static final Pixmap PIXMAP = new Pixmap(Gdx.files.local("assets/img/splat.png"));

    private final Vector3 pos = new Vector3();
    
    private float x1,y1,z,x2,y2,height;

    public PaintSplotch(float hitX, float hitY, float hitZ, float wallNormal) {
        float x,y;
        x = hitX + (float) Math.cos(wallNormal)*DIST;
        y = hitY + (float) Math.sin(wallNormal)*DIST;

        pos.set(x, y, hitZ);

        float wallAngle = wallNormal + (float) Math.PI/2f;

        x1 = x - (float) Math.cos(wallAngle)*SIZE;
        y1 = hitY - (float) Math.sin(wallAngle)*SIZE;
        z = hitZ-SIZE;
        x2 = x + (float) Math.cos(wallAngle)*SIZE;
        y2 = hitY + (float) Math.sin(wallAngle)*SIZE;
        height = SIZE*2;
    }

    @Override
    public WallSprite getInfo() {
        return new WallSprite(PIXMAP, x1,y1,z,x2,y2,height);
    }

    @Override
    public int getCurrentSector() {
        return 0;
    }

    @Override
    public Vector3 pos() {
        return pos;
    }

    @Override
    public float getHeight() {
        return SIZE*2;
    }

    @Override
    public float getRadius() {
        return SIZE;
    }

    @Override
    public void setCurrentSector(int sInd) {

    }
}
