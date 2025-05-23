package com.disector.editor;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

public class COMP_ControlGroup extends Table implements UpdatableComponent {
    private static final int INPUT_WIDTH = 100;
    private static final int LABEL_WIDTH = 80;
    private static final int BUTTON_WIDTH = 20;

    Runnable minusAction;
    Runnable plusAction;
    Runnable onTextSubmit;
    Runnable onUpdateMap;

    Label label;
    com.badlogic.gdx.scenes.scene2d.ui.Button minusButton;
    com.badlogic.gdx.scenes.scene2d.ui.Button plusButton;
    TextField textField;

    public COMP_ControlGroup(String name, Skin skin, Editor1 editor) {
        super();

        final com.badlogic.gdx.scenes.scene2d.ui.Button.ButtonStyle minusStyle = skin.get("minimize", com.badlogic.gdx.scenes.scene2d.ui.Button.ButtonStyle.class);
        final com.badlogic.gdx.scenes.scene2d.ui.Button.ButtonStyle plusStyle = skin.get("maximize", com.badlogic.gdx.scenes.scene2d.ui.Button.ButtonStyle.class);

        label = new Label(name, skin);
        minusButton = new com.badlogic.gdx.scenes.scene2d.ui.Button(minusStyle);
        plusButton = new Button(plusStyle);
        textField = new TextField("", skin);

        textField.setFocusTraversal(true);

        minusButton.addListener(new ChangeListener() { @Override
        public void changed(ChangeEvent changeEvent, Actor actor) {
            if (minusAction != null) {
                editor.shouldUpdateViewRenderer = true;
                try {minusAction.run();} catch (Exception e) {};
            }
        }});

        plusButton.addListener(new ChangeListener() { @Override
        public void changed(ChangeEvent changeEvent, Actor actor) {
            if (plusAction != null) {
                editor.shouldUpdateViewRenderer = true;
                try {plusAction.run();} catch (Exception e) {};
            }
        }});

        textField.addListener(new ChangeListener() {@Override
        public void changed(ChangeEvent changeEvent, Actor actor) {
            if (onTextSubmit != null) {
                editor.shouldUpdateViewRenderer = true;
                try {onTextSubmit.run();} catch (Exception e) {};
            }
        }});

        this.defaults().pad(5);
        this.add(label).width(LABEL_WIDTH);
        this.add(minusButton).size(BUTTON_WIDTH);
        this.add(textField).width(INPUT_WIDTH);
        this.add(plusButton).size(BUTTON_WIDTH);

    }

    @Override
    public void onMapLoad() {
        if (onUpdateMap != null) {
            try {onUpdateMap.run();} catch (Exception e) {};
        }
    }
}
