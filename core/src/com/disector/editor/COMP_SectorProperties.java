package com.disector.editor;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;

import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.disector.Sector;

public class COMP_SectorProperties extends Window {
    private final Editor editor;

    private Array<ControlGroup> actorsToUpdate = new Array<>();
    private int secIndex = -1;
    private Sector sec = null;

    private final Button.ButtonStyle minusStyle =
            getSkin().get("minimize", Button.ButtonStyle.class);
    private final Button.ButtonStyle plusStyle =
            getSkin().get("maximize", Button.ButtonStyle.class);

    public COMP_SectorProperties(String title, Skin skin, String styleName,Editor editor) {
        super(title, skin, styleName);
        this.editor = editor;
        setSector(0);
        setup();
    }

    public COMP_SectorProperties(String title, WindowStyle style,Editor editor) {
        super(title, style);
        this.editor = editor;
        setSector(0);
        setup();
    }

    public COMP_SectorProperties(String title, Skin skin,Editor editor) {
        super(title, skin);
        this.editor = editor;
        setSector(0);
        setup();
    }

    public void update() {
        for (ControlGroup item : actorsToUpdate) {
            item.onUpdateMap.run();
        }
    }

    public void setSector(int secIndex) {
        Sector previous = sec;
        int prevIndex = this.secIndex;

        try {
            this.secIndex = secIndex;
            this.sec = editor.sectors.get(secIndex);
            this.indexLabel.setText("Index: " + secIndex);
            update();
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

        ControlGroup floorZ_control = new ControlGroup("Floor Z", getSkin());
        floorZ_control.minusAction  = () -> floorZ_control.textField.setText(shiftFloor(-5));
        floorZ_control.plusAction   = () -> floorZ_control.textField.setText(shiftFloor(5));
        floorZ_control.onTextSubmit = () -> setFloor(Float.parseFloat(floorZ_control.textField.getText()));
        floorZ_control.onUpdateMap  = () -> floorZ_control.textField.setText("" + sec.floorZ);
        actorsToUpdate.add(floorZ_control);
        mainTable.add(floorZ_control);

        ControlGroup ceilZ_control = new ControlGroup("Ceil Z", getSkin());
        ceilZ_control.minusAction  = () -> ceilZ_control.textField.setText(shiftCeil(-5));
        ceilZ_control.plusAction   = () -> ceilZ_control.textField.setText(shiftCeil(5));
        ceilZ_control.onTextSubmit = () -> setCeil(Float.parseFloat(ceilZ_control.textField.getText()));
        ceilZ_control.onUpdateMap  = () -> ceilZ_control.textField.setText("" + sec.ceilZ);
        actorsToUpdate.add(ceilZ_control);
        mainTable.add(ceilZ_control);

        mainTable.row();

        ControlGroup floorLightControl = new ControlGroup("Floor Light", getSkin());
        floorLightControl.minusAction  = () -> floorLightControl.textField.setText(shiftFloorLight(-0.1f));
        floorLightControl.plusAction   = () -> floorLightControl.textField.setText(shiftFloorLight(0.1f));
        floorLightControl.onTextSubmit = () -> setFloorLight(Float.parseFloat(floorLightControl.textField.getText()));
        floorLightControl.onUpdateMap  = () -> floorLightControl.textField.setText("" + sec.lightFloor);
        actorsToUpdate.add(floorLightControl);
        mainTable.add(floorLightControl);

        ControlGroup ceilLightControl = new ControlGroup("Ceil Light", getSkin());
        ceilLightControl.minusAction  = () -> ceilLightControl.textField.setText(shiftCeilLight(-0.1f));
        ceilLightControl.plusAction   = () -> ceilLightControl.textField.setText(shiftCeilLight(0.1f));
        ceilLightControl.onTextSubmit = () -> setCeilLight(Float.parseFloat(ceilLightControl.textField.getText()));
        ceilLightControl.onUpdateMap  = () -> ceilLightControl.textField.setText("" + sec.lightCeil);
        actorsToUpdate.add(ceilLightControl);
        mainTable.add(ceilLightControl);

        mainTable.row();

        mainTable.align(0);
        this.add(mainTable);

        this.pack();

        update();
    }

    private class ControlGroup extends Table {
        Runnable minusAction;
        Runnable plusAction;
        Runnable onTextSubmit;
        Runnable onUpdateMap;

        Label label;
        Button minusButton;
        Button plusButton;
        TextField textField;

        public ControlGroup(String name, Skin skin) {
            super();

            label = new Label(name, skin);
            minusButton = new Button(minusStyle);
            plusButton = new Button(plusStyle);
            textField = new TextField("", skin);

            textField.setFocusTraversal(true);

            minusButton.addListener(new ChangeListener() { @Override
                public void changed(ChangeEvent changeEvent, Actor actor) {
                    if (minusAction != null) {
                        editor.shouldUpdateViewRenderer = true;
                        try {minusAction.run();} catch (Exception e) {};
                    }
                }});

            plusButton.addListener(new ChangeListener() { @Override
                public void changed(ChangeEvent changeEvent, Actor actor) {
                    if (plusAction != null) {
                        editor.shouldUpdateViewRenderer = true;
                        try {plusAction.run();} catch (Exception e) {};
                    }
            }});

            textField.addListener(new ChangeListener() {@Override
                public void changed(ChangeEvent changeEvent, Actor actor) {
                    if (onTextSubmit != null) {
                        editor.shouldUpdateViewRenderer = true;
                        try {onTextSubmit.run();} catch (Exception e) {};
                    }
            }});

            this.defaults().pad(5);
            this.add(label).width(70);
            this.add(minusButton).size(20);
            this.add(textField);
            this.add(plusButton).size(20);

        }

    }

    // ---------  Action Helpers -------------------------------

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

}
