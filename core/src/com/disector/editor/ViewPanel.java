package com.disector.editor;

class ViewPanel extends Panel{
    public ViewPanel(Editor editor) {
        super(editor, "RenderViewPanel");
    }

    @Override
    void clickedIn() {
        isForcingMouseFocus = true;
    }
}
