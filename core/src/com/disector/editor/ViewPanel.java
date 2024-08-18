package com.disector.editor;

import com.disector.inputrecorder.InputChainNode;

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
    void clickedIn() {
        isForcingMouseFocus = true;
    }
}
