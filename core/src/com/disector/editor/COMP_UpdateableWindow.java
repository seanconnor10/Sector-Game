package com.disector.editor;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.utils.Array;

public class COMP_UpdateableWindow extends Window implements UpdatableComponent {
    protected final Editor1 editor;

    protected final Array<UpdatableComponent> actorsToUpdate = new Array<>();

    public COMP_UpdateableWindow(String title, Skin skin, Editor1 editor) {
        super(title, skin);
        this.editor = editor;
    }

    public COMP_UpdateableWindow(String title, Skin skin, String styleName, Editor1 editor) {
        super(title, skin, styleName);
        this.editor = editor;
    }

    public COMP_UpdateableWindow(String title, WindowStyle style, Editor1 editor) {
        super(title, style);
        this.editor = editor;
    }

    @Override
    public void onMapLoad() {
        //Probably you want to override this method,
        //Caused infinite recursion once
        updateControlGroups();
    }

    protected void updateControlGroups() {
        for (UpdatableComponent item : actorsToUpdate) {
            item.onMapLoad();
        }
    }
}
