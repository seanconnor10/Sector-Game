package com.disector.editor;

import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.Array;

import com.disector.Sector;
import com.disector.Wall;

import com.disector.editor.actions.EditAction;

class STATE_ExtrudingSector extends EditorState {

    int previousSectorIndex;
    Sector previousSector;
    int newSectorIndex;
    Sector newSector;
    final int initialWallIndex;
    Wall initialWall;
    Wall initialWallCopy;
    Array<Wall> createdWalls = new Array<>();
    IntArray createdWallIndices = new IntArray();

    public STATE_ExtrudingSector(Editor1 editor, Panel panel) {
        super(editor, panel);
        visibleName = "Extruding Wall as Portal";
        ignoreEditorClick = true;
        initialWallIndex = editor.selection.getWallHighlightIndex();
        if (initialWallIndex == -1) {
            shouldFinish = true;
            return;
        }
        initialWall = editor.selection.getWallHighlight();
        initialWallCopy = new Wall(initialWall);
        if (initialWall.isPortal) {
            shouldFinish = true;
            return;
        }
        init();
    }

    void init() {
        setPreviousSector();
        newSectorIndex = editor.sectors.size;
        newSector = new Sector(previousSector, false);
        newSector.walls.add(initialWallIndex);
        editor.sectors.add(newSector);

        initialWall.isPortal = true;
        initialWall.linkA = previousSectorIndex;
        initialWall.linkB = newSectorIndex;
        makeNextWall();
        editor.messageLog.log("Extruding New Sector from Wall " + initialWallIndex);
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
        if ( Math.abs(x() - initialWall.x2) < 0.05f && Math.abs(y() - initialWall.y2) < 0.05f ) {
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
        Wall previous, newWall;
        if (createdWalls.size == 0) {
            previous = initialWall;
            newWall = new Wall(previous);
            newWall.x2 = x;
            newWall.y2 = y;
        } else {
            previous = createdWalls.get(createdWalls.size-1);
            newWall = new Wall(previous);
            newWall.x1 = previous.x2;
            newWall.y1 = previous.y2;
            newWall.x2 = x;
            newWall.y2 = y;
        }
        newWall.isPortal = false;
        newWall.linkA = 0;
        newWall.linkB = 0;

        int newIndex = editor.app.walls.size;
        createdWallIndices.add(newIndex);
        createdWalls.add(newWall);
        newSector.walls.add(newIndex);
        editor.app.walls.add(newWall);
    }

    private void deleteWall() {
        int i = createdWallIndices.size-1;
        int wallIndex = createdWallIndices.get(i);
        createdWallIndices.removeIndex(i);
        createdWalls.removeIndex(i);
        newSector.walls.removeValue(wallIndex);
        editor.app.walls.removeIndex(wallIndex);

        if (createdWalls.isEmpty()) {
            shouldFinish = true;
            initialWall.setFromCopy(initialWallCopy);
        }
    }

    private void setPreviousSector() {
        for (int i=0; i<editor.sectors.size; i++) {
            Sector sector = editor.sectors.get(i);
            if (sector.walls.contains(initialWallIndex)) {
                previousSector = sector;
                previousSectorIndex = i;
                break;
            }
        }
    }

}
