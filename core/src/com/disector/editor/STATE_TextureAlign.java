package com.disector.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

import com.disector.Wall;
import com.disector.editor.actions.EditAction;
import com.disector.inputrecorder.InputChainNode;

public class STATE_TextureAlign extends EditorState{

    com.disector.inputrecorder.InputChainNode input;

    Wall wall;

    boolean scalingNotShifting;

    float origXScale, origYScale, origXOffset, origYOffset;

    public STATE_TextureAlign(Editor editor, Panel panel, int wallIndex) {
        super(editor, panel);
        this.wall = editor.walls.get(wallIndex);
        this.visibleName = "Texture Align Wall " + wallIndex;

        this.origXOffset = wall.xOffset;
        this.origYOffset = wall.yOffset;
        this.origXScale = wall.xScale;
        this.origYScale = wall.yScale;

        input = new InputChainNode(editor.input, "Editor-State-Texture-Align") {

            @Override
            public boolean mouseMoved(int screenX, int screenY) {
                if (shouldFinish)
                    return false;

                if (scalingNotShifting) {
                    wall.xScale += 0.01f * Gdx.input.getDeltaX();
                    wall.yScale += 0.01f * Gdx.input.getDeltaY();
                } else {
                    wall.xOffset += 0.02f * Gdx.input.getDeltaX();
                    wall.yOffset += 0.02f * Gdx.input.getDeltaY();
                }

                editor.shouldUpdateViewRenderer = true;

                Gdx.input.setCursorPosition( (int) (panel.rect.x + panel.rect.width / 2), Gdx.graphics.getHeight() - (int) (panel.rect.y + panel.rect.height / 2) );

                return true;
            }

            @Override
            public boolean scrolled(float amountX, float amountY) {

                return true;
            }

        };

        input.on();

    }

    @Override
    void step() {

        if (input.isJustPressed(Input.Keys.SHIFT_LEFT)) {
            scalingNotShifting = !scalingNotShifting;
        }

        if (input.isJustPressed(Input.Keys.G)) {
            shouldFinish = true;
        }
    }

    @Override
    void click() {

    }

    @Override
    void rightClick() {
        wall.xOffset = origXOffset;
        wall.yOffset = origYOffset;
        wall.xScale = origXScale;
        wall.yScale = origYScale;
        shouldFinish = true;
    }

    @Override
    EditAction[] finish() {
        editor.input.remove(this.input);
        return new EditAction[0];
    }
}
