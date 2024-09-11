package com.disector;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.disector.gameworld.components.Movable;
import com.disector.gameworld.components.Positionable;

public class Physics {

    public static Array<Wall> walls;
    public static Array<Sector> sectors;

    public static boolean containsPoint(Sector sec, float x, float y) {
        /*
         * For each wall in Sector, casts a ray, if an odd number of walls were contacted,
         * the point is within the sector
         */
        int intersections = 0;
        for (Integer wInd : sec.walls.toArray()) {
            /*
                Giving rayWallIntersection a non-zero angle fixing containsPoint failing
                This all worked correctly in the previous SectorGame where a Wall's
                Points were Integers not Floats... Fix at some point...
             */
            Vector2 intersect = rayWallIntersection(walls.get(wInd), 1, x, y, false);
            if (intersect != null /*&& intersect.x > x*/)
                intersections++;
        }

        return (intersections%2 == 1);
    }

    public static boolean containsPoint(int sInd, float x, float y) {
        if (sInd >= sectors.size || sInd < 0)
            return false;
        return containsPoint(sectors.get(sInd), x, y);
    }

    public static int findCurrentSectorBranching(int startIndex, float x, float y) {
        boolean secIndValid = (startIndex >= 0 && startIndex < sectors.size);

        if (secIndValid && containsPoint(sectors.get(startIndex), x, y)) return startIndex;

        Array<Integer> checkedSectors = new Array<>( sectors.size );
        Array<Integer> sectorsToCheck = new Array<>(16);

        checkedSectors.add(startIndex);
        sectorsToCheck.add(startIndex);

        //Loop, stopping when either all sectors are checked or there are no more portal connections to unchecked sectors
        if ( secIndValid ) {
             while (checkedSectors.size < sectors.size && sectorsToCheck.size > 0) {

                 //Check this pass of sectors for our position, and return once found
                 for (Integer indS : sectorsToCheck) {
                     checkedSectors.add(indS);
                     if (containsPoint(sectors.get(indS), x, y)) {
                         return indS;
                     }
                 }

                 //Temporarily store the sectors we are checking this pass,
                 // so we can manipulate sectorsToCheck
                 int[] sectorsThisRound = new int[sectorsToCheck.size];
                 for (int i = 0; i < sectorsThisRound.length; i++) {
                     sectorsThisRound[i] = sectorsToCheck.get(i);
                 }

                 //Clear once stored
                 sectorsToCheck.clear();

                 //Find linked sectors that arent yet checked
                 for (int j : sectorsThisRound) {
                     for (Integer indW : sectors.get(j).walls.toArray()) {
                         Wall wall = walls.get(indW);
                         if (wall.isPortal) {
                             if (!checkedSectors.contains(wall.linkA, false)) {
                                 sectorsToCheck.add(wall.linkA);
                                 //Added to checkedSectors at start of next loop
                             }
                             if (!checkedSectors.contains(wall.linkB, false)) {
                                 sectorsToCheck.add(wall.linkB);
                                 //Added to checkedSectors at start of next loop
                             }
                         }
                     }
                 }
             } //End while loop
        }

        for (int i=0; i<sectors.size; i++) {
            if (containsPoint(i, x, y))
                return i;
        }

        return startIndex; //If no match found, don't change..
    }

