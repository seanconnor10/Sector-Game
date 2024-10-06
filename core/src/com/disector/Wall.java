package com.disector;

import com.badlogic.gdx.math.Vector2;

public class Wall {
    //When adding more members, add to
    // copy constructor (now the setFromCopy() method) ,
    //and MapLoaders' save() and load()
    public float x1, y1, x2, y2;
    public boolean isPortal;
    public int linkA, linkB;
    public float normalAngle; //Angle of line protruding outward perpendicularly from wall into the sector
    public int mat, matUpper, matLower;
    public float light = 1.f;
    public float lightUpper = 1f;
    public float lightLower = 1.f;
    public float xOffset = 0.f, yOffset = 0.f;
    public float xScale = 1.f, yScale = 1.f;
    public float Upper_xOffset = 0.f, Upper_yOffset = 0.f;
    public float Upper_xScale = 1.f, Upper_yScale = 1.f;
    public float Lower_xOffset = 0.f, Lower_yOffset = 0.f;
    public float Lower_xScale = 1.f, Lower_yScale = 1.f;

    public PortalType type = PortalType.NORMAL;

    public enum PortalType {
        NORMAL, TEXTURE, TEXTURE_BLOCK_OBJECT, TEXTURE_BLOCK_ALL, TELEPORT;
    }

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
        setFromCopy(w);
    }

    public void setFromCopy(Wall w) {
        this.x1 = w.x1;
        this.y1 = w.y1;
        this.x2 = w.x2;
        this.y2 = w.y2;
        this.isPortal = w.isPortal;
        this.type = w.type;
        this.linkA = w.linkA;
        this.linkB = w.linkB;
        this.copyTexProps(w);
        setNormalAngle();
    }
    
    public void copyTexProps(Wall src) {
        this.mat = src.mat;
        this.matUpper = src.matUpper;
        this.matLower = src.matLower;
        this.light = src.light;
        this.lightUpper = src.lightUpper;
        this.lightLower = src.lightLower;
        this.xOffset = src.xOffset;
        this.yOffset = src.yOffset;
        this.xScale = src.xScale;
        this.yScale = src.yScale;
        this.Upper_xOffset = src.Upper_xOffset;
        this.Upper_yOffset = src.Upper_yOffset;
        this.Upper_xScale = src.Upper_xScale;
        this.Upper_yScale = src.Upper_yScale;
        this.Lower_xOffset = src.Lower_xOffset;
        this.Lower_yOffset = src.Lower_yOffset;
        this.Lower_xScale = src.Lower_xScale;
        this.Lower_yScale = src.Lower_yScale;
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
        // ... DotProduct divided by length of the wall (squared for some reason?) To not take sqrt of numerator...
        float projection = ( (point.x-x1)*(x2-x1) + (point.y-y1)*(y2-y1) ) / (float) Math.pow( length(), 2);
        projection = Math.min(0.99f, Math.max(0.01f, projection));
        return new Vector2(x1 + ((x2-x1)*projection), y1 + ( (y2-y1)*projection));
    }

}
