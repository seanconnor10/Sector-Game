package com.disector.editor;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.utils.FocusListener;

import com.disector.Sector;

public class COMP_SectorProperties extends COMP_UpdateableWindow {

    private int secIndex = -1;
    private Sector sec = null;

    public COMP_SectorProperties(String title, Skin skin, String styleName,Editor editor) {
        super(title, skin, styleName, editor);
        setSector(0);
        setup();
    }

    public COMP_SectorProperties(String title, WindowStyle style,Editor editor) {
        super(title, style, editor);
        setSector(0);
        setup();
    }

    public COMP_SectorProperties(String title, Skin skin,Editor editor) {
        super(title, skin, editor);
        setSector(0);
        setup();
    }

    @Override public void onMapLoad() {
        setSector(secIndex); //Grab new sector reference
        /*
         * This call will call updateControlGroups() and
         * will update everything here
         */
    }

    public void setSector(int secIndex) {
        Sector previous = sec;
        int prevIndex = this.secIndex;

        try {
            this.secIndex = secIndex;
            this.sec = editor.sectors.get(secIndex);
            this.indexLabel.setText("Index: " + secIndex);
            updateControlGroups();
        } catch (Exception e) {
            this.secIndex = prevIndex;
            this.sec = previous;
        }
    }

    // -----------------------------------------------------------------

    //Element that must be accessed later??...
    private Label indexLabel = new Label("Index: " + secIndex, getSkin());

    private void setup() {
        this.setResizable(true);

        this.row().fill().expandX();

        final Button.ButtonStyle minusStyle = getSkin().get("minimize", Button.ButtonStyle.class);
        final Button.ButtonStyle plusStyle = getSkin().get("maximize", Button.ButtonStyle.class);

        Table mainTable = new Table();
        mainTable.defaults().pad(10);
        mainTable.setFillParent(false);

        mainTable.add(indexLabel).colspan(2);
        mainTable.row();

        COMP_ControlGroup floorZ_control = new COMP_ControlGroup("Floor Z", getSkin(), editor);
        floorZ_control.minusAction  = () -> floorZ_control.textField.setText(shiftFloor(-5));
        floorZ_control.plusAction   = () -> floorZ_control.textField.setText(shiftFloor(5));
        floorZ_control.onTextSubmit = () -> setFloor(Float.parseFloat(floorZ_control.textField.getText()));
        floorZ_control.onUpdateMap  = () -> floorZ_control.textField.setText("" + sec.floorZ);
        actorsToUpdate.add(floorZ_control);
        mainTable.add(floorZ_control);

        COMP_ControlGroup ceilZ_control = new COMP_ControlGroup("Ceil Z", getSkin(), editor);
        ceilZ_control.minusAction  = () -> ceilZ_control.textField.setText(shiftCeil(-5));
        ceilZ_control.plusAction   = () -> ceilZ_control.textField.setText(shiftCeil(5));
        ceilZ_control.onTextSubmit = () -> setCeil(Float.parseFloat(ceilZ_control.textField.getText()));
        ceilZ_control.onUpdateMap  = () -> ceilZ_control.textField.setText("" + sec.ceilZ);
        actorsToUpdate.add(ceilZ_control);
        mainTable.add(ceilZ_control);

        mainTable.row();

        COMP_ControlGroup floorLightControl = new COMP_ControlGroup("Floor Light", getSkin(), editor);
        floorLightControl.minusAction  = () -> floorLightControl.textField.setText(shiftFloorLight(-0.1f));
        floorLightControl.plusAction   = () -> floorLightControl.textField.setText(shiftFloorLight(0.1f));
        floorLightControl.onTextSubmit = () -> setFloorLight(Float.parseFloat(floorLightControl.textField.getText()));
        floorLightControl.onUpdateMap  = () -> floorLightControl.textField.setText("" + sec.lightFloor);
        actorsToUpdate.add(floorLightControl);
        mainTable.add(floorLightControl);

        COMP_ControlGroup ceilLightControl = new COMP_ControlGroup("Ceil Light", getSkin(), editor);
        ceilLightControl.minusAction  = () -> ceilLightControl.textField.setText(shiftCeilLight(-0.1f));
        ceilLightControl.plusAction   = () -> ceilLightControl.textField.setText(shiftCeilLight(0.1f));
        ceilLightControl.onTextSubmit = () -> setCeilLight(Float.parseFloat(ceilLightControl.textField.getText()));
        ceilLightControl.onUpdateMap  = () -> ceilLightControl.textField.setText("" + sec.lightCeil);
        actorsToUpdate.add(ceilLightControl);
        mainTable.add(ceilLightControl);

        mainTable.row();

        COMP_ControlGroup floorMaterialControl = new COMP_ControlGroup("Floor Tex", getSkin(), editor);
        floorMaterialControl.minusAction  = () -> floorMaterialControl.textField.setText(shiftFloorMaterial(true));
        floorMaterialControl.plusAction   = () -> floorMaterialControl.textField.setText(shiftFloorMaterial(false));
        //Don't update when simply typing
        floorMaterialControl.onUpdateMap  = () -> floorMaterialControl.textField.setText("" + sec.matFloor);
        floorMaterialControl.addListener(new FocusListener() {
            @Override
            public void keyboardFocusChanged(FocusEvent event, Actor actor, boolean focused) {
                if (!focused) {
                    floorMaterialControl.textField.setText(
                        setFloorMaterial(floorMaterialControl.textField.getText())
                    );
                }
            }
        });
        actorsToUpdate.add(floorMaterialControl);
        mainTable.add(floorMaterialControl);

        COMP_ControlGroup ceilMaterialControl = new COMP_ControlGroup("Ceil Tex", getSkin(), editor);
        ceilMaterialControl.minusAction  = () -> ceilMaterialControl.textField.setText(shiftCeilMaterial(true));
        ceilMaterialControl.plusAction   = () -> ceilMaterialControl.textField.setText(shiftCeilMaterial(false));
        //Don't update when simply typing
        ceilMaterialControl.onUpdateMap  = () -> ceilMaterialControl.textField.setText("" + sec.matCeil);
        ceilMaterialControl.addListener(new FocusListener() {
            @Override
            public void keyboardFocusChanged(FocusEvent event, Actor actor, boolean focused) {
                if (!focused) {
                    ceilMaterialControl.textField.setText(
                            setCeilMaterial(ceilMaterialControl.textField.getText())
                    );
                }
            }
        });
        actorsToUpdate.add(ceilMaterialControl);
        mainTable.add(ceilMaterialControl);

        mainTable.align(0);
        this.add(mainTable);

        this.pack();

        onMapLoad();
    }

