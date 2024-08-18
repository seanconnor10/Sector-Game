package com.disector.editor;

import com.disector.editor.actions.EditAction;

class STATE_PlacingCamera extends EditorState{
    float cameraStartX, cameraStartY, cameraStartR;
    int cameraX, cameraY;

    public STATE_PlacingCamera(Editor editor, Panel panel) {
        super(editor, panel);
        ignoreEditorClick = true;
        visibleName = "Placing Render Camera";
        init();
    }

    void init() {
        cameraX = x();
        cameraY = y();
        cameraStartX = editor.viewRenderer.camX;
        cameraStartY = editor.viewRenderer.camY;
        cameraStartR = editor.viewRenderer.camR;
        editor.placeViewCamera(cameraX, cameraY);
    }

    @Override
    void step() {
        if (shouldFinish) return;
        int x = xUnSnapped();
        int y = yUnSnapped();
        editor.viewRenderer.camR = (float) Math.atan((double)(y-cameraY)/(double)(x-cameraX) );
        if (x<cameraX) editor.viewRenderer.camR += (float) Math.PI;
        editor.shouldUpdateViewRenderer = true;
    }

    @Override
    void click() {
        shouldFinish = true;
    }

    @Override
    void rightClick() {
        shouldFinish = true;
        editor.placeViewCamera(cameraStartX, cameraStartY);
        editor.viewRenderer.camR = cameraStartR;
        editor.shouldUpdateViewRenderer = true;
    }

    @Override
    EditAction[] finish() {
        editor.state = null;
        return new EditAction[0];
    }

}
