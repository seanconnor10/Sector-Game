package com.disector.editor;

import com.badlogic.gdx.math.Rectangle;

import java.util.function.Function;

class Button {
    String text = "";

    final Panel panel;
    final Rectangle panelRect;
    final Rectangle rect;

    Function<Void, Void> releaseAction = (Void) -> {text = "WHOOPS! NO ACTION..."; return Void;};

    boolean pressed;
    boolean active = true;

    Button(Editor1 editor, Panel panel, String text) {
        this.panel = panel;
        this.rect = new Rectangle();
        this.text = text;
        panelRect = panel.rect;
    }

}