    private String shiftFloor(float amt) {
        sec.floorZ = Math.round( sec.floorZ + amt );
        editor.shouldUpdateViewRenderer = true;
        return "" + sec.floorZ;
    }

    private String setFloor(float amt) {
        sec.floorZ = amt;
        editor.shouldUpdateViewRenderer = true;
        return "" + sec.floorZ;
    }

    private String shiftCeil(float amt) {
        sec.ceilZ = Math.round( sec.ceilZ + amt );
        editor.shouldUpdateViewRenderer = true;
        return "" + sec.ceilZ;
    }

    private String setCeil(float amt) {
        sec.ceilZ = amt;
        editor.shouldUpdateViewRenderer = true;
        return "" + sec.ceilZ;
    }

    private String shiftFloorLight(float amt) {
        sec.lightFloor = Math.max(0f, Math.min(1.0f, sec.lightFloor + amt));
        sec.lightFloor = Math.round(sec.lightFloor * 10)/10.0f;
        editor.shouldUpdateViewRenderer = true;
        return "" + sec.lightFloor;
    }

    private String setFloorLight(float amt) {
        sec.lightFloor = Math.max(0f, Math.min(1.0f, amt));
        editor.shouldUpdateViewRenderer = true;
        return "" + sec.lightFloor;
    }

    private String shiftCeilLight(float amt) {
        sec.lightCeil = Math.max(0f, Math.min(1.0f, sec.lightCeil + amt));
        sec.lightCeil = Math.round(sec.lightCeil * 10)/10.0f;
        editor.shouldUpdateViewRenderer = true;
        return "" + sec.lightCeil;
    }

    private String setCeilLight(float amt) {
        sec.lightCeil = Math.max(0f, Math.min(1.0f, amt));
        editor.shouldUpdateViewRenderer = true;
        return "" + sec.lightCeil;
    }

    private String shiftFloorMaterial(boolean goDown) {
        sec.matFloor = sec.matFloor + (goDown ? -1 : 1);
        if (sec.matFloor >= editor.materials.size) sec.matFloor = 0;
        if (sec.matFloor < 0) sec.matFloor = editor.materials.size - 1;
        editor.shouldUpdateViewRenderer = true;
        return "" + sec.matFloor;
    }

    private String setFloorMaterial(String entry) {
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

        sec.matFloor = index;
        editor.shouldUpdateViewRenderer = true;

        return nameMatchFound ? matchName : "" + sec.matFloor;
    }

    private String shiftCeilMaterial(boolean goDown) {
        sec.matCeil = sec.matCeil + (goDown ? -1 : 1);
        if (sec.matCeil >= editor.materials.size) sec.matCeil = 0;
        if (sec.matCeil < 0) sec.matCeil = editor.materials.size - 1;
        editor.shouldUpdateViewRenderer = true;
        return "" + sec.matCeil;
    }

    private String setCeilMaterial(String entry) {
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

        sec.matCeil = index;
        editor.shouldUpdateViewRenderer = true;

        return nameMatchFound ? matchName : "" + sec.matCeil;
    }

}