    public static Vector2 rayWallIntersection(Wall w, float angle, float rayX, float rayY, boolean allowBehind) {
        /*
         * Returns the position of an intersection between a ray and a Wall
         * If allowBehind is enabled, the ray is cast backwards as well
         */

        float raySlope = angle == 0.0 ? 0.f : (float) ( Math.sin(angle) / Math.cos(angle) );

        //If wall is horizontal
        if (w.y1 == w.y2) {
            if (!allowBehind) {
                if (Math.sin(angle) > 0.0 && rayY > w.y1) return null;
                if (Math.sin(angle) < 0.0 && rayY < w.y1) return null;
            }
            float deltaY = w.y1-rayY;
            float intersectX = rayX + deltaY/raySlope;
            if (!isBetween(intersectX, w.x1, w.x2)) return null;
            return new Vector2(intersectX, w.y1);
        }

        //Is wall is vertical
        if (w.x1 == w.x2){
            if (!allowBehind) {
                if (Math.cos(angle) > 0.0 && rayX > w.x1) return null;
                if (Math.cos(angle) < 0.0 && rayY < w.x1) return null;
            }

            float deltaX = w.x1-rayX;
            float intersectY = rayY + deltaX*raySlope;
            if (!isBetween(intersectY, w.y1,w.y2)) return null;
            return new Vector2(w.x1, intersectY);
        }

        //If wall is neither vertical nor horizontal
        // "Given two line equations" Method from Wikipedia
        //ax+c=bx+d // a=wallSlope c=wallIntercept b=raySlope d=rayIntercept // iX = (d-c)/(a-b)
        float wallSlope = (w.y2-w.y1) / (w.x2-w.x1);
        float wallYIntercept = w.y1 - wallSlope*w.x1;
        float rayYIntercept = rayY - raySlope*rayX;
        float iX = (rayYIntercept-wallYIntercept) / (wallSlope-raySlope);
        float iY = (wallSlope*iX) + wallYIntercept;

        if ( ! (isBetween(iX,w.x1, w.x2) && isBetween(iY,w.y1,w.y2)) ) return null;

        //Check if actually behind
        if (!allowBehind) {
            if (Math.sin(angle) > 0.0 && iY < rayY) return null;
            if (Math.sin(angle) < 0.0 && iY > rayY) return null;
        }

        return new Vector2(iX,iY);

    }

    public static boolean isBetween(float var, float bound1, float bound2) {
        /*
            Including only the second bound is ESSENTIAL for ensuring
            a ray does not collide at the join of the walls and think
            it is hitting two separate walls!
         */
        if (bound2 > bound1)
            return (var > bound1 && var <= bound2);
        return (var >= bound2 && var < bound1);
        //return (var > bound1) ^ (var > bound2); //May or may not include bounds
    }

    public static Vector2 bounceVector(Vector2 velocity, Wall wall) {
        float wallXNormal = (float) Math.cos(wall.normalAngle);
        float wallYNormal = (float) Math.sin(wall.normalAngle);
        float proj_norm = velocity.x * wallXNormal + velocity.y * wallYNormal;
        float perpendicularVelX = proj_norm * wallXNormal;
        float perpendicularVelY = proj_norm * wallYNormal;
        float parallelVelX = velocity.x - perpendicularVelX;
        float parallelVelY = velocity.y - perpendicularVelY;
        final float elasticity = 0.5f;
        final float restitution = 0.9f;

        return new Vector2(
            parallelVelX * restitution - perpendicularVelX * elasticity,
            parallelVelY * restitution - perpendicularVelY * elasticity
        );
    }

    public static boolean boundingBoxCheck(Wall w, Vector2 objPos, float objRadius) {
        float leftBound = Math.min(w.x1, w.x2) - objRadius;
        float rightBound = Math.max(w.x1, w.x2) + objRadius;
        float topBound = Math.min(w.y1, w.y2) - objRadius;
        float bottomBound = Math.max(w.y1, w.y2) + objRadius;
        if (objPos.x > rightBound) return false;
        if (objPos.x < leftBound) return false;
        if (objPos.y > bottomBound) return false;
        return !(objPos.y < topBound);
    }

    public static void resolveCollision(WallInfoPack collisionInfo, Movable obj) {
        Wall w = collisionInfo.w;

        float resolutionDistance = obj.getRadius() - collisionInfo.distToNearest;
        float xLast = obj.snagPosition().x - obj.getVelocity().x;
        float yLast = obj.snagPosition().y - obj.getVelocity().y;

        float scalar;


        if (w.isPortal) {
            scalar= (float) (
                    (xLast-collisionInfo.nearestPoint.x) * Math.cos(w.normalAngle)
                            + (yLast-collisionInfo.nearestPoint.y) * Math.sin(w.normalAngle)
            ); //Dot Product of Camera's position relative (on last frame) to nearest point and the wall's normalised normal vector
            if (scalar < 0) resolutionDistance = -resolutionDistance;
        }

        /*
        if (collisionInfo.w.isPortal && collisionInfo.w.linkA == obj.getCurrentSector())
            resolutionDistance *= -1;
         */

        Vector2 objPos = obj.snagPosition();
        objPos.x += (float) Math.cos(collisionInfo.w.normalAngle) * resolutionDistance;
        objPos.y += (float) Math.sin(collisionInfo.w.normalAngle) * resolutionDistance;
    }

}
