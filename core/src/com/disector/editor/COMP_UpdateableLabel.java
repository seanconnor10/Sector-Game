package com.disector.editor;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class COMP_UpdateableLabel extends Label implements UpdatableComponent {
    Runnable onUpdate;

    public COMP_UpdateableLabel(CharSequence text, Skin skin) {
        super(text, skin);
    }

    public COMP_UpdateableLabel(CharSequence text, Skin skin, String styleName) {
        super(text, skin, styleName);
    }

    public COMP_UpdateableLabel(CharSequence text, Skin skin, String fontName, Color color) {
        super(text, skin, fontName, color);
    }

    public COMP_UpdateableLabel(CharSequence text, Skin skin, String fontName, String colorName) {
        super(text, skin, fontName, colorName);
    }

    public COMP_UpdateableLabel(CharSequence text, LabelStyle style) {
        super(text, style);
    }

    @Override
    public void onMapLoad() {
        if (onUpdate != null) {
            try {onUpdate.run();} catch (Exception e) {System.out.println(e.toString() + '\n');};
        }
    }
}
