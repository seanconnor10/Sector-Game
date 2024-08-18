package com.disector.inputrecorder;

import com.badlogic.gdx.utils.Array;

public class InputChainNode {
    Array<InputChainNode> children;
    InputChainNode parent;

    boolean isRoot;
    boolean isActive;

    public InputChainNode(InputChainNode parent) {
        this.parent = parent;
    }

    public InputChainNode() {
        isRoot = true;
    }

    public InputChainNode makeChild() {
        InputChainNode newNode = new InputChainNode(this);
        children.add(newNode);
        return newNode;
    }

    public void deactivate() {
        for (InputChainNode node : children)
            node.deactivate();
        if (!isRoot) this.isActive = false;
    }
}
