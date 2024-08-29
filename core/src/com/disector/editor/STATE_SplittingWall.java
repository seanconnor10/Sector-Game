package com.disector.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import com.disector.Sector;
import com.disector.Wall;
import com.disector.WallInfoPack;
import com.disector.editor.actions.EditAction;

public class STATE_SplittingWall extends EditorState {
    public STATE_SplittingWall(Editor editor, Panel panel) {
        super(editor, panel);
        visibleName = "Splitting Walls";
        editor.messageLog.log("  Left Click to Perform Split");
        editor.messageLog.log("Splitting Walls");
    }

    @Override
    void step() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.I)) {
            shouldFinish = true;
        }
    }

    @Override
    void click() {
        if (shouldFinish)
            return;

        if (editor.selection.highlightedWall == null || editor.selection.highlightedWallIndex == -1)
            return;

        WallInfoPack highlightedWall = new WallInfoPack(
                editor.selection.getWallHighlight(),
                editor.selection.getWallHighlightIndex(),
                new Vector2( round(x()), round(y()) )
        );

        if (highlightedWall.distToNearest < editor.gridSize) {
            addSplitHalfToMapData(highlightedWall);
        }
    }

    @Override
    void rightClick() {

    }

    @Override
    EditAction[] finish() {
        editor.messageLog.log("Done Splitting");
        return new EditAction[0];
    }

    private void addSplitHalfToMapData(WallInfoPack wall) {
        int newWallIndex = editor.walls.size;
        Wall newSplitHalf = new Wall(wall.w);
        editor.walls.add(newSplitHalf);

        wall.w.x2 = wall.nearestPoint.x;
        wall.w.y2 = wall.nearestPoint.y;
        newSplitHalf.x1 = wall.nearestPoint.x;
        newSplitHalf.y1 = wall.nearestPoint.y;

        if (wall.w.isPortal) {
            editor.sectors.get(wall.w.linkA).addWallSafely(newWallIndex);
            editor.sectors.get(wall.w.linkB).addWallSafely(newWallIndex);
        } else {
            Sector sector = findSectorHavingWallIndx(wall.wInd);
            if (sector != null)
                sector.addWallSafely(newWallIndex);
        }
    }

    private Sector findSectorHavingWallIndx(int wallIndex) {
        for (Sector sector : editor.sectors) {
            if ( sector.walls.contains(wallIndex) )
                return sector;
        }
        return null;
    }

    private float round(float val) {
        return Math.round(val*100f)/100f;
    }
}
