package com.disector.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.disector.gameworld.Player;
import com.disector.inputrecorder.InputChainNode;
import com.disector.inputrecorder.InputRecorder;

class ViewPanel extends Panel{
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
    }
}
