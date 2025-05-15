package com.disector.editor;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import com.disector.Wall;
import com.disector.editor.actions.EditAction;

abstract class EditorState {
    String visibleName = "MISSING STATE NAME";

    final Editor1 editor;
    final Panel panel;

    boolean ignoreEditorClick;
    boolean shouldFinish;

    EditorState(Editor1 editor, Panel panel) {
        this.editor = editor;
        this.panel = panel;
    }

    abstract void step();

    abstract void click();

    abstract void rightClick();

    abstract EditAction[] finish(); //Maybe return the EditAction for the undo stack here?

    int x(){return editor.isGridSnapping ? xSnapped() : xUnSnapped();}

    int y(){return editor.isGridSnapping ? ySnapped() : yUnSnapped();}

    int xUnSnapped() {return editor.mapPanel.getMouseWorldX();}

    int yUnSnapped() {return editor.mapPanel.getMouseWorldY();}

    int xSnapped() {return editor.snap(xUnSnapped());}

    int ySnapped() {return editor.snap(yUnSnapped());}

    protected Array<Wall> getPointsOnesAtMouse() {
        Array<Wall> walls = new Array<>();
        float x = xUnSnapped();
        float y = yUnSnapped();

        final float grabDistance = 5.0f / editor.mapRenderer.zoom;

        for (Wall w : editor.walls) {
            if (squareCollision(w.x1, w.y1, x, y, grabDistance))
                walls.add(w);
        }

        return walls;
    }

    protected int[] getPointsOnesAtMouseByIndex(float worldDistance) {
        IntArray walls = new IntArray();

        float x = xUnSnapped();
        float y = yUnSnapped();

        final float grabDistance = worldDistance / editor.mapRenderer.zoom;

        for (int i=0; i<editor.walls.size; i++) {
            if (squareCollision(editor.walls.get(i).x1, editor.walls.get(i).y1, x, y, grabDistance))
                walls.add(i);
        }

        return walls.toArray();
    }

    protected int[] getPointsTwosAtMouseByIndex(float worldDistance) {
        IntArray walls = new IntArray();

        float x = xUnSnapped();
        float y = yUnSnapped();

        final float grabDistance = worldDistance / editor.mapRenderer.zoom;

        for (int i=0; i<editor.walls.size; i++) {
            if (squareCollision(editor.walls.get(i).x2, editor.walls.get(i).y2, x, y, grabDistance))
                walls.add(i);
        }

        return walls.toArray();
    }

    protected boolean squareCollision(float x1, float y1, float x2, float y2, float maxDistance) {
        if ( Math.abs(x1-x2) > maxDistance)
            return false;
        return Math.abs(y1-y2) < maxDistance;
    }

    private boolean mouseOutsidePanel() {
    int x = xUnSnapped();
    int y = yUnSnapped();

    return (
        x < 0 ||
        x >= panel.rect.width ||
        y < 0 ||
        y >= panel.rect.height
    );
    }

}
