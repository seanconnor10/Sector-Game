package com.disector;

import com.badlogic.gdx.math.Vector2;

public class Wall {
    //When adding more members, add to
    // copy constructor,
    //and MapLoaders' save() and load()
    public float x1, y1, x2, y2;
    public boolean isPortal;
    public int linkA, linkB;
    public float normalAngle; //Angle of line protruding outward perpendicularly from wall into the sector
    public int mat, matUpper, matLower;
    public float light = 1.f;
    public float xOffset = 0.f, yOffset = 0.f;
    public float xScale = 1.f, yScale = 1.f;

    public Wall() {

    }

    public Wall(float x1, float y1, float x2, float y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        setNormalAngle();
    }

    public Wall(Wall w) {
        this.x1 = w.x1;
        this.y1 = w.y1;
        this.x2 = w.x2;
        this.y2 = w.y2;
        this.isPortal = w.isPortal;
        this.linkA = w.linkA;
        this.linkB = w.linkB;
        this.mat = w.mat;
        this.matUpper = w.matUpper;
        this.matLower = w.matLower;
        this.light = w.light;
        this.xOffset = w.xOffset;
        this.yOffset = w.yOffset;
        this.xScale = w.xScale;
        this.yScale = w.yScale;
        setNormalAngle();
    }

    public void setFromCopy(Wall w) {
        this.x1 = w.x1;
        this.y1 = w.y1;
        this.x2 = w.x2;
        this.y2 = w.y2;
        this.isPortal = w.isPortal;
        this.linkA = w.linkA;
        this.linkB = w.linkB;
        this.mat = w.mat;
        this.matUpper = w.matUpper;
        this.matLower = w.matLower;
        this.light = w.light;
        this.xOffset = w.xOffset;
        this.yOffset = w.yOffset;
        this.xScale = w.xScale;
        this.yScale = w.yScale;
        setNormalAngle();
    }

    public float length() {
        if (x1 == x2)
            return Math.abs(y2-y1); //(y2 > y1) ? y2-y1 : y1-y2;
        else if (y1 == y2)
            return Math.abs(x2-x1); //(x2 > x1) ? x2-x1 : x1-x2;

        return (float) Math.sqrt( (x2-x1)*(x2-x1) + (y2-y1)*(y2-y1) );
    }

    public void setNormalAngle() {
        if (x1 == x2) {
            normalAngle = (y2>y1) ? 0.f : (float) Math.PI;
            return;
        }
        if (y1 == y2) {
            normalAngle = (x2>x1) ? (float) Math.PI*1.5f : (float) Math.PI*0.5f;
            return;
        }
        normalAngle = (float) -Math.atan2(x2-x1, y2-y1);
    }

    public Vector2 findNearestTo(Vector2 point) {
        //Project Vector that is the playerPosition Relative to the wall origin onto the vector that
        // is the Wall's point2 relative to its point1
        // ... DotProduct divided by length of the wall (squared for some reason?) To not take sqrt of numerator..
        float projection = ( (point.x-x1)*(x2-x1) + (point.y-y1)*(y2-y1) ) / (float) Math.pow( length(), 2);
        projection = Math.min(0.99f, Math.max(0.01f, projection));
        return new Vector2(x1 + ((x2-x1)*projection), y1 + ( (y2-y1)*projection));
    }

}
