package com.disector.gameworld;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector4;
import com.badlogic.gdx.utils.Array;

import com.badlogic.gdx.utils.IntArray;
import com.disector.*;
import com.disector.gameworld.components.Movable;
import com.disector.gameworld.components.Positionable;
import com.disector.inputrecorder.InputChainInterface;
import com.disector.inputrecorder.InputChainNode;
import com.disector.renderer.sprites.FacingSprite;
import com.disector.renderer.sprites.Sprite;

import static com.disector.Physics.containsPoint;
import static com.disector.Physics.findCurrentSectorBranching;

public class GameWorld implements I_AppFocus{
    private final Application app;
    private final Array<Wall> walls;
    private final Array<Sector> sectors;

    private final InputChainNode input;

    private float dt;
    private boolean shouldDisplayMap;

    public Player player1;
    public Grenade grenade;

    public GameWorld(Application app, InputChainInterface inputParent) {
        this.app = app;
        this.walls = app.walls;
        this.sectors = app.sectors;
        this.input = new InputChainNode(inputParent, "GameWorld");
        this.input.on();
        player1 = new Player(this, input);
        grenade = new Grenade();
        player1.z = 100.f;
    }

    @Override
    public InputChainInterface getInputReference() {
        return input;
    }

    public void step(float dt) {
        this.dt = dt;

        if (input.getActionInfo("DISPLAY_MAP").justPressed)
            shouldDisplayMap = !shouldDisplayMap;

        player1.movementInput(dt);
        moveObj(player1);
        moveObj(grenade);
        if (input.isJustPressed(Input.Keys.G)) {
            grenade.position.set(player1.position);
            grenade.z = player1.z - player1.CROUCHING_HEIGHT;
            grenade.velocity.set( (float) Math.cos(player1.r) * 300, (float) Math.sin(player1.r) * 300);
            grenade.zSpeed = 32 + player1.zSpeed;
        }
    }

    //*****************************************************

    public Vector4 getPlayerPosition() {
        return new Vector4(player1.copyPosition(), player1.z, player1.r);
    }

    public float getPlayerRadius() {
        return player1.getRadius();
    }

    public int getPlayerSectorIndex() {
        return player1.currentSectorIndex;
    }

    public float getPlayerVLook() {
        return player1.vLook;
    }

    public boolean shouldDisplayMap() {
        return shouldDisplayMap;
    }

    public void setPos(Positionable obj, float x, float y) {
        obj.snagPosition().set(x,y);
    }

    public int refreshPlayerSectorIndex() {
        int sec = player1.currentSectorIndex;
        float x = getPlayerPosition().x;
        float y = getPlayerPosition().y;

        try {
            if (Physics.containsPoint(sectors.get(sec), x, y))
                return sec;
        } catch (IndexOutOfBoundsException indexException) {
            System.out.println("Player1 currentSectorIndex nonexistent");
        }

        sec = Physics.findCurrentSectorBranching(getPlayerSectorIndex(), x, y);

        /*sec = 0;

        for (int i=0; i<sectors.size; i++) {
            if (Physics.containsPoint(sectors.get(i), x, y)) {
                sec = i;
                break;
            }
        }*/

        player1.currentSectorIndex = sec;
        return sec;
    }

    public Sprite[] getSpriteList() {
        Sprite[] arr = new Sprite[1];
        arr[0] = grenade.getInfo();
        return arr;
    }

    //*****************************************************

