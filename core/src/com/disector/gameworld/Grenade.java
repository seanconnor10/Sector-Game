package com.disector.gameworld;

import com.badlogic.gdx.math.Vector3;
import com.disector.gameworld.components.PhysicsProperties;
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

	private static final PhysicsProperties PHYS_PROPS = new PhysicsProperties(
	0.7f, 0.8f, 0.7f, 1.5f, 50.0f, 0.25f
	);

	int currentSectorIndex = 0;

	Vector3 pos = new Vector3();

	Vector2 velocity = new Vector2();
	float zSpeed;

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

	/*@Override
	public Vector2 snagPosition() {
		return position;
	}

	@Override
	public void setZ(float z) {
		this.z = z;
	}

	@Override
	public Vector2 copyPosition() {return new Vector2(position);}

		@Override
	public float getZ() {
		return z;
	}*/

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

