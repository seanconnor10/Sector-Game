package com.disector.gameworld.components;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public interface Movable extends Positionable{

    /************ :D ********************
     * Interface to return velocity components
     * for GameWorld to act upon
     * Extends on Positionable
     ************************* D: ******/

    Vector2 getVelocity();
    float getZSpeed();
    void setZSpeed(float zSpeed);
    //void setZ(float z);
    void setOnGround(boolean val);
    boolean isOnGround();
    PhysicsProperties getProps();
}
