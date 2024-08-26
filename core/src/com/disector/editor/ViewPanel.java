package com.disector.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.disector.gameworld.Player;
import com.disector.inputrecorder.InputChainNode;
import com.disector.inputrecorder.InputRecorder;
import com.disector.renderer.EditingSoftwareRenderer;

class ViewPanel extends Panel{
    ActiveSelection.Surface recentViewClick = null;

    public ViewPanel(Editor editor) {
        super(editor);

        this.input = new InputChainNode(editor.input, "View-Panel-Input") {
            @Override
            public boolean scrolled(float amountX, float amountY) {
                return super.scrolled(amountX, amountY);
            }
        };
    }

    @Override
    void step(float dt) {
        if (editor.viewRenderer.highLightStrength > 0) {
            editor.viewRenderer.highLightStrength -= dt;
            editor.shouldUpdateViewRenderer = true;
            if (editor.viewRenderer.highLightStrength < 0)
                editor.viewRenderer.highLightStrength = 0;
        }

    }

    @Override
    void stepFocused(float dt) {
        super.stepFocused(dt);
        if (Gdx.input.isButtonPressed(Input.Buttons.MIDDLE)) {
            editor.shouldUpdateViewRenderer = true;
            editor.viewRenderer.camR -= InputRecorder.mouseDeltaX / 250;
            editor.viewRenderer.camVLook -= InputRecorder.mouseDeltaY / 2;
            Gdx.input.setCursorPosition( (int) (rect.x + rect.width / 2), Gdx.graphics.getHeight() - (int) (rect.y + rect.height / 2) );
        }

        if (input.isJustPressed(Input.Keys.P)) {
            Player p1 = editor.app.gameWorld.player1;
            p1.position.set(editor.viewRenderer.camX, editor.viewRenderer.camY);
            p1.setCurrentSector(editor.viewRenderer.camCurrentSector);
            p1.setZ(editor.viewRenderer.camZ);
            p1.r = editor.viewRenderer.camR;
            p1.vLook = editor.viewRenderer.camVLook;
        }

        if (editor.state == null && input.isJustPressed(Input.Keys.F)) {
            int index = editor.viewRenderer.wallHighLightIndex;
            if (index < 0 || index >= editor.walls.size) return;
            editor.state = new STATE_TextureAlign(editor, this, recentViewClick);
        }
    }

    @Override
    void clickedIn() {
        int frameX = (int) ( relX() / (rect.width/editor.viewRenderer.getWidth()) );
        int frameY = (int) ( relY() / (rect.height/editor.viewRenderer.getHeight()) );

        EditingSoftwareRenderer.ClickInfo info =
                editor.viewRenderer.getClickInfo(frameX, frameY);

        editor.viewRenderer.wallHighLightIndex = -1;
        editor.viewRenderer.sectorHighlightIndex = -1;
        editor.shouldUpdateViewRenderer = true;

        recentViewClick = new ActiveSelection.Surface(info.index, info.type);

        switch (info.type) {
        case WALL_MAIN:
        case WALL_UPPER:
        case WALL_LOWER:
            editor.viewRenderer.highLightStrength = 0.75f;
            editor.viewRenderer.wallHighLightIndex = info.index;
            editor.propertiesPanel.wallPropertiesWindow.setWall(info.index);
            break;
        case FLOOR:
        case CEIL:
            editor.viewRenderer.highLightStrength = 0.75f;
            editor.viewRenderer.sectorHighlightIndex = info.index;
            editor.propertiesPanel.sectorPropertiesWindow.setSector(info.index);
            break;
        default:
            break;
        }

    }
}
