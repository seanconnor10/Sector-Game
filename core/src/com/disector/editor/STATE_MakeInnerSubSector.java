package com.disector.editor;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.utils.IntArray;

import com.disector.Sector;
import com.disector.Wall;
import com.disector.editor.actions.EditAction;

public class STATE_MakeInnerSubSector extends EditorState {

    public STATE_MakeInnerSubSector(Editor1 editor, Panel panel) {
        super(editor, panel);
        visibleName = "Making Walls Sub-sector";
    }

    @Override
    void step() {
        editor.selection.setHighlights(xUnSnapped(), yUnSnapped());
        if (editor.input.isJustPressed(Input.Keys.N))
            shouldFinish = true;
    }

    @Override
    void click() {
        IntArray wallsToChange = new IntArray();

        Wall highlightedWall = editor.selection.getWallHighlight();
        int highlightedWallIndex = editor.selection.getWallHighlightIndex();

        if (highlightedWall == null || highlightedWall.isPortal)
            return;

        Sector sector;
        int sectorIndex = getSectorIndexOfWall(highlightedWallIndex);
        if (sectorIndex == -1)
            return;
        sector = editor.sectors.get(sectorIndex);

        /*Wall[] sectorsWalls = new Wall[sector.walls.size];
        for (int i = 0; i < sectorsWalls.length; i++) {
            sectorsWalls[i] = editor.walls.get(sector.walls.get(i));
        }*/

        wallsToChange.add(highlightedWallIndex);

        int checks = 0;
        while(getNextWall(wallsToChange, sector)) {
            checks++;
            if (checks > sector.walls.size)
                break;
        }

        if (chainComplete(wallsToChange)) {
            Sector newSector = new Sector(sector, false);
            int newSectorIndex = editor.sectors.size;
            editor.sectors.add(newSector);

            for (int wInd : wallsToChange.toArray()) {
                Wall w = editor.walls.get(wInd);
                newSector.addWallSafely(wInd);
                w.isPortal = true;
                w.linkA = newSectorIndex;
                w.linkB = sectorIndex;
                w.matLower = w.mat;
                w.matUpper = w.mat;
            }

            editor.onMapLoad();
            editor.shouldUpdateViewRenderer = true;
        }



    }

    @Override
    void rightClick() {

    }

    @Override
    EditAction[] finish() {
        return new EditAction[0];
    }

    private boolean getNextWall(IntArray wallChain, Sector sector) {
        if (wallChain == null || wallChain.isEmpty())
            return false;

        Wall lastWall;
        try {
            lastWall = editor.walls.get(wallChain.get(wallChain.size - 1));
        } catch (NullPointerException | ArrayIndexOutOfBoundsException e) {
            return false;
        }

        float x = lastWall.x2, y = lastWall.y2;

        Wall next = null;
        for (int wInd : sector.walls()) {
            Wall w = editor.walls.get(wInd);
            if (w.isPortal) continue;
            if (wallChain.contains(wInd)) continue;

            if (w.x1 == x && w.y1 == y) {
                wallChain.add(wInd);
                return true;
            }
        }

        return false;
    }

    private int getSectorIndexOfWall(int wallIndex) {
        if (wallIndex < 0 || wallIndex >= editor.walls.size)
            return -1;

        for (int i = 0; i < editor.sectors.size; i++) {
            if (editor.sectors.get(i).walls.contains(wallIndex))
                return i;
        }

        return -1;
    }

    private boolean chainComplete(IntArray wallChain) {
        Wall first, last;

        try {
            first = editor.walls.get(wallChain.get(0));
            last = editor.walls.get(wallChain.get(wallChain.size - 1));
        } catch (ArrayIndexOutOfBoundsException e) {
            return false;
        }

        return
            first.x1 == last.x2 && first.y1 == last.y2;
    }
}
