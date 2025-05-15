package com.disector.editor;

import com.disector.I_AppFocus;

public interface EditorInterface extends I_AppFocus {
    void step(float deltaTime);
    void draw();
    ActiveSelection getSelection();
    void resize(int w, int h);
}
