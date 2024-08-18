package com.disector.editor;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.disector.inputrecorder.InputChainInterface;
import com.disector.inputrecorder.InputChainNode;

import java.util.function.Function;

class Panel {
    Editor editor;
    Rectangle rect;

    /**
     * Used for when we want to take input only if this panel is focused/
     * If we always want input for something regardless of panel focus
     * call from editor::input
     */
    InputChainInterface input;

    Function<Void, Void> stepFunction = (Void) -> {return Void;};

    boolean isForcingMouseFocus;

    final Array<Button> buttons = new Array<>();

    public Panel(Editor editor, String inputName) {
        this.editor = editor;
        this.rect = new Rectangle();
        this.input = new InputChainNode(editor.input, inputName);
    }

    void resize(Rectangle rect) {
        resize(rect.x, rect.y, rect.width, rect.height);
    }

    void resize(float x, float y, float width, float height) {
        rect.set(x, y, width, height);
        rearrangeButtons();
    }

    void rearrangeButtons() {
        int placedButtons = 0;
        for (Button b : buttons) {
            if (b.active) {
                b.rect.set(2 + placedButtons * 140, 2, 100, 28);
                placedButtons++;
            } else {
                b.rect.set(-1,-1,-1,-1);
            }
        }
    }

    Button getButtonByText(String text) {
        for (Button b : buttons) {
            if (b.text.equalsIgnoreCase(text))
                return b;
        }
        return null;
        //Needing .toArray() makes this second method probably slower?
        //return Arrays.stream(buttons.toArray())
        //        .filter( b -> b.text.equals(text) ).findFirst().orElse(null);
    }

    void clickedIn() {
        for (Button b : buttons) {
            if (editor.mouseIn(b)) {
                editor.clickedButton = b;
                b.pressed = true;
                break;
            }
        }
    }

    void step(float dt) {

    }

    float relX() {
        return editor.mouseX - rect.x;
    }

    float relY() {
        return editor.mouseY - rect.y;
    }

}
