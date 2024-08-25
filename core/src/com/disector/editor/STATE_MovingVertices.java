package com.disector.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.disector.Wall;
import com.disector.editor.actions.EditAction;

import java.util.ArrayList;

public class STATE_MovingVertices extends EditorState {
    boolean grabbing;
    ArrayList<Wall> grabbedPointOnes = new ArrayList<>();
    ArrayList<Wall> grabbedPointTwos = new ArrayList<>();

    public STATE_MovingVertices(Editor editor, Panel panel) {
        super(editor, panel);
        visibleName = "Moving Vertices";
        ignoreEditorClick = false;
    }

    @Override
    void step() {
        if (grabbing) {
            editor.shouldUpdateViewRenderer = true;
            if (!Gdx.input.isButtonPressed(Input.Buttons.LEFT))
                letGo();
            else
               moveGrabbedWall();
        }
        if (editor.input.isJustPressed(Input.Keys.M))
            shouldFinish = true;
    }

    @Override
    void click() {
        letGo();

        float x = xUnSnapped(), y = yUnSnapped();
        final float grabDistance = 5.0f / editor.mapRenderer.zoom;

        for (Wall w : editor.walls) {
            if (squareCollision(w.x1, w.y1, x, y, grabDistance))
                grabbedPointOnes.add(w);
            else if (squareCollision(w.x2, w.y2, x, y, grabDistance))
                grabbedPointTwos.add(w);
        }
        grabbing = (!grabbedPointOnes.isEmpty() || !grabbedPointTwos.isEmpty());
    }

    @Override
    void rightClick() {

    }

    @Override
    EditAction[] finish() {
        return new EditAction[0];
    }

    private void letGo() {
        grabbedPointOnes.clear();
        grabbedPointTwos.clear();
        grabbing = false;
    }

    private void moveGrabbedWall() {
        int x = x(), y = y();

        for (Wall w : grabbedPointOnes) {
            w.x1 = x;
            w.y1 = y;
            w.setNormalAngle();
        }
        for (Wall w : grabbedPointTwos) {
            w.x2 = x;
            w.y2 = y;
            w.setNormalAngle();
        }
    }

}
