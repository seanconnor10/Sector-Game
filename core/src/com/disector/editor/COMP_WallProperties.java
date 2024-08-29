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

        COMP_ControlGroup light_control = new COMP_ControlGroup("Light", getSkin(), editor);
        light_control.minusAction  = () -> light_control.textField.setText(shiftLight(-0.1f));
        light_control.plusAction   = () -> light_control.textField.setText(shiftLight(0.1f));
        light_control.onTextSubmit = () -> setLight(Float.parseFloat(light_control.textField.getText()));
        light_control.onUpdateMap  = () -> light_control.textField.setText("" + wall.light);
        actorsToUpdate.add(light_control);
        mainTable.add(light_control);

        mainTable.row();

        mainTable.add( new Label("-= MIDDLE =-", getSkin()) );

        COMP_ControlGroup mat_control = new COMP_ControlGroup("Material", getSkin(), editor);
        mat_control.minusAction  = () -> mat_control.textField.setText(shiftMaterial(true, MATERIAL_LOCATION.MID));
        mat_control.plusAction   = () -> mat_control.textField.setText(shiftMaterial(false, MATERIAL_LOCATION.MID));
        mat_control.onTextSubmit = () -> setMaterial(mat_control.textField.getText(), MATERIAL_LOCATION.MID);
        mat_control.onUpdateMap  = () -> mat_control.textField.setText("" + wall.mat);
        actorsToUpdate.add(mat_control);
        mainTable.add(mat_control);

        mainTable.row();

        COMP_ControlGroup xScale_control = new COMP_ControlGroup("X Scale", getSkin(), editor);
        xScale_control.minusAction  = () -> xScale_control.textField.setText( shiftXScale(-0.2f) );
        xScale_control.plusAction   = () -> xScale_control.textField.setText( shiftXScale(0.2f) );
        xScale_control.onTextSubmit = () -> setXScale(Float.parseFloat(xScale_control.textField.getText()));
        xScale_control.onUpdateMap  = () -> xScale_control.textField.setText("" + wall.xScale);
        actorsToUpdate.add(xScale_control);
        mainTable.add(xScale_control);

        COMP_ControlGroup yScale_control = new COMP_ControlGroup("Y Scale", getSkin(), editor);
        yScale_control.minusAction  = () -> yScale_control.textField.setText( shiftYScale(-0.2f) );
        yScale_control.plusAction   = () -> yScale_control.textField.setText( shiftYScale(0.2f) );
        yScale_control.onTextSubmit = () -> setYScale(Float.parseFloat(yScale_control.textField.getText()));
        yScale_control.onUpdateMap  = () -> yScale_control.textField.setText("" + wall.yScale);
        actorsToUpdate.add(yScale_control);
        mainTable.add(yScale_control);

        mainTable.row();

        COMP_ControlGroup xOffset_control = new COMP_ControlGroup("X Offset", getSkin(), editor);
        xOffset_control.minusAction  = () -> xOffset_control.textField.setText( shiftXOffset(-0.1f) );
        xOffset_control.plusAction   = () -> xOffset_control.textField.setText( shiftXOffset(0.1f) );
        xOffset_control.onTextSubmit = () -> setXOffset(Float.parseFloat(xOffset_control.textField.getText()));
        xOffset_control.onUpdateMap  = () -> xOffset_control.textField.setText("" + wall.xOffset);
        actorsToUpdate.add(xOffset_control);
        mainTable.add(xOffset_control);

        COMP_ControlGroup yOffset_control = new COMP_ControlGroup("Y Offset", getSkin(), editor);
        yOffset_control.minusAction  = () -> yOffset_control.textField.setText( shiftYOffset(-0.1f) );
        yOffset_control.plusAction   = () -> yOffset_control.textField.setText( shiftYOffset(0.1f) );
        yOffset_control.onTextSubmit = () -> setYOffset(Float.parseFloat(yOffset_control.textField.getText()));
        yOffset_control.onUpdateMap  = () -> yOffset_control.textField.setText("" + wall.yOffset);
        actorsToUpdate.add(yOffset_control);
        mainTable.add(yOffset_control);

        mainTable.row();

        mainTable.add( new Label("-= LOWER =-", getSkin()) );

        COMP_ControlGroup matLower_control = new COMP_ControlGroup("Material", getSkin(), editor);
        matLower_control.minusAction  = () -> matLower_control.textField.setText(shiftMaterial(true, MATERIAL_LOCATION.LOWER));
        matLower_control.plusAction   = () -> matLower_control.textField.setText(shiftMaterial(false, MATERIAL_LOCATION.LOWER));
        matLower_control.onTextSubmit = () -> setMaterial(matLower_control.textField.getText(), MATERIAL_LOCATION.LOWER);
        matLower_control.onUpdateMap  = () -> matLower_control.textField.setText("" + wall.matLower);
        actorsToUpdate.add(matLower_control);
        mainTable.add(matLower_control);

        mainTable.row();

        COMP_ControlGroup Lower_xScale_control = new COMP_ControlGroup("X Scale", getSkin(), editor);
        Lower_xScale_control.minusAction  = () -> Lower_xScale_control.textField.setText( shiftLowerXScale(-0.2f) );
        Lower_xScale_control.plusAction   = () -> Lower_xScale_control.textField.setText( shiftLowerXScale(0.2f) );
        Lower_xScale_control.onTextSubmit = () -> setLowerXScale(Float.parseFloat(Lower_xScale_control.textField.getText()));
        Lower_xScale_control.onUpdateMap  = () -> Lower_xScale_control.textField.setText("" + wall.Lower_xScale);
        actorsToUpdate.add(Lower_xScale_control);
        mainTable.add(Lower_xScale_control);

        COMP_ControlGroup Lower_yScale_control = new COMP_ControlGroup("Y Scale", getSkin(), editor);
        Lower_yScale_control.minusAction  = () -> Lower_yScale_control.textField.setText( shiftLowerYScale(-0.2f) );
        Lower_yScale_control.plusAction   = () -> Lower_yScale_control.textField.setText( shiftLowerYScale(0.2f) );
        Lower_yScale_control.onTextSubmit = () -> setLowerYScale(Float.parseFloat(Lower_yScale_control.textField.getText()));
        Lower_yScale_control.onUpdateMap  = () -> Lower_yScale_control.textField.setText("" + wall.Lower_yScale);
        actorsToUpdate.add(Lower_yScale_control);
        mainTable.add(Lower_yScale_control);

        mainTable.row();

        COMP_ControlGroup Lower_xOffset_control = new COMP_ControlGroup("X Offset", getSkin(), editor);
        Lower_xOffset_control.minusAction  = () -> Lower_xOffset_control.textField.setText( shiftLowerXOffset(-0.1f) );
        Lower_xOffset_control.plusAction   = () -> Lower_xOffset_control.textField.setText( shiftLowerXOffset(0.1f) );
        Lower_xOffset_control.onTextSubmit = () -> setLowerXOffset(Float.parseFloat(Lower_xOffset_control.textField.getText()));
        Lower_xOffset_control.onUpdateMap  = () -> Lower_xOffset_control.textField.setText("" + wall.Lower_xOffset);
        actorsToUpdate.add(Lower_xOffset_control);
        mainTable.add(Lower_xOffset_control);

        COMP_ControlGroup Lower_yOffset_control = new COMP_ControlGroup("Y Offset", getSkin(), editor);
        Lower_yOffset_control.minusAction  = () -> Lower_yOffset_control.textField.setText( shiftLowerYOffset(-0.1f) );
        Lower_yOffset_control.plusAction   = () -> Lower_yOffset_control.textField.setText( shiftLowerYOffset(0.1f) );
        Lower_yOffset_control.onTextSubmit = () -> setLowerYOffset(Float.parseFloat(Lower_yOffset_control.textField.getText()));
        Lower_yOffset_control.onUpdateMap  = () -> Lower_yOffset_control.textField.setText("" + wall.Lower_yOffset);
        actorsToUpdate.add(Lower_yOffset_control);
        mainTable.add(Lower_yOffset_control);

        mainTable.row();

        mainTable.add( new Label("-= UPPER =-", getSkin()) );

        COMP_ControlGroup matUpper_control = new COMP_ControlGroup("Material", getSkin(), editor);
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

    private String setXScale(float amt) {
        wall.xScale = amt;
        editor.shouldUpdateViewRenderer = true;
        return "" + wall.xScale;
    }

    private String shiftXScale(float amt) {
        wall.xScale += amt;
        editor.shouldUpdateViewRenderer = true;
        return "" + wall.xScale;
    }

    private String setYScale(float amt) {
        wall.yScale = amt;
        editor.shouldUpdateViewRenderer = true;
        return "" + wall.yScale;
    }

    private String shiftYScale(float amt) {
        wall.yScale += amt;
        editor.shouldUpdateViewRenderer = true;
        return "" + wall.yScale;
    }

    private String setXOffset(float amt) {
        wall.xOffset = amt;
        editor.shouldUpdateViewRenderer = true;
        return "" + wall.xOffset;
    }

    private String shiftXOffset(float amt) {
        wall.xOffset += amt;
        editor.shouldUpdateViewRenderer = true;
        return "" + wall.xOffset;
    }

    private String setYOffset(float amt) {
        wall.yOffset = amt;
        editor.shouldUpdateViewRenderer = true;
        return "" + wall.yOffset;
    }

    private String shiftYOffset(float amt) {
        wall.yOffset += amt;
        editor.shouldUpdateViewRenderer = true;
        return "" + wall.yOffset;
    }

    private String setLowerXScale(float amt) {
        wall.Lower_xScale = amt;
        editor.shouldUpdateViewRenderer = true;
        return "" + wall.Lower_xScale;
    }

    private String shiftLowerXScale(float amt) {
        wall.Lower_xScale += amt;
        editor.shouldUpdateViewRenderer = true;
        return "" + wall.Lower_xScale;
    }

    private String setLowerYScale(float amt) {
        wall.Lower_yScale = amt;
        editor.shouldUpdateViewRenderer = true;
        return "" + wall.Lower_yScale;
    }

    private String shiftLowerYScale(float amt) {
        wall.Lower_yScale += amt;
        editor.shouldUpdateViewRenderer = true;
        return "" + wall.Lower_yScale;
    }

    private String setLowerXOffset(float amt) {
        wall.Lower_xOffset = amt;
        editor.shouldUpdateViewRenderer = true;
        return "" + wall.Lower_xOffset;
    }

    private String shiftLowerXOffset(float amt) {
        wall.Lower_xOffset += amt;
        editor.shouldUpdateViewRenderer = true;
        return "" + wall.Lower_xOffset;
    }

    private String setLowerYOffset(float amt) {
        wall.Lower_yOffset = amt;
        editor.shouldUpdateViewRenderer = true;
        return "" + wall.Lower_yOffset;
    }

    private String shiftLowerYOffset(float amt) {
        wall.Lower_yOffset += amt;
        editor.shouldUpdateViewRenderer = true;
        return "" + wall.Lower_yOffset;
    }

    private String setUpperXScale(float amt) {
        wall.Upper_xScale = amt;
        editor.shouldUpdateViewRenderer = true;
        return "" + wall.Upper_xScale;
    }

    private String shiftUpperXScale(float amt) {
        wall.Upper_xScale += amt;
        editor.shouldUpdateViewRenderer = true;
        return "" + wall.Upper_xScale;
    }

    private String setUpperYScale(float amt) {
        wall.Upper_yScale = amt;
        editor.shouldUpdateViewRenderer = true;
        return "" + wall.Upper_yScale;
    }

    private String shiftUpperYScale(float amt) {
        wall.Upper_yScale += amt;
        editor.shouldUpdateViewRenderer = true;
        return "" + wall.Upper_yScale;
    }

    private String setUpperXOffset(float amt) {
        wall.Upper_xOffset = amt;
        editor.shouldUpdateViewRenderer = true;
        return "" + wall.Upper_xOffset;
    }

    private String shiftUpperXOffset(float amt) {
        wall.Upper_xOffset += amt;
        editor.shouldUpdateViewRenderer = true;
        return "" + wall.Upper_xOffset;
    }

    private String setUpperYOffset(float amt) {
        wall.Upper_yOffset = amt;
        editor.shouldUpdateViewRenderer = true;
        return "" + wall.Upper_yOffset;
    }

    private String shiftUpperYOffset(float amt) {
        wall.Upper_yOffset += amt;
        editor.shouldUpdateViewRenderer = true;
        return "" + wall.Upper_yOffset;
    }
    
    private enum MATERIAL_LOCATION{
        MID, UPPER, LOWER;
    }
}
