package com.disector.gameworld;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import com.disector.gameworld.components.Movable;
import com.disector.inputrecorder.InputChainInterface;
import com.disector.inputrecorder.InputRecorder;

public class Player implements Movable {
    private final GameWorld world;
    
    private final InputChainInterface input;

    Vector2 position = new Vector2(0.f, 0.f);
    float z, r;
    float vLook; // 'Angle' of vertical view direction
    float height;
    Vector2 velocity = new Vector2(0.f, 0.f);
    float zSpeed;
    int currentSectorIndex;
    boolean onGround;

    final float MAX_SPEED = 100.f, ACCEL = 10.0f;
    final float MOUSE_SENS_X = 0.002f, MOUSE_SENS_Y = 0.5f;
    final float TURN_SPEED = 3.0f, VLOOK_SPEED = 200.0f;
    final float VLOOK_CLAMP = 300.f;
    final int STANDING_HEIGHT = 20;
    final int CROUCHING_HEIGHT = 5;
    final float RADIUS = 5.f;

    Player(GameWorld world, InputChainInterface input) {
        this.world = world;
        this.input = input;
    }

    public Vector2 movementInput(float dt) {
        Vector2 startingPosition = copyPosition();

        //Record needed button presses
        boolean forwardDown   = input.getActionInfo("FORWARD")   .isDown;
        boolean leftDown      = input.getActionInfo("LEFT")      .isDown;
        boolean rightDown     = input.getActionInfo("RIGHT")     .isDown;
        boolean backwardDown  = input.getActionInfo("BACKWARD")  .isDown;
        boolean turnLeftDown  = input.getActionInfo("TURN_LEFT") .isDown;
        boolean turnRightDown = input.getActionInfo("TURN_RIGHT").isDown;
        boolean lookUpDown    = input.getActionInfo("LOOK_UP")   .isDown;
        boolean lookDownDown  = input.getActionInfo("LOOK_DOWN") .isDown;

        //Find input vector
        Vector2 inputVector = new Vector2(0.f, 0.f);
        if (forwardDown) inputVector.x += 1.0f;
        if (backwardDown) inputVector.x -= 1.0f;
        if (leftDown) inputVector.y += 1.0f;
        if (rightDown) inputVector.y -= 1.0f;

        //Quick Vector Normalization
        if ( inputVector.x != 0.0f && Math.abs(inputVector.x) == Math.abs(inputVector.y) ) {
            inputVector.x *= 0.707107f;
            inputVector.y *= 0.707107f;
        }

        inputVector.rotateRad(r);

        //Update velocity with input vector
        velocity.add( new Vector2(inputVector).scl(ACCEL) );
        float currentSpeed = velocity.len();
        if (currentSpeed > MAX_SPEED) velocity.setLength(MAX_SPEED);

        //Friction when not inputting
        if (inputVector.isZero(0.05f)) velocity.scl( 1.f - 3.f*dt);

        //Rotate player + look up and down
        if (Gdx.input.isCursorCatched()) {
            r -= InputRecorder.mouseDeltaX * MOUSE_SENS_X;
            vLook -= InputRecorder.mouseDeltaY * MOUSE_SENS_Y;
        }
        if (turnLeftDown) r += TURN_SPEED*dt;
        if (turnRightDown) r -= TURN_SPEED*dt;
        if (lookUpDown) vLook += VLOOK_SPEED*dt;
        if (lookDownDown) vLook -= VLOOK_SPEED*dt;
        vLook = Math.min( Math.max(vLook, -VLOOK_CLAMP), VLOOK_CLAMP );

        //Crouching
        height = (input.isJustPressed(Input.Keys.CONTROL_LEFT)) ? CROUCHING_HEIGHT : STANDING_HEIGHT;

        //Jump
        if (onGround && input.isJustPressed(Input.Keys.SPACE))
            zSpeed = 100.0f;

        //Return starting position for collision function to use
        return startingPosition;
    }

    //Positionable Implementations //////////////
    @Override
    public Vector2 copyPosition() {
        return new Vector2(position);
    }

    @Override
    public Vector3 copyPosition3D() {
        return new Vector3(position.x, position.y, z);
    }

    @Override
    public float getZ() {
        return z;
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
    public float getHeight() {
        return height;
    }

    @Override
    public float getRadius() {
        return RADIUS;
    }

    //Movable Implementations ////////////////
    @Override
    public Vector2 snagVelocity() {
        return velocity;
    }

    @Override
    public float getZSpeed() {
        return zSpeed;
    }

    @Override
    public Vector2 snagPosition() {
        return position;
    }

    @Override
    public Vector2 copyVelocity() {
        return new Vector2(velocity);
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
        onGround = val;
    }

    @Override
    public boolean isOnGround() {
        return onGround;
    }
}