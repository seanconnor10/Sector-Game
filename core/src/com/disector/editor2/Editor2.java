package com.disector.editor2;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;

import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.disector.*;
import com.disector.inputrecorder.InputChainInterface;
import com.disector.inputrecorder.InputChainNode;
import com.disector.inputrecorder.InputChainStage;

public class Editor2 implements I_AppFocus {
    static BitmapFont font = new BitmapFont(Gdx.files.local("assets/font/fira.fnt"));

    final Application app;
    final Array<Wall> walls;
    final Array<Sector> sectors;
    final Array<Material> materials;
    final ShapeRenderer shape;
    final SpriteBatch batch;

    public InputChainInterface input;
    public InputChainStage stage;
    public Skin skin;

    public Editor2(Application app,InputChainInterface inputParent) {
        this.app = app;
        this.walls = app.walls;
        this.sectors = app.sectors;
        this.materials = app.materials;
        this.shape = app.shape;
        this.batch = app.batch;
        this.input = new InputChainNode(inputParent, "Editor2");

        setupStage();
    }

    public void step(float deltaTime) {

    }

    public void draw() {
        batch.begin();
        ScreenUtils.clear(Color.GRAY);
        stage.act();
        stage.draw();
        batch.end();
    }

    public void resize(int w, int h) {
        stage.getViewport().update(Math.max(w, 1), Math.max(h, 1), true);
    }

    @Override
    public InputChainInterface getInputReference() {
        return input;
    }

    private void setupStage() {
        stage = new InputChainStage(new ScreenViewport(), new SpriteBatch(), input);
        stage.on();

        skin = new Skin(
                Gdx.files.local("assets/skin/2/skin2.json"),
                new TextureAtlas( Gdx.files.local("assets/skin/2/skin2.atlas") )
        );

        Table mainContainer = new Table(skin);
        mainContainer.setFillParent(true);
        stage.addActor(mainContainer);

        Table topSection = new Table(skin);
        Table topToolbar = new Table(skin);
        TextButton button1 = new TextButton("Button 1", skin);
        TextButton button2 = new TextButton("Button 2", skin);
        TextButton button11 = new TextButton("Button 11", skin);
        TextButton button22 = new TextButton("Button 22", skin);
        topToolbar.add(button1);
        topToolbar.add(button2);
        topToolbar.add(button11);
        topToolbar.add(button22);
        topSection.add(topToolbar).left().expand();
        Table topRightToolbar = new Table(skin);
        TextButton closeButton = new TextButton("X", skin);
        closeButton.setFillParent(true);
        topRightToolbar.add(closeButton);
        topSection.add(topRightToolbar).right();

        Table midSection = new Table(skin);

        Table midToolbar = new Table(skin);
        TextButton button3 = new TextButton("3", skin);
        TextButton button4 = new TextButton("4", skin);
        midToolbar.add(button3).width(50);
        midToolbar.row();
        midToolbar.add(button4).width(50);

        Window main1 = new Window("Hi", skin);
        Window main2 = new Window("Hello", skin);
        main1.setResizable(false);
        main2.setResizable(false);
        main1.setMovable(false);
        main2.setMovable(false);
        SplitPane midMain = new SplitPane(main1, main2, false, skin);

        midSection.add(midToolbar).width(50).top().expandY();
        midSection.add(midMain).right().expand().fill();

        Table lowerSection = new Table(skin);
        TextButton button5 = new TextButton("5", skin);
        TextButton button6 = new TextButton("6", skin);
        TextButton button7 = new TextButton("7", skin);
        TextButton button8 = new TextButton("8", skin);

        lowerSection.add(button5).width(50);
        lowerSection.add(button6).width(50);
        lowerSection.add(button7).width(50);
        lowerSection.add(button8).width(50);

        mainContainer.setBackground("white");
        mainContainer.add(topSection).top().height(50).fillX();
        mainContainer.row();
        mainContainer.add(midSection).expand().fill();
        mainContainer.row();
        mainContainer.add(lowerSection).bottom().left().height(50).expandX();
    }
}
