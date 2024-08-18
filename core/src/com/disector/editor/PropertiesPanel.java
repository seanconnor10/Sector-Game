package com.disector.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.ScreenUtils;

import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.disector.Sector;
import com.disector.gui.GuiStage;
import com.disector.inputrecorder.InputChainNode;
import com.disector.inputrecorder.InputRecorder;

class PropertiesPanel extends Panel {
    private final static Pixmap.Format pixelFormat = Pixmap.Format.RGBA8888;
    private final static BitmapFont font = new BitmapFont( Gdx.files.local("assets/font/fira.fnt") );

    int chosenSectorIndex = -1;
    Sector chosenSector = null;

    FrameBuffer frame;
    //ShapeRenderer shape = new ShapeRenderer();
    //SpriteBatch batch = new SpriteBatch();

    Skin guiSkin;
    GuiStage sectorFieldsStage;

    PROPERTIES_PANEL_STATES state = PROPERTIES_PANEL_STATES.SHOW_SECTOR_FIELDS;

    PropertiesPanel(Editor editor) {
        super(editor);
        this.input = new InputChainNode(editor.input, "Props-Panel-Input");
        frame = new FrameBuffer(pixelFormat, 1, 1, false);
        setupStage();
        refreshPanelSize(rect);
        font.setColor(Color.WHITE);
    }

    @Override
    void stepFocused(float dt) {
        super.stepFocused(dt);
        sectorFieldsStage.act(dt);

        if (Gdx.input.isButtonPressed(Input.Buttons.MIDDLE)) {
            Vector3 camPos = sectorFieldsStage.getCamera().position;
            camPos.x -= InputRecorder.mouseDeltaX;
            camPos.y += InputRecorder.mouseDeltaY;
        }

    }

    void render() {
        frame.begin();
        ScreenUtils.clear(Color.SLATE);
        sectorFieldsStage.act();
        sectorFieldsStage.getBatch().setColor(Color.ORANGE);
        sectorFieldsStage.draw();
        frame.end();
    }

    void refreshPanelSize(Rectangle r) {
        frame.dispose();
        int w = Math.max( (int) r.width,  1 );
        int h = Math.max( (int) r.height, 1 );
        frame = new FrameBuffer(pixelFormat, w, h, false);
        //shape.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, w, h));
        //batch.setProjectionMatrix(shape.getProjectionMatrix());
        sectorFieldsStage.getViewport().update(w, h, false);
    }

    private void setupStage() {
        guiSkin = new Skin(
                Gdx.files.local("assets/skin/clean-crispy-ui.json"),
                new TextureAtlas( Gdx.files.local("assets/skin/clean-crispy-ui.atlas") )
        );

        sectorFieldsStage = new GuiStage(new ScreenViewport(), new SpriteBatch(), input);
        sectorFieldsStage.on();

        Table tableOne = new Table();
        tableOne.setFillParent(true);
        Button testButton = new TextButton("Please Don't Click!!", guiSkin);
        tableOne.add(testButton);

        sectorFieldsStage.addActor(tableOne);

    }

}
