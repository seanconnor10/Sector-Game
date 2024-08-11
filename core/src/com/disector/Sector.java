
package com.disector;

import com.badlogic.gdx.utils.IntArray;

public class Sector {
    public IntArray walls = new IntArray();
    public float floorZ = 0f, ceilZ = 100f;
    public int matFloor, matCeil;
    public float lightFloor = 1.f, lightCeil = 1.f;

    public static final Sector BLANK_SECTOR = new Sector();

    public Sector() {
    }

    public Sector(Sector s, boolean copyWalls) {
        if (copyWalls) {
            this.walls = new IntArray(s.walls.size);
            for (int wInd : s.walls.toArray()) {
                this.walls.add(wInd);
            }
        }
        this.floorZ     =   s.floorZ;
        this.ceilZ      =   s.ceilZ;
        this.matFloor   =   s.matFloor;
        this.matCeil    =   s.matCeil;
        this.lightCeil  =   s.lightCeil;
        this.lightFloor =   s.lightFloor;
    }

    public void addWallSafely(int wInd) {
        if (!walls.contains(wInd))
            walls.add(wInd);
    }

    public int[] walls() {
        return walls.toArray();
    }

    public void removeDuplicateIndices() {
        if (walls.size<2) return;
        walls.sort();
        int thisValue = walls.items[0];
        for (int i=1; i<walls.size; i++) {
            if (walls.items[i] == thisValue) {
                walls.removeIndex(i);
                i--; // :)
            } else {
                thisValue = walls.items[i];
            }
        }
    }

}