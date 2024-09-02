package com.disector.gameworld.components;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public interface Positionable {

    /************ :D ********************
     * Interface to provide info needed for GameWorld/App to locate object
     * and find it's currentSector
     ************************* D: ******/

    Vector2 snagPosition();
    Vector2 copyPosition();
    float getZ();
    float getHeight();
    float getRadius();
    int getCurrentSector();
    void setCurrentSector(int sInd);

}
