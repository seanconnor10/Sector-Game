package com.disector.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import com.disector.Sector;
import com.disector.inputrecorder.InputChainStage;
import com.disector.inputrecorder.InputChainNode;
import com.disector.inputrecorder.InputRecorder;

class PropertiesPanel extends Panel {
    private static Color COL_BACK = new Color(0x80_80_A0_FF);

    private final static Pixmap.Format pixelFormat = Pixmap.Format.RGBA8888;
    private final static BitmapFont font = new BitmapFont( Gdx.files.local("assets/font/fira.fnt") );

    //int chosenSectorIndex = -1;
    //Sector chosenSector = null;
    COMP_SectorProperties sectorPropertiesWindow;
    COMP_WallProperties wallPropertiesWindow;

    FrameBuffer frame;

    Skin guiSkin;
    InputChainStage stage;

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
        stage.act(dt);

        if (Gdx.input.isButtonPressed(Input.Buttons.MIDDLE)) {
            Vector3 camPos = stage.getCamera().position;
            camPos.x -= InputRecorder.mouseDeltaX;
            camPos.y += InputRecorder.mouseDeltaY;
        }

    }

    void render() {
        frame.begin();
        ScreenUtils.clear(COL_BACK);
        stage.act();
        stage.draw();
        frame.end();
    }

    void refreshPanelSize(Rectangle r) {
        frame.dispose();
        int w = Math.max( (int) r.width,  1 );
        int h = Math.max( (int) r.height, 1 );
        frame = new FrameBuffer(pixelFormat, w, h, false);
        stage.getViewport().update(w, h, false);
    }

    private void setupStage() {
        guiSkin = new Skin(
                Gdx.files.local("assets/skin/clean-crispy-ui.json"),
                new TextureAtlas( Gdx.files.local("assets/skin/clean-crispy-ui.atlas") )
        );

        stage = new InputChainStage(new ScreenViewport(), new SpriteBatch(), input);
        stage.on();

        sectorPropertiesWindow = new COMP_SectorProperties("Sector Properties", guiSkin, editor);
        wallPropertiesWindow = new COMP_WallProperties("Wall Properties", guiSkin, editor);

        stage.addActor(sectorPropertiesWindow);
        stage.addActor(wallPropertiesWindow);

    }

}
