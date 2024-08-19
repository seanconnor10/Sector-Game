package com.disector.editor;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;

import com.badlogic.gdx.scenes.scene2d.utils.FocusListener;
import com.disector.Wall;

public class COMP_WallProperties extends COMP_UpdateableWindow {

    private int wallIndex = -1;
    private Wall wall = null;

    public COMP_WallProperties(String title, Skin skin, String styleName,Editor editor) {
        super(title, skin, styleName, editor);
        setWall(0);
        setup();
    }

    public COMP_WallProperties(String title, WindowStyle style,Editor editor) {
        super(title, style, editor);
        setWall(0);
        setup();
    }

    public COMP_WallProperties(String title, Skin skin,Editor editor) {
        super(title, skin, editor);
        setWall(0);
        setup();
    }

    @Override public void onMapLoad() {
        setWall(wallIndex); //Grab new sector reference
        /*
         * This call will call updateControlGroups() and
         * will update everything here, so we avoid calling
         * super.onMapLoad() which calls updateControlGroups()
         */
    }

    public void setWall(int wallIndex) {
        Wall previous = wall;
        int prevIndex = wallIndex;

        try {
            wall = editor.walls.get(wallIndex);
            this.wallIndex = wallIndex;
            updateControlGroups();
        } catch (Exception e) {
            wallIndex = prevIndex;
            wall = previous;
        }
    }

    // -----------------------------------------------------------------

    private void setup() {
        this.setResizable(true);

        this.row().fill().expandX();
        
        Table mainTable = new Table();
        mainTable.defaults().pad(10);
        mainTable.setFillParent(false);


        COMP_ControlGroup indexControl = new COMP_ControlGroup("Wall", getSkin(), editor);
        indexControl.minusAction  = () -> setWall(wallIndex-1);
        indexControl.plusAction   = () -> setWall(wallIndex+1);
        indexControl.onTextSubmit = () -> setWall(Integer.parseInt(indexControl.textField.getText()));
        indexControl.onUpdateMap  = () -> indexControl.textField.setText("" + wallIndex);
        indexControl.addListener(new FocusListener() {
            @Override
            public void keyboardFocusChanged(FocusEvent event, Actor actor, boolean focused) {
                if (!focused) {
                    indexControl.onUpdateMap.run(); //Update the text field, when leaving
                }
            }
        });
        actorsToUpdate.add(indexControl);
        mainTable.add(indexControl);

        mainTable.row();

        COMP_ControlGroup light_control = new COMP_ControlGroup("Floor Z", getSkin(), editor);
        light_control.minusAction  = () -> light_control.textField.setText(shiftLight(-0.1f));
        light_control.plusAction   = () -> light_control.textField.setText(shiftLight(0.1f));
        light_control.onTextSubmit = () -> setLight(Float.parseFloat(light_control.textField.getText()));
        light_control.onUpdateMap  = () -> light_control.textField.setText("" + wall.light);
        actorsToUpdate.add(light_control);
        mainTable.add(light_control);

        COMP_ControlGroup mat_control = new COMP_ControlGroup("Material", getSkin(), editor);
        mat_control.minusAction  = () -> mat_control.textField.setText(shiftMaterial(true, MATERIAL_LOCATION.MID));
        mat_control.plusAction   = () -> mat_control.textField.setText(shiftMaterial(false, MATERIAL_LOCATION.MID));
        mat_control.onTextSubmit = () -> setMaterial(mat_control.textField.getText(), MATERIAL_LOCATION.MID);
        mat_control.onUpdateMap  = () -> mat_control.textField.setText("" + wall.mat);
        actorsToUpdate.add(mat_control);
        mainTable.add(mat_control);

        mainTable.row();

        COMP_ControlGroup matLower_control = new COMP_ControlGroup("Mat Lower", getSkin(), editor);
        matLower_control.minusAction  = () -> matLower_control.textField.setText(shiftMaterial(true, MATERIAL_LOCATION.LOWER));
        matLower_control.plusAction   = () -> matLower_control.textField.setText(shiftMaterial(false, MATERIAL_LOCATION.LOWER));
        matLower_control.onTextSubmit = () -> setMaterial(matLower_control.textField.getText(), MATERIAL_LOCATION.LOWER);
        matLower_control.onUpdateMap  = () -> matLower_control.textField.setText("" + wall.matLower);
        actorsToUpdate.add(matLower_control);
        mainTable.add(matLower_control);

        COMP_ControlGroup matUpper_control = new COMP_ControlGroup("Mat Upper", getSkin(), editor);
        matUpper_control.minusAction  = () -> matUpper_control.textField.setText(shiftMaterial(true, MATERIAL_LOCATION.UPPER));
        matUpper_control.plusAction   = () -> matUpper_control.textField.setText(shiftMaterial(false, MATERIAL_LOCATION.UPPER));
        matUpper_control.onTextSubmit = () -> setMaterial(matUpper_control.textField.getText(), MATERIAL_LOCATION.UPPER);
        matUpper_control.onUpdateMap  = () -> matUpper_control.textField.setText("" + wall.matUpper);
        actorsToUpdate.add(matUpper_control);
        mainTable.add(matUpper_control);

        mainTable.row();

        mainTable.align(0);
        this.add(mainTable);

        this.pack();

        onMapLoad();
    }

