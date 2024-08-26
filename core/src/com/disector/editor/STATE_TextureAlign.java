package com.disector.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

import com.disector.Wall;
import com.disector.editor.actions.EditAction;
import com.disector.inputrecorder.InputChainNode;
import com.disector.renderer.EditingSoftwareRenderer.CLICK_TYPE;
import com.disector.editor.ActiveSelection.Surface;

class STATE_TextureAlign extends EditorState{

    com.disector.inputrecorder.InputChainNode input;

    Wall wall;

    boolean scalingNotShifting;

    boolean isConstrainingAxis, constrainVertically;

    float origXScale, origYScale, origXOffset, origYOffset;
    
    Surface target;

    STATE_TextureAlign(Editor editor, Panel panel, Surface target) {
        super(editor, panel);
        if (target == null || target.type == CLICK_TYPE.FLOOR || target.type == CLICK_TYPE.CEIL) {
            shouldFinish = true;
            return;
        }
        this.wall = editor.walls.get(target.index);
        this.visibleName = "Texture Align Wall " + target.index;

        switch(target.type) {
            case WALL_MAIN:
                this.origXOffset = wall.xOffset;
                this.origYOffset = wall.yOffset;
                this.origXScale = wall.xScale;
                this.origYScale = wall.yScale;
                break;
            case WALL_LOWER:
                this.origXOffset = wall.Lower_xOffset;
                this.origYOffset = wall.Lower_yOffset;
                this.origXScale = wall.Lower_xScale;
                this.origYScale = wall.Lower_yScale;
                break;
            case WALL_UPPER:
                this.origXOffset = wall.Upper_xOffset;
                this.origYOffset = wall.Upper_yOffset;
                this.origXScale = wall.Upper_xScale;
                this.origYScale = wall.Upper_yScale;
                break;
            default:
                break;
        }

        this.target = target;

        input = new InputChainNode(editor.input, "Editor-State-Texture-Align") {

            @Override
            public boolean mouseMoved(int screenX, int screenY) {
                if (shouldFinish)
                    return false;

                boolean shift = input.isDown(Input.Keys.SHIFT_LEFT);

                int deltaX = Gdx.input.getDeltaX();
                int deltaY = Gdx.input.getDeltaY();

                if (deltaX == 0 && deltaY == 0)
                    return false;

                int xABS = Math.abs(deltaX);
                int yABS = Math.abs(deltaY);

                if (!isConstrainingAxis && shift) {
                    if (xABS > 0 && xABS > yABS) {
                        constrainVertically = false;
                        isConstrainingAxis = true;
                    } else if (yABS > 0 && yABS > xABS ) {
                        constrainVertically = true;
                        isConstrainingAxis = true;
                    }

                } else if (isConstrainingAxis && !shift) {
                    isConstrainingAxis = false;
                }

                boolean allowHorizontal = !isConstrainingAxis || !constrainVertically;
                boolean allowVertical = !isConstrainingAxis || constrainVertically;

                if (scalingNotShifting) {
                    switch (target.type) {
                    case WALL_MAIN:
                        if (allowHorizontal) wall.xScale += 0.01f * deltaX;
                        if (allowVertical) wall.yScale += 0.01f * deltaY;
                        break;
                    case WALL_LOWER:
                        if (allowHorizontal) wall.Lower_xScale += 0.01f * deltaX;
                        if (allowVertical) wall.Lower_yScale += 0.01f * deltaY;
                        break;
                    case WALL_UPPER:
                        if (allowHorizontal) wall.Upper_xScale += 0.01f * deltaX;
                        if (allowVertical) wall.Upper_yScale += 0.01f * deltaY;
                        break;
                    default:
                        break;
                    }
                } else {
                    switch (target.type) {
                        case WALL_MAIN:
                            if (allowHorizontal) wall.xScale += 0.02f * deltaX;
                            if (allowVertical) wall.yOffset += 0.02f * deltaY;
                            break;
                        case WALL_LOWER:
                            if (allowHorizontal) wall.Lower_xOffset += 0.02f * deltaX;
                            if (allowVertical) wall.Lower_yOffset += 0.02f * deltaY;
                            break;
                        case WALL_UPPER:
                            if (allowHorizontal) wall.Upper_xOffset += 0.02f * deltaX;
                            if (allowVertical) wall.Upper_yOffset += 0.02f * deltaY;
                            break;
                        default:
                            break;
                    }
                }

                editor.onMapLoad();

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
        editor.shouldUpdateViewRenderer = true;
        editor.onMapLoad();

        switch(target.type) {
            case WALL_MAIN:
                wall.xOffset = origXOffset;
                wall.yOffset = origYOffset;
                wall.xScale = origXScale;
                wall.yScale = origYScale;
                break;
            case WALL_LOWER:
                wall.Lower_xOffset = origXOffset;
                wall.Lower_yOffset = origYOffset;
                wall.Lower_xScale = origXScale;
                wall.Lower_yScale = origYScale;
                break;
            case WALL_UPPER:
                wall.Upper_xOffset = origXOffset;
                wall.Upper_yOffset = origYOffset;
                wall.Upper_xScale = origXScale;
                wall.Upper_yScale = origYScale;
                break;
            default:
                break;
        }

        shouldFinish = true;
    }

    @Override
    EditAction[] finish() {
        this.input.off();
        editor.input.remove(this.input);
        return new EditAction[0];
    }
}
