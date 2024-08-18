package com.disector.inputrecorder;

import com.badlogic.gdx.utils.Array;

public class InputChainNode implements InputChainInterface {
    private Array<InputChainNode> children;
    private InputChainInterface parent;

    public boolean isActive;

    public InputChainNode(InputChainNode parent) {
        this.parent = parent;
        parent.addAsChild(this);
    }

    public void deactivate() {
        for (InputChainNode node : children)
            node.deactivate();
        this.isActive = false;
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
    public boolean isRoot() {
        return false;
    }

    @Override
    public void addAsChild(InputChainNode node) {
        children.add(node);
    }
}