    private String shiftLight(float amt) {
        wall.light = Math.max(0f, Math.min(1.0f, wall.light + amt));
        wall.light = Math.round(wall.light * 10)/10.0f;
        editor.shouldUpdateViewRenderer = true;
        return "" + wall.light;
    }

    private String setLight(float amt) {
        wall.light = Math.max(0f, Math.min(1.0f, amt));
        editor.shouldUpdateViewRenderer = true;
        return "" + wall.light;
    }

    private String shiftMaterial(boolean goDown, MATERIAL_LOCATION loc) {
        editor.shouldUpdateViewRenderer = true;

        switch (loc) {
        case MID:
            wall.mat = wall.mat + (goDown ? -1 : 1);
            if (wall.mat >= editor.materials.size) wall.mat = 0;
            if (wall.mat < 0) wall.mat = editor.materials.size - 1;
            return "" + wall.mat;
        case UPPER:
            wall.matUpper = wall.matUpper + (goDown ? -1 : 1);
            if (wall.matUpper >= editor.materials.size) wall.matUpper = 0;
            if (wall.matUpper < 0) wall.matUpper = editor.materials.size - 1;
            return "" + wall.matUpper;
        case LOWER:
            wall.matLower = wall.matLower + (goDown ? -1 : 1);
            if (wall.matLower >= editor.materials.size) wall.matLower = 0;
            if (wall.matLower < 0) wall.matLower = editor.materials.size - 1;
            return "" + wall.matLower;
        default:
            return "Huh?";
        }
    }

    private String setMaterial(String entry, MATERIAL_LOCATION loc) {
        boolean isNumber;
        int index = -1;

        try {
            float num = Float.parseFloat(entry);
            index = (int) Math.max(0, Math.min(editor.materials.size - 1, num));
            isNumber = true;
        } catch (NumberFormatException e) {
            isNumber = false;
        }

        boolean nameMatchFound = false;
        String matchName = "";

        if (!isNumber) {
            String term = entry.trim().toLowerCase();
            for (int i=0; i<editor.materials.size; i++) {
                String name = editor.materials.get(i).nameReference.toLowerCase();
                if (name.startsWith(term)) {
                    nameMatchFound = true;
                    matchName = name;
                    index = i;
                    break;
                }
            }
        }

        editor.shouldUpdateViewRenderer = true;

        switch (loc) {
        case MID:
            wall.mat = index;
            return nameMatchFound ? matchName : "" + wall.mat;
        case LOWER:
            wall.matLower = index;
            return nameMatchFound ? matchName : "" + wall.matLower;
        case UPPER:
            wall.matUpper = index;
            return nameMatchFound ? matchName : "" + wall.matUpper;
        default:
            return "Huh?";
        }

    }

    private enum MATERIAL_LOCATION{
        MID, UPPER, LOWER;
    }
}
