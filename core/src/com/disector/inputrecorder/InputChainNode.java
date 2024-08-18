package com.disector.inputrecorder;

import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.utils.Array;

public class InputChainNode extends InputMultiplexer implements InputChainInterface {
    protected Array<InputChainNode> children = new Array<>();
    private final InputChainInterface parent;

    private final String name;

    public boolean isActive;

    public InputChainNode(InputChainInterface parent, String name) {
        this.parent = parent;
        parent.addAsChild(this);
        this.name = name;
    }

    private void deactivateAllChildren() {
        for (InputChainNode node : children)
            node.deactivateAllChildren();
        this.isActive = false;
    }

    @Override
    public InputRecorder.keyPressData getActionInfo(String name) {
        return isActive ? parent.getActionInfo(name) : InputRecorder.keyPressData.BLANK;
    }

    @Override
    public boolean isDown(int keyCode) {
        return isActive && parent.isDown(keyCode);
    }

    @Override
    public boolean isJustPressed(int keyCode) {
        return isActive && parent.isJustPressed(keyCode);
    }

    @Override
    public boolean isRoot() {
        return false;
    }

    @Override
    public void addAsChild(InputChainNode node) {
        children.add(node);
        addProcessor(node);
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
        return name;
    }

    // --------------------------------------

    @Override
    public boolean scrolled(float amountX, float amountY) {
        for (InputChainNode child : children) {
            if (child.isActive) child.scrolled(amountX, amountY);
        }
        return false;
    }
}
