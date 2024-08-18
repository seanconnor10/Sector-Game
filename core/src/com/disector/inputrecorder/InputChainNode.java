package com.disector.inputrecorder;

import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Null;
import org.jetbrains.annotations.Nullable;

public class InputChainNode implements InputChainInterface {
    protected Array<InputChainInterface> children = new Array<>();

    private final InputChainInterface parent;

    private final String name;

    public boolean isActive;

    public InputChainNode(InputChainInterface parent, String name) {
        this.parent = parent;
        parent.addAsChild(this);
        this.name = name;
    }

//    private void deactivateAllChildren() {
//        for (InputChainNode node : children)
//            node.deactivateAllChildren();
//        this.isActive = false;
//    }

    // --- InputChainInterface Methods ----------------------------------------

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
    public void addAsChild(InputChainInterface node) {
        children.add(node);
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

    @Override
    public boolean isActive() {
        return isActive;
    }

    // --- InputProcessor Methods ----------------------------------------

    @Override
    public boolean keyDown(int i) {
        for (InputChainInterface child : children) {
            if (child.isActive()) child.keyDown(i);
        }
        return false;
    }

    @Override
    public boolean keyUp(int i) {
        for (InputChainInterface child : children) {
            if (child.isActive()) child.keyUp(i);
        }
        return false;
    }

    @Override
    public boolean keyTyped(char c) {
        for (InputChainInterface child : children) {
            if (child.isActive()) child.keyTyped(c);
        }
        return false;
    }

    @Override
    public boolean touchDown(int i, int i1, int i2, int i3) {
        for (InputChainInterface child : children) {
            if (child.isActive()) child.touchDown(i, i1, i2, i3);
        }
        return false;
    }

    @Override
    public boolean touchUp(int i, int i1, int i2, int i3) {
        for (InputChainInterface child : children) {
            if (child.isActive()) child.touchUp(i, i1, i2, i3);
        }
        return false;
    }

    @Override
    public boolean touchCancelled(int i, int i1, int i2, int i3) {
        for (InputChainInterface child : children) {
            if (child.isActive()) child.touchCancelled(i, i1, i2, i3);
        }
        return false;
    }

    @Override
    public boolean touchDragged(int i, int i1, int i2) {
        for (InputChainInterface child : children) {
            if (child.isActive()) child.touchDragged(i, i1, i2);
        }
        return false;
    }

    @Override
    public boolean mouseMoved(int i, int i1) {
        for (InputChainInterface child : children) {
            if (child.isActive()) child.mouseMoved(i, i1);
        }
        return false;
    }

    @Override
    public boolean scrolled(float v, float v1) {
        for (InputChainInterface child : children) {
            if (child.isActive()) child.scrolled(v, v1);
        }
        return false;
    }
}
