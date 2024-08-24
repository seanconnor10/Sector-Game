package com.disector.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.disector.inputrecorder.InputChainNode;
import com.disector.inputrecorder.InputRecorder;

class MapPanel extends Panel {
    public MapPanel(Editor editor) {
        super(editor);
        this.input = new InputChainNode(editor.input, "Map-Panel-Input") {
            @Override
            public boolean scrolled(float amountX, float amountY) {
                float mapZoom = editor.mapRenderer.zoom;
                mapZoom -= amountY * 0.5f * ( mapZoom < 1 ? 0.1f : mapZoom < 4 ? 0.5f : 2 );
                if (mapZoom < 0.1f) mapZoom = 0.1f;
                if (mapZoom > 20) mapZoom = 20;
                editor.mapRenderer.zoom = mapZoom;
                return true;
            }
        };
    }

    @Override
    void stepFocused(float dt) {
        super.stepFocused(dt);

        keyActions();

        if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {
            editor.mapRenderer.camX -= InputRecorder.mouseDeltaX / editor.mapRenderer.zoom;
            editor.mapRenderer.camY += InputRecorder.mouseDeltaY / editor.mapRenderer.zoom;
        }

        editor.selection.setHighlights(getMouseWorldX(), getMouseWorldY());
    }

    @Override
    void step(float dt) {
        super.step(dt);
        keyActions();
    }

    @Override
    void clickedIn() {
        super.clickedIn();

        if (editor.state == null) {
            editor.selection.addHighlightedWallToSelection();
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

        boolean shift = input.isDown(Input.Keys.SHIFT_LEFT);

        if (input.isJustPressed(Input.Keys.E)) {
            editor.state = new STATE_ExtrudingSector(editor, this);
            return;
        }
        if (!input.isJustPressed(Input.Keys.CONTROL_LEFT) && editor.input.isJustPressed(Input.Keys.I)) {
            editor.state = new STATE_SplittingWall(editor, this);
            return;
        }
        if (input.isJustPressed(Input.Keys.SPACE)) {
            editor.state = new STATE_CreatingSector(editor, this);
            return;
        }
        if (input.isJustPressed(Input.Keys.M)) {
            editor.state = new STATE_MovingVertices(editor, this);
            return;
        }
        if (input.isJustPressed(Input.Keys.N)) {
            editor.state = new STATE_MakeInnerSubSector(editor, this);
            return;
        }
        if (input.isJustPressed(Input.Keys.P)) {
            editor.app.gameWorld.player1.snagPosition().set(
                    editor.mapPanel.getMouseWorldX(),
                    editor.mapPanel.getMouseWorldY()
            );
        }
        if (editor.state == null && input.isJustPressed(Input.Keys.C)) {
            editor.state = new STATE_PlacingCamera(editor, this);
        }

    }

}