    private void moveObj(Movable obj) {
        final int MAX_COLLISIONS = 100;

        /*
         * Takes any game object with a position and velocity and moves it,
         * colliding it against walls and updating its currentSectorIndex
         */
        Sector currentSector = Sector.BLANK_SECTOR;
        try {
            currentSector = sectors.get( obj.getCurrentSector() );
        } catch (IndexOutOfBoundsException indexException) {
            System.out.println("Movable-Obj's currentSectorIndex Nonexistent");
        }

        Vector2 objPos = obj.snagPosition(); //Snag grabs a reference to the Vector so we can change it
        Vector2 velocity = obj.getVelocity();

        objPos.x += velocity.x * dt;
        objPos.y += velocity.y * dt;

        float stepUpAllowance = obj.isOnGround() && obj.getZSpeed() >= 0.0f ? 10.f : 0.f;

        Array<WallInfoPack> wallsCollided;
        IntArray potentialNewSectors = new IntArray();
        float teeterHeight = currentSector.floorZ;
        float lowestCeilHeight = currentSector.ceilZ;
        int collisionsProcessed = 0;

        while (collisionsProcessed < MAX_COLLISIONS) {
            //Get all wall collisions in current sector
            wallsCollided = findCollisions(currentSector, obj);

            //Remove Portal Walls that we can vertically fit through,
            //putting the destination sector index into another array
            for (int i=0; i<wallsCollided.size; i++) {
                WallInfoPack wallInfo = wallsCollided.get(i);
                if (wallInfo.w.isPortal) {
                    int destSector = wallInfo.w.linkA;
                    if (destSector == obj.getCurrentSector())
                        destSector = wallInfo.w.linkB;
                    Sector dest = sectors.get(destSector);
                    if (heightCheck(dest, obj, stepUpAllowance)) {
                        //If bounding circle is jutting into other sectors...
                        teeterHeight = Math.max(teeterHeight, dest.floorZ);
                        lowestCeilHeight = Math.min(lowestCeilHeight, dest.ceilZ);
                        potentialNewSectors.add(destSector);
                        wallsCollided.removeIndex(i);
                        i--;
                    }
                }
            }

            //Find new currentSector from list of potentials made above
            for (int sInd : potentialNewSectors.toArray()) {
                if (containsPoint( sectors.get(sInd), objPos.x, objPos.y)) {
                    obj.setCurrentSector(sInd);
                    break;
                }
            }

            if (!containsPoint( obj.getCurrentSector(), objPos.x, objPos.y)) {
                obj.setCurrentSector(
                        findCurrentSectorBranching(obj.getCurrentSector(), objPos.x, objPos.y)
                );
            }

            if (wallsCollided.isEmpty()) break;

            wallsCollided.sort( ////Sort collided walls to get the first one we should collide with
                (WallInfoPack o1, WallInfoPack o2) -> Float.compare(o1.distToNearest, o2.distToNearest)
            );

            //Get reference to closest collision
            WallInfoPack closestCollision = wallsCollided.get(0);

            Physics.resolveCollision(closestCollision, obj);
            velocity.set(Physics.bounceVector(velocity, closestCollision.w));

            collisionsProcessed++;

        }

        obj.setOnGround(obj.getZ() < teeterHeight+0.5f);

        //Grav
        if (obj.getZ() > teeterHeight) obj.setZSpeed(obj.getZSpeed() - 200.f*dt);
        if (obj.getZSpeed() < -300.0f) obj.setZSpeed(-300.0f);
        //Enact motion
        obj.setZ( obj.getZ() + obj.getZSpeed()*dt );
        //Hit Floor
        if (obj.getZ()<teeterHeight) {
            obj.setZ(teeterHeight);
            if (obj.getZSpeed() < 0) obj.setZSpeed(0);
        }
        //HitCeiling
        if (obj.getZ()+obj.getHeight()>lowestCeilHeight) {
            obj.setZ(lowestCeilHeight-obj.getHeight());
            if (obj.getZSpeed() > 0) obj.setZSpeed(0);
        }


    }

    private boolean heightCheck(Sector s, Positionable obj, float stepUpAllowance) {
        //Return whether the obj can fit the sector height-wise
        return obj.getZ()+obj.getHeight() < s.ceilZ && obj.getZ() >= s.floorZ-stepUpAllowance;
    }

    private Array<WallInfoPack> findCollisions(Sector sector, Positionable obj) {
        Array<WallInfoPack> collisions = new Array<>();
        Vector2 objPos = obj.copyPosition();
        //For every wall in sector, check collision by bounding box
        //If collided, check collision accurately
        //and if still colliding, add to list of collisions
        for (int wInd : sector.walls.toArray()) {
            Wall w = walls.get(wInd);
            if (Physics.boundingBoxCheck(w, obj.copyPosition(), obj.getRadius())) {
                WallInfoPack info = new WallInfoPack(w, wInd, objPos);
                if (info.distToNearest < obj.getRadius()) {
                    collisions.add(info);
                }
            }
        }
        return collisions;
    }

}