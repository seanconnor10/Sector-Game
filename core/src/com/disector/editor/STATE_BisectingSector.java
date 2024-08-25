package com.disector.editor;

import com.badlogic.gdx.utils.Array;

import com.disector.Physics;
import com.disector.Sector;
import com.disector.Wall;
import com.disector.editor.actions.EditAction;

import java.util.Arrays;

class STATE_BisectingSector extends EditorState {
    boolean started;

    Sector sector;
    int[] sec_wallIndices;
    Array<Wall> sec_walls;

    STATE_BisectingSector(Editor editor, Panel panel) {
        super(editor, panel);
        ignoreEditorClick = true;
        visibleName = "Sector Bisection";
    }

    @Override
    void step() {
        if (!started) return;
    }

    @Override
    void click() {
        Wall wall;
        int wInd;

        wInd = editor.selection.highlightedWallIndex;
        wall = editor.selection.highlightedWall;

        if (wInd == -1 || wall == null) return;

        //Find Sector we're splitting

        int secInd = -1;

        if (wall.isPortal) {
            if (Physics.containsPoint(wall.linkA, x(), y() ))
                secInd = wall.linkA;
            else if (Physics.containsPoint(wall.linkB, x(), y() ))
                secInd = wall.linkB;
            else
                return;
        } else {
            for (int i = 0; i < editor.sectors.size; i++) {
                if (editor.sectors.get(i).walls.contains(wInd)) {
                    secInd = i;
                    break;
                }
            }
        }

        if (secInd == -1) return;

        sector = editor.sectors.get(secInd);

        sec_wallIndices = Arrays.copyOf(sector.walls(), sector.walls.size);
        sec_walls = new Array<>(sec_wallIndices.length);
        started = true;



    }

    @Override
    void rightClick() {

    }

    @Override
    EditAction[] finish() {
        return new EditAction[0];
    }
}
