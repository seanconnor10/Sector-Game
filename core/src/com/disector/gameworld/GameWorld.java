package com.disector.gameworld;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.Vector4;
import com.badlogic.gdx.utils.Array;

import com.badlogic.gdx.utils.IntArray;
import com.disector.*;
import com.disector.assets.SoundManager;
import com.disector.gameworld.components.Movable;
import com.disector.gameworld.components.PhysicsProperties;
import com.disector.gameworld.components.Positionable;
import com.disector.inputrecorder.InputChainInterface;
import com.disector.inputrecorder.InputChainNode;
import com.disector.renderer.sprites.Sprite;
import com.disector.renderer.sprites.WallSprite;

import static com.disector.Physics.containsPoint;
import static com.disector.Physics.findCurrentSectorBranching;

public class GameWorld implements I_AppFocus{
    private final Application app;
    private final Array<Wall> walls;
    final Array<Sector> sectors;

    private final InputChainNode input;

    private float dt;
    private boolean shouldDisplayMap;

    public Player player1;
    public final Array<Grenade> grenades = new Array<>();
    public final Array<WallSpriteObject> wallSpriteObjects = new Array<>();

    public GameWorld(Application app, InputChainInterface inputParent) {
        this.app = app;
        this.walls = app.walls;
        this.sectors = app.sectors;
        this.input = new InputChainNode(inputParent, "GameWorld");
        this.input.on();
        player1 = new Player(this, input);
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

        for (Grenade g : grenades) {
            moveObj(g);
            if (g.velocity.isZero(1)) {
                SoundManager.playStaticPosition(SoundManager.SFX_Boom, new Vector3(g.position.x, g.position.y, g.getZ()), 500);
                //Temporary Test Control
                //Press To Toss Player around
                float dist = getPlayerXYZ().dst(g.position.x, g.position.y, g.z);
                if (dist < 100) {
                    float force = 200 * (1 - (dist / 100));
                    float angle = (float) Math.atan2(player1.position.x - g.position.x, player1.position.y -g.position.y);
                    player1.velocity.x += force * (float) Math.cos(angle);
                    player1.velocity.y += force * (float) Math.sin(angle);
                    player1.zSpeed     += force;
                }
                grenades.removeValue(g, true);
            }
        }

        if (input.isJustPressed(Input.Keys.G)) {
            Grenade grenade = new Grenade();
            grenade.currentSectorIndex = player1.currentSectorIndex;
            grenade.position.set(player1.position);
            grenade.z = player1.z - player1.CROUCHING_HEIGHT - 0.5f;
            grenade.velocity.set(
                    player1.velocity.x + (float) Math.cos(player1.r) * 300,
                    player1.velocity.y + (float) Math.sin(player1.r) * 300
            );
            grenade.zSpeed = 16 + player1.zSpeed;
            grenades.add(grenade);
        }
    }

    //*****************************************************

    public Vector4 getPlayerPosition() {
        return new Vector4(player1.copyPosition(), player1.z, player1.r);
    }

    public Vector3 getPlayerXYZ() {
        return new Vector3(player1.position.x, player1.position.y, player1.z);
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

        sec = Physics.findCurrentSectorBranching(0, x, y);

        player1.currentSectorIndex = sec;
        return sec;
    }

    public Sprite[] getSpriteList() {
        int size = grenades.size + wallSpriteObjects.size;
        Sprite[] sprites = new Sprite[size];

        int i=0;
        for (Grenade g : grenades) {
            sprites[i] = g.getInfo();
            i++;
        }
        for (WallSpriteObject w : wallSpriteObjects) {
            sprites[i] = new WallSprite(
                app.materials.get(w.texInd).tex[0],
                w.x1, w.y1, w.z,
                w.x2, w.y2, w.height
            );
            i++;
        }

        return sprites;
    }

    // ****************************************************

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
            obj.setCurrentSector(
                //startIndex argument is zero so we set to something valid if not in a sector
                Physics.findCurrentSectorBranching(
                    0,
                    obj.snagPosition().x,
                    obj.snagPosition().y
                )
            );
        }

        PhysicsProperties props = obj.getProps();
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

            //Get reference to the closest collision
            WallInfoPack closestCollision = wallsCollided.get(0);

            Physics.resolveCollision(closestCollision, obj);
            velocity.set(Physics.bounceVector(velocity, closestCollision.w, props));
            if (velocity.isZero(1)) velocity.set(Vector2.Zero);

            SoundManager.playStaticPosition(
                SoundManager.SFX_Clink,
                new Vector3(objPos, obj.getZ()),
                300
            );

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

        //Enact friction
        if (props.zFriction != 0.f && obj.getZ() == teeterHeight) {
            velocity.scl(1.f - props.zFriction*dt);
        }

    }

    public boolean heightCheck(Sector s, Positionable obj, float stepUpAllowance) {
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
                if (info.distToNearest < obj.getRadius() - 0.01f) {
                    collisions.add(info);
                }
            }
        }
        return collisions;
    }

    // // ****************************************************

    public void mapLoad() {
        grenades.clear();
        wallSpriteObjects.clear();
    }

}