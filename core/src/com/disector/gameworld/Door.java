package com.disector.gameworld;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.disector.Sector;
import com.disector.Wall;
import com.disector.assets.SoundManager;

/*
    Maybe a group of SectionEffect interface and Objects that use them..
    IE. toggleableEffect, or something.. so that objects can toggle other objects
 */

class Door {
    private int sectorIndex;
    private final Array<Wall> walls = new Array<>();

    final static float SPEED = 8f;
    final static float TIME_CYCLE = 1f;

    float maxDistance = 64f;
    float currentDistance = 0f;
    float timeSpentThisState = 0f;
    int state = 0; //0:StayOff 1:Forward 2:StayOn 3: Backward

    SoundManager.PositionableSound movingSound;
    Vector3 soundPosition;

    public Door(int sectorIndex, Sector sector, Array<Wall> worldWalls) {
        this.sectorIndex = sectorIndex;
        int[] wallIndices = sector.walls();
        for (int wallIndex : wallIndices) {
            this.walls.add(worldWalls.get(wallIndex));
        }
        soundPosition = new Vector3(walls.get(0).x1, walls.get(0).y1, (sector.floorZ+sector.ceilZ)/2.f);
    }

    void step(float dt) {
        soundPosition.x = walls.get(0).x1;
        soundPosition.y =walls.get(0).y1;
        float distance;
        switch (state) {
            case 0:
                timeSpentThisState += dt;
                /*if (timeSpentThisState > TIME_CYCLE) {
                    timeSpentThisState = 0f;
                    state = 1;
                    startMovingSoundLoop();
                    playStartMovingSound();
                }*/
                break;
            case 1:
                if (currentDistance >= maxDistance) {
                    currentDistance = maxDistance;
                    timeSpentThisState = 0f;
                    state = 2;
                    stopMovingSoundLoop();
                    playStopMovingSound();
                    return;
                }
                distance = Math.min(SPEED * dt, maxDistance - currentDistance);
                for (Wall wall : walls) {
                    wall.x1 -= distance;
                    wall.x2 -= distance;
                }
                currentDistance += distance;
                break;
            case 2:
                timeSpentThisState += dt;
                if (timeSpentThisState > TIME_CYCLE) {
                    state = 3;
                    timeSpentThisState = 0f;
                    startMovingSoundLoop();
                    playStartMovingSound();
                }
                break;
            case 3:
                if (currentDistance <= 0f) {
                    currentDistance = 0f;
                    timeSpentThisState = 0f;
                    state = 0;
                    stopMovingSoundLoop();
                    playStopMovingSound();
                    return;
                }
                distance = Math.min(SPEED * dt, currentDistance);
                for (Wall wall : walls) {
                    wall.x1 += distance;
                    wall.x2 += distance;
                }
                currentDistance -= distance;
                break;
        }
    }

    void toggle() {
        if (state == 0 ) {
            timeSpentThisState = 0f;
            state = 1;
            startMovingSoundLoop();
            playStartMovingSound();
        }
    }

    private void startMovingSoundLoop() {
        movingSound = SoundManager.loopPosition(SoundManager.SFX_Mechanical, soundPosition, 400f);
    }
    private void stopMovingSoundLoop() {
        if (movingSound != null) SoundManager.killLoopingPositionable(movingSound);
    }

    private void playStopMovingSound() {
        SoundManager.playPosition(SoundManager.SFX_MetalDoorThud, soundPosition, 400f);
    }

    private void playStartMovingSound() {
        SoundManager.playPosition(SoundManager.SFX_DoorLatch, soundPosition, 150f);
    }
}