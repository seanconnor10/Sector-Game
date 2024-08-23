package com.disector.inputrecorder;

import com.badlogic.gdx.InputProcessor;

public interface InputChainInterface extends InputProcessor {
    InputRecorder.keyPressData getActionInfo(String name);

    boolean isDown(int keyCode);
    boolean isJustPressed(int keyCode);

    void addAsChild(InputChainInterface node);

    boolean isRoot();
    boolean isActive();

    void on();
    void off();
    void toggle();
    void remove(InputChainInterface node);

    String showName();
}
