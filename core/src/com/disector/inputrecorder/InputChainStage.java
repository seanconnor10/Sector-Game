package com.disector.inputrecorder;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.disector.editor.UpdatableComponent;

public class InputChainStage extends Stage implements InputChainInterface {
    private final InputChainInterface parent;

    private boolean isActive;

    public InputChainStage(InputChainInterface parent) {
        super();
        this.parent = parent;
        parent.addAsChild(this);
    }

    public InputChainStage(Viewport viewport, InputChainInterface parent) {
        super(viewport);
        this.parent = parent;
        parent.addAsChild(this);
    }

    public InputChainStage(Viewport viewport, Batch batch, InputChainInterface parent) {
        super(viewport, batch);
        this.parent = parent;
        parent.addAsChild(this);
    }

    public void onMapLoad() {
        for (Actor item : getActors()) {
            if (item instanceof UpdatableComponent) {
                ((UpdatableComponent) item).onMapLoad();
            }
        }
    }

    @Override
    public InputRecorder.keyPressData getActionInfo(String name) {
        return null;
    }

    @Override
    public boolean isDown(int keyCode) {
        return false;
    }

    @Override
    public boolean isJustPressed(int keyCode) {
        return false;
    }

    @Override
    public void addAsChild(InputChainInterface node) {

    }

    @Override
    public boolean isRoot() {
        return false;
    }

    @Override
    public boolean isActive() {
        return isActive;
    }

    @Override
    public void on() {
        isActive = true;
    }

    @Override
    public void off() {
        isActive = false;
    }

    @Override
    public void toggle() {
        isActive = !isActive;
    }

    @Override
    public void remove(InputChainInterface node) {

    }


    @Override
    public String showName() {
        return "GUI_STAGE";
    }
}
