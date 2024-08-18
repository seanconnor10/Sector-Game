package com.disector.inputrecorder;

public interface InputChainInterface {
    InputRecorder.keyPressData getActionInfo(String name);

    boolean isDown(int keyCode);
    boolean isJustPressed(int keyCode);

    boolean isRoot();

    void addAsChild(InputChainNode node);

}
