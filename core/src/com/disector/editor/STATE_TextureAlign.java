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

    boolean isConstrainingAxis, constrainVertically;

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

                boolean shift = input.isDown(Input.Keys.SHIFT_LEFT);

                int deltaX = Gdx.input.getDeltaX();
                int deltaY = Gdx.input.getDeltaY();

                if (!isConstrainingAxis && shift) {
                    if (deltaX > deltaY ) {
                        constrainVertically = false;
                        isConstrainingAxis = true;
                    } else if (deltaY > deltaX) {
                        constrainVertically = true;
                        isConstrainingAxis = true;
                    }
                } else if (isConstrainingAxis && !shift) {
                    isConstrainingAxis = false;
                }

                boolean allowHorizontal = !isConstrainingAxis || !constrainVertically;
                boolean allowVertical = !isConstrainingAxis || constrainVertically;

                if (scalingNotShifting) {
                    if (allowHorizontal)    wall.xScale += 0.01f * deltaX;
                    if (allowVertical)      wall.yScale += 0.01f * deltaY;
                } else {
                    if (allowHorizontal)    wall.xOffset += 0.02f * deltaX;
                    if (allowVertical)      wall.yOffset += 0.02f * deltaY;
                }

                editor.shouldUpdateViewRenderer = true;

                Gdx.input.setCursorPosition(
                        (int) ( panel.rect.x + panel.rect.width / 2 ),
                        Gdx.graphics.getHeight() - (int) ( panel.rect.y + panel.rect.height / 2 )
                );

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

        if (input.isJustPressed(Input.Keys.F)) {
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
        this.input.off();
        editor.input.remove(this.input);
        return new EditAction[0];
    }
}
