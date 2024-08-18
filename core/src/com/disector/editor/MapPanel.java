package com.disector.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

class MapPanel extends Panel {
    public MapPanel(Editor editor) {
        super(editor, "MapPanel");
    }

    @Override
    void step(float dt) {
        super.step(dt);

        if (editor.state == null) {
            editor.selection.setHighlights(getMouseWorldX(), getMouseWorldY());
        }

        keyActions();

        editor.selection.setHighlights(getMouseWorldX(), getMouseWorldY());
    }

    @Override
    void clickedIn() {
        super.clickedIn();

        if (editor.state == null) {
            editor.state = new STATE_PlacingCamera(editor, this);
        }
    }

    int getMouseWorldX() {
        float mouseX = relX();
        int mapRendererCenterX = Math.round(editor.mapRenderer.camX);
        return (int) ( mapRendererCenterX + (mouseX - (rect.width/2.0)) / editor.mapRenderer.zoom );
    }

    int getMouseWorldY() {
        float mouseY = relY();
        int mapRendererCenterY = Math.round(editor.mapRenderer.camY);
        return (int) ( mapRendererCenterY + (mouseY - (rect.height/2.0)) / editor.mapRenderer.zoom );
    }

    private void keyActions() {
        if (editor.state != null) {
            return;
        }
        if (editor.input.isJustPressed(Input.Keys.E)) {
            editor.state = new STATE_ExtrudingSector(editor, this);
            return;
        }
        if (!editor.input.isJustPressed(Input.Keys.CONTROL_LEFT) && editor.input.isJustPressed(Input.Keys.I)) {
            editor.state = new STATE_SplittingWall(editor, this);
            return;
        }
        if (editor.input.isJustPressed(Input.Keys.SPACE)) {
            editor.state = new STATE_CreatingSector(editor, this);
            return;
        }
        if (editor.input.isJustPressed(Input.Keys.M)) {
            editor.state = new STATE_MovingVertices(editor, this);
            return;
        }
        if (editor.input.isJustPressed(Input.Keys.N)) {
            editor.state = new STATE_MakeInnerSubSector(editor, this);
            return;
        }

    }

}
