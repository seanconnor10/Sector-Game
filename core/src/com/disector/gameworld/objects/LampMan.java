package com.disector.gameworld.objects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.disector.assets.SoundManager;
import com.disector.gameworld.components.HasFacingSprite;
import com.disector.gameworld.components.Movable;
import com.disector.gameworld.components.PhysicsProperties;
import com.disector.renderer.sprites.FacingSprite;

public class LampMan implements Movable, HasFacingSprite {
    private static Pixmap PIXMAP = new Pixmap(Gdx.files.local("assets/img/lamp.png"));
    private static final float RADIUS = 6f, HEIGHT = 32f;
    private static final PhysicsProperties PHYS_PROPS = new PhysicsProperties(
            0.9f, 0.8f, 0.7f, 0.5f, 10.0f, 1.0f
    );

    public int currentSectorIndex = 0;

    public final Vector3 pos = new Vector3();
    public final Vector2 velocity = new Vector2();
    public float zSpeed = 0f;
    public float r = 0f; //Angle

    public boolean onGround;

    public LampMan() {
        SoundManager.loopPosition(SoundManager.SFX_LampSpeech, pos, 400);
    }

    @Override
    public FacingSprite getInfo() {
        return new FacingSprite(PIXMAP, pos.x, pos.y, pos.z, RADIUS*2, HEIGHT);
    }

    @Override
    public Vector2 getVelocity() {
        return velocity;
    }

    @Override
    public float getZSpeed() {
        return zSpeed;
    }

    @Override
    public void setZSpeed(float zSpeed) {
        this.zSpeed = zSpeed;
    }

    @Override
    public void setOnGround(boolean val) {
        this.onGround = val;
    }

    @Override
    public boolean isOnGround() {
        return onGround;
    }

    @Override
    public PhysicsProperties getProps() {
        return PHYS_PROPS;
    }

    @Override
    public Vector3 pos() {
        return pos;
    }

    @Override
    public float getHeight() {
        return HEIGHT;
    }

    @Override
    public float getRadius() {
        return RADIUS;
    }

    @Override
    public int getCurrentSector() {
        return currentSectorIndex;
    }

    @Override
    public void setCurrentSector(int sInd) {
        this.currentSectorIndex = sInd;
    }
}
