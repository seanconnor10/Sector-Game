package com.disector;

import com.badlogic.gdx.math.Vector2;

public class WallInfoPack {
    public final Wall w;
    public final int wInd;
    public final Vector2 nearestPoint;
    public final float distToNearest;

    public WallInfoPack(Wall w, int wInd, Vector2 point) {
        this.w = w;
        this.wInd = wInd;
        this.nearestPoint = new Vector2( w.findNearestTo(point) );
        this.distToNearest = (float) Math.sqrt( Math.pow(nearestPoint.x-point.x, 2) + Math.pow(nearestPoint.y-point.y, 2) );
    }

    @Override
    public String toString() {
        return wInd + ") Dist: " + distToNearest;
    }

}