package com.disector.gameworld;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import com.disector.Sector;
import com.disector.assets.SoundManager;
import com.disector.gameworld.components.Movable;
import com.disector.gameworld.components.PhysicsProperties;
import com.disector.inputrecorder.InputChainInterface;
import com.disector.inputrecorder.InputRecorder;

public class Player implements Movable {
    private final GameWorld world;
    
    private final InputChainInterface input;

    final float MAX_SPEED = 150.f, ACCEL = 200.0f;
    final float MAX_SPEED_SLOW = 60.f;
    final float MOUSE_SENS_X = 0.002f, MOUSE_SENS_Y = 0.1f;
    final float TURN_SPEED = 3.0f, VLOOK_SPEED = 30.0f;
    final float VLOOK_ANGLE_CLAMP = 60f;

    final float CROUCH_SPEED = 100;
    final float HEADSPACE = 3;
    final float STANDING_HEIGHT = 25;
    final float CROUCHING_HEIGHT = 5;
    final float RADIUS = 5.f;

    final PhysicsProperties phys_props = new PhysicsProperties(
    0.5f, 0.9f, 0.5f, 0.0f, 300.0f, 1f
    );

    public Vector3 pos = new Vector3();
    public float r; //(IN RADIANS!)
    public float v_angle; // Actual angle of vertical look (IN DEGREES!)
    float height = STANDING_HEIGHT;

    Vector2 velocity = new Vector2(0.f, 0.f);
    float zSpeed;

    public float zoom = 1f;

    int currentSectorIndex;
    boolean onGround;

    Player(GameWorld world, InputChainInterface input) {
        this.world = world;
        this.input = input;
    }

    public void movementInput(float dt) {
        //Record needed button presses
        boolean forwardDown   = input.getActionInfo("FORWARD")   .isDown;
        boolean leftDown      = input.getActionInfo("LEFT")      .isDown;
        boolean rightDown     = input.getActionInfo("RIGHT")     .isDown;
        boolean backwardDown  = input.getActionInfo("BACKWARD")  .isDown;
        boolean turnLeftDown  = input.getActionInfo("TURN_LEFT") .isDown;
        boolean turnRightDown = input.getActionInfo("TURN_RIGHT").isDown;
        boolean lookUpDown    = input.getActionInfo("LOOK_UP")   .isDown;
        boolean lookDownDown  = input.getActionInfo("LOOK_DOWN") .isDown;

        boolean crouch        = input.isDown(Input.Keys.CONTROL_LEFT);
        zoom = Gdx.input.isButtonPressed(Input.Buttons.RIGHT) ? 4 : 1;

        float dtSQRT = (float)Math.sqrt(dt);

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
        float top_speed = (height == STANDING_HEIGHT && !input.isDown(Input.Keys.SHIFT_LEFT)) ? MAX_SPEED : MAX_SPEED_SLOW;
        Vector2 impulse = new Vector2(inputVector).scl(ACCEL * dtSQRT);
        Vector2 newVel = new Vector2(velocity).add(impulse);
        if (velocity.len() < top_speed || newVel.len2()<velocity.len2()) {
            velocity.add(impulse);
        }

        //Friction
        float friction;
        if (!onGround) {
            friction = 0.01f;
        }
        else if (inputVector.isZero(0.05f)) {
            friction = 0.8f;
        } else {
            float speedAngle = (float) Math.atan2(velocity.x, velocity.y);
            float velAngle = (float) Math.atan2(inputVector.x, inputVector.y) ;
            if (speedAngle>Math.PI) speedAngle -= (float) Math.PI*2;
            if (speedAngle<-Math.PI) speedAngle += (float) Math.PI*2;
            if (velAngle>Math.PI) velAngle -= (float) Math.PI*2;
            if (velAngle<-Math.PI) velAngle += (float) Math.PI*2;
            float angleDifference = (float) Math.min( Math.abs(speedAngle-velAngle), Math.abs(Math.PI-speedAngle - (Math.PI-velAngle) ) );
            while(angleDifference>Math.PI) angleDifference -= (float) Math.PI*2;
            while(angleDifference<-Math.PI) angleDifference += (float) Math.PI*2;
            float lerp = (float) ( Math.abs(angleDifference)/Math.PI );
            friction = 0.5f + lerp*0.3f;
        }
        velocity.scl(1f - friction * dtSQRT);

        //Rotate player + look up and down
        if (Gdx.input.isCursorCatched()) {
            r -= InputRecorder.mouseDeltaX * MOUSE_SENS_X;
            v_angle -= InputRecorder.mouseDeltaY * MOUSE_SENS_Y;
        }
        if (turnLeftDown) r += TURN_SPEED*dt;
        if (turnRightDown) r -= TURN_SPEED*dt;
        if (lookUpDown) v_angle += VLOOK_SPEED*dt;
        if (lookDownDown) v_angle -= VLOOK_SPEED*dt;
        v_angle = Math.min( Math.max(v_angle, -VLOOK_ANGLE_CLAMP), VLOOK_ANGLE_CLAMP );

        //Crouching
        if (crouch && height != CROUCHING_HEIGHT) {
            float prevHeight = height;
            height = Math.max(height - CROUCH_SPEED*dt, CROUCHING_HEIGHT);
            pos.z += (prevHeight-height);
        } else if (!crouch && height != STANDING_HEIGHT) {
            Sector cur = world.sectors.get(currentSectorIndex);
            if (cur.ceilZ-cur.floorZ >= STANDING_HEIGHT+HEADSPACE) {
                float prevHeight = height;
                height = Math.min(height + CROUCH_SPEED * dt, STANDING_HEIGHT);
                pos.z += (prevHeight-height);
            }
        }

        //Jump
        if (onGround && input.isJustPressed(Input.Keys.SPACE))
            zSpeed = 100.0f;

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
        return height + HEADSPACE;
    }

    @Override
    public float getRadius() {
        return RADIUS;
    }

    @Override
    public Vector3 pos() {
        return pos;
    }

    //Movable Implementations ////////////////
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
        onGround = val;
    }

    @Override
    public boolean isOnGround() {
        return onGround;
    }

    @Override
    public PhysicsProperties getProps() {
        return phys_props;
    }
}
