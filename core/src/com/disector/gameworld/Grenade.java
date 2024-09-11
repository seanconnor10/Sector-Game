package com.disector.gameworld;

import com.disector.renderer.sprites.FacingSprite;
import com.disector.gameworld.components.Movable;
import com.disector.gameworld.components.HasFacingSprite;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.Vector2;

class Grenade implements Movable, HasFacingSprite {
	//Used for Collision Checking
	private static final float RADIUS = 3f, HEIGHT = 3f;

	private static final Pixmap PIXMAP = new Pixmap(Gdx.files.local("assets/img/grenade.png"));
	private static final int IMG_W = 10, IMG_H = 10;

	int currentSectorIndex = 0;

	Vector2 position = new Vector2();
	float z;

	Vector2 velocity = new Vector2();
	float zSpeed;

	@Override
	public FacingSprite getInfo() {
		return new FacingSprite(PIXMAP, position.x, position.y, z, IMG_W, IMG_H);
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
	public void setZ(float z) {
		this.z = z;
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
	public Vector2 snagPosition() {
		return position;
	}

	@Override
	public Vector2 copyPosition() {
		return new Vector2(position);
	}

	@Override
	public float getZ() {
		return z;
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
		currentSectorIndex = sInd;
	}
}

