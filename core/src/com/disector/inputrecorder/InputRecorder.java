package com.disector.inputrecorder;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.utils.Array;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.HashMap;

public class InputRecorder extends InputMultiplexer implements InputChainInterface {

    /*
     *  The inputrecorder package provides a way of
     *  1) globally storing 'just released this frame' info for key input
     *  2) mapping named actions to keycodes
     *  3) distributing global input info through a tree of nodes assigned to
     *     differing parts of the application (ie Console, Game/Editor), offering
     *     listener input (via extending InputMultiplexer) as well as polled
     *     input
     */

    public static Map<String, Integer> keyBinds = new HashMap<>();
    public static Map<Integer, keyPressData> keyPressMap = new HashMap<>();

    public static int keyCount;

    public static float mouseDeltaX, mouseDeltaY;

    public static void updateKeys() {
        /*
         * Called once per game frame to store polled input for every
         * key which has an action bound to it
         */
        mouseDeltaX = Gdx.input.getDeltaX();
        mouseDeltaY = Gdx.input.getDeltaY();

        for (Map.Entry<Integer, keyPressData> keyEntry : keyPressMap.entrySet()) {
            keyEntry.getValue().justReleased = keyEntry.getValue().isDown && !Gdx.input.isKeyPressed(keyEntry.getKey());
            keyEntry.getValue().isDown = Gdx.input.isKeyPressed(keyEntry.getKey());
            keyEntry.getValue().justPressed = Gdx.input.isKeyJustPressed(keyEntry.getKey());
        }
    }

    public static void repopulateKeyCodeMap() {
        Field[] fields = KeyMapping.class.getFields();

        keyBinds.clear(); //Map of 'Action Names' and the keyCode they're assigned to
        for (Field field : fields) {
            if (!field.isAnnotationPresent(KeyCode.class)) continue;;
            String fieldName = field.getName();
            try {
                keyBinds.put(fieldName, field.getInt(InputRecorder.class));
            } catch (Exception e) {
                System.out.println(" -=== ERROR ===-");
                System.out.println(" InputRecorder failed to repopulate KeyCodeMap via Reflection");
                System.out.println(" Exception Type: " + e.getClass().getName() );
                System.out.println(" " + e.getMessage());
                System.out.println(" -=============-");
                System.exit(1);
            }
        }

        System.out.println(printKeyBindInfo());

        keyPressMap.clear(); //Map of keyCodes assigned to an action and a keyPressData for each
        for (Integer code : keyBinds.values() ) {
            keyPressMap.put(code, new keyPressData() );
        }
        //System.out.println("InputRecorder::KeyPressMap = " + keyPressMap.entrySet().toString());

        keyCount = keyPressMap.size();
    }

    public static keyPressData getKeyInfo(String actionName) {
        keyPressData data = keyPressMap.getOrDefault( keyBinds.getOrDefault(actionName, -1), null );
        if (data == null) {
            throw new RuntimeException("Action: " + actionName + " not found in KeyBind map.");
        }
        return data;
    }

    public static String printKeyBindInfo() {
        return "InputRecorder::KeyCodeMap = " + keyBinds.toString().replace("}", "\n}").replace("{", "{\n    ").replace(", ", ", \n    ");
    }

    public static class keyPressData {
        public boolean isDown, justPressed, justReleased;

        public static keyPressData BLANK = new keyPressData();

        @Override
        public String toString() {
            String str = (isDown ? "CurrentlyPressed " : "") + (justPressed ? "NewlyPressed " : "") + (justReleased ? "NewlyReleased " : "");
            if (str.isEmpty()) str = "none";
            return str;
        }
    }

    // ------- Non-Static Methods --------------------------

    /* Used to allow an instance of this class to serve
     * as a root of the tree distributing input visibility
     * Necessary since this otherwise static class couldn't
     * implement InputChainInterface
     */

    private final Array<InputChainNode> children = new Array<>();

    @Override
    public boolean isDown(int keyCode) {
        return Gdx.input.isKeyPressed(keyCode);
    }

    @Override
    public boolean isJustPressed(int keyCode) {
        return Gdx.input.isKeyJustPressed(keyCode);
    }

    @Override
    public keyPressData getActionInfo(String actionName) {
        return getKeyInfo(actionName);
    }

    @Override
    public boolean isRoot() {
        return true;
    }

    @Override
    public void addAsChild(InputChainNode node) {
        children.add(node);
        addProcessor(node);
    }

    @Override
    public void on() {
        //Since an instance of this class is used as the root of the input tree
        //We don't every really want to toggle activation?
    }

    @Override
    public void off() {
        //Since an instance of this class is used as the root of the input tree
        //We don't every really want to toggle activation?
    }

    @Override
    public void toggle() {

    }

    @Override
    public String showName() {
        return "ROOT!";
    }

    //InputMultiplexer Methods

    @Override
    public boolean scrolled(float amountX, float amountY) {
        for (InputChainNode child : children) {
            if (child.isActive) child.scrolled(amountX, amountY);
        }
        return false;
    }
}
