package com.disector.gui;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.disector.inputrecorder.InputChainInterface;
import com.disector.inputrecorder.InputRecorder;

public class GuiStage extends Stage implements InputChainInterface {
    private boolean isActive;

    public GuiStage(InputChainInterface parent) {
        super();
        parent.addAsChild(this);
    }

    public GuiStage(Viewport viewport, InputChainInterface parent) {
        super(viewport);
        parent.addAsChild(this);
    }

    public GuiStage(Viewport viewport, Batch batch, InputChainInterface parent) {
        super(viewport, batch);
        parent.addAsChild(this);
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
    public String showName() {
        return "GUI_STAGE";
    }
}
