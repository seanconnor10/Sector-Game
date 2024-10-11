package com.disector.gameworld.objects;

import com.badlogic.gdx.math.Vector3;
import com.disector.gameworld.components.PhysicsProperties;
import com.disector.renderer.sprites.FacingSprite;
import com.disector.gameworld.components.Movable;
import com.disector.gameworld.components.HasFacingSprite;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.Vector2;

public class Grenade implements Movable, HasFacingSprite {
	//Used for Collision Checking
	private static final float RADIUS = 3f, HEIGHT = 3f;

	private static final Pixmap PIXMAP = new Pixmap(Gdx.files.local("assets/img/grenade.png"));
	private static final int IMG_W = 10, IMG_H = 10;

	private static final PhysicsProperties PHYS_PROPS = new PhysicsProperties(
			0.7f, 0.8f, 0.7f, 1.5f, 50.0f, 0.25f
	);
	
	public int currentSectorIndex = 0;

	public Vector3 pos = new Vector3();

	public Vector2 velocity = new Vector2();
	public float zSpeed;

	public boolean damagedToExplosion;
	public float timeTillExplosion = 0.75f;

	public boolean countDown(float dt) {
		timeTillExplosion -= dt;
		return timeTillExplosion < 0;
	}

	@Override
	public FacingSprite getInfo() {
		return new FacingSprite(PIXMAP, pos.x, pos.y, pos.z, IMG_W, IMG_H);
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
		return;
	}

	@Override
	public boolean isOnGround() {
		return false;
	}

	@Override
	public Vector3 pos() {return pos;}

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
		currentSectorIndex = sInd;
	}

	@Override
	public PhysicsProperties getProps() {
		return PHYS_PROPS;
	}
}

