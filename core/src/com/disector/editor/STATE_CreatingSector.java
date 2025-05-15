package com.disector.editor;

import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.Array;

import com.disector.Physics;
import com.disector.Sector;
import com.disector.Wall;

import com.disector.editor.actions.EditAction;

class STATE_CreatingSector extends EditorState {

    boolean isNewSector = false;
    int sectorIndex;
    Sector sector;
    Array<Wall> createdWalls = new Array<>();
    IntArray createdWallIndices = new IntArray();
    float firstWallX, firstWallY;

    public STATE_CreatingSector(Editor1 editor, Panel panel) {
        super(editor, panel);
        ignoreEditorClick = true;
        visibleName = "Creating Walls";
        init();
    }

    void init() {
        setSector();
        makeNextWall();
        firstWallX = createdWalls.get(0).x1;
        firstWallY = createdWalls.get(0).y1;
        if (isNewSector) {
            editor.messageLog.log("Creating New Detached Sector");
        } else {
            editor.messageLog.log("Creating Walls in Sector " + sectorIndex);
        }
    }

    @Override
    void step() {
        if (shouldFinish) return;

        Wall newestWall = createdWalls.get(createdWalls.size-1);
        int x = x(), y = y();

        if (newestWall.x2 != x || newestWall.y2 != y) {
            newestWall.x2 = x();
            newestWall.y2 = y();
            editor.shouldUpdateViewRenderer = true;
        }
        newestWall.setNormalAngle();
    }

    @Override
    void click() {
        if (shouldFinish) return;

        Wall newestWall = createdWalls.get(createdWalls.size-1);

        if (newestWall.x2 == newestWall.x1 && newestWall.y2 == newestWall.y1)
            return;

        //If Clicking On Beginning of first wall, finish
        if ( Math.abs(x() - firstWallX) < 0.05f && Math.abs(y() - firstWallY) < 0.05f ) {
            shouldFinish = true;
            return;
        }

        makeNextWall();
    }

    @Override
    void rightClick() {
        if (shouldFinish) return;
        deleteWall();
    }

    @Override
    EditAction[] finish() {
        return new EditAction[0];
    }

    private void makeNextWall() {
        int x = x(), y = y();
        Wall newWall = new Wall( x, y, x, y);
        int newIndex = editor.app.walls.size;
        createdWallIndices.add(newIndex);
        createdWalls.add(newWall);
        sector.walls.add(newIndex);
        editor.app.walls.add(newWall);
    }

    private void deleteWall() {
        int i = createdWallIndices.size-1;
        int wallIndex = createdWallIndices.get(i);
        createdWallIndices.removeIndex(i);
        createdWalls.removeIndex(i);
        sector.walls.removeValue(wallIndex);
        editor.app.walls.removeIndex(wallIndex);

        if (createdWalls.isEmpty())
            shouldFinish = true;
    }

    private void setSector() {
        sectorIndex = Physics.findCurrentSectorBranching(-1, x(), y());
        if (sectorIndex == -1) {
            isNewSector = true;
            sectorIndex = editor.sectors.size;
            editor.sectors.add(new Sector());
        }
        sector = editor.sectors.get(sectorIndex);

        /*sectorIndex = editor.selection.highlightedSectorIndex;
        if (sectorIndex == -1) {
            isNewSector = true;
            sectorIndex = editor.sectors.size;
            editor.sectors.add(new Sector());
        }
        sector = editor.sectors.get(sectorIndex);*/
    }
}
