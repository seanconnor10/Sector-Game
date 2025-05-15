package com.disector.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector4;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Array;

import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.disector.*;
import com.disector.inputrecorder.InputChainInterface;
import com.disector.inputrecorder.InputChainNode;
import com.disector.inputrecorder.InputChainStage;
import com.disector.renderer.EditingSoftwareRenderer;

public class Editor2 implements EditorInterface {
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

    public ActiveSelection activeSelection;

    public EditingSoftwareRenderer viewRenderer;
    Table viewPanel;

    public Editor2(Application app,InputChainInterface inputParent) {
        this.app = app;
        this.walls = app.walls;
        this.sectors = app.sectors;
        this.materials = app.materials;
        this.shape = app.shape;
        this.batch = app.batch;
        this.input = new InputChainNode(inputParent, "Editor2");

        this.activeSelection = new ActiveSelection(app.sectors, app.walls, this);

        this.viewRenderer = new EditingSoftwareRenderer(app, this);
        this.viewRenderer.placeCamera(100, 30, -(float)Math.PI/4f);
        this.viewRenderer.camZ = 30f;
        viewRenderer.resizeFrame(300, 200);

        setupStage();
    }

    @Override
    public void step(float deltaTime) {

    }

    @Override
    public void draw() {
        int viewX, viewY, viewW, viewH;
        viewX = (int) viewPanel.getOriginX()+50;
        viewY = (int) viewPanel.getOriginY()+50;
        viewW = (int) viewPanel.getWidth();
        viewH = (int) viewPanel.getHeight();

        viewRenderer.renderWorld();

        Texture viewTex = viewRenderer.copyPixelsAsTexture();

        batch.begin();
        ScreenUtils.clear(Color.GRAY);
        stage.act();
        stage.draw();
        batch.draw(viewTex, viewX, viewY, viewW, viewH, 0, 0, viewTex.getWidth(), viewTex.getHeight(), false, true);
        batch.end();

        viewTex.dispose();
    }

    @Override
    public void resize(int w, int h) {
        stage.getViewport().update(Math.max(w, 1), Math.max(h, 1), true);
    }

    @Override
    public ActiveSelection getSelection() {
        return activeSelection;
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

        Table main1 = new Table(skin);
        Table main2 = new Table(skin);
        Table main3 = new Table(skin);
        main1.setBackground("headerless_window");
        main2.setBackground("headerless_window");
        main3.setBackground("headerless_window");
        Table midMainContainer = new Table(skin);
        SplitPane midSecondarySplit = new SplitPane(main2, main3, true, skin);
        SplitPane midMainSplit = new SplitPane(main1, midSecondarySplit, false, skin);
        midMainSplit.setMinSplitAmount(0.1f);
        midSecondarySplit.setMinSplitAmount(0.1f);
        midMainSplit.setMaxSplitAmount(0.9f);
        midSecondarySplit.setMaxSplitAmount(0.9f);
        //midMain.setStyle(skin.get("c-horizontal", SplitPane.SplitPaneStyle.class));
        midMainContainer.add(midMainSplit).fill().expand();

        midSection.add(midToolbar).width(50).top().expandY();
        midSection.add(midMainContainer).right().expand().fill();

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

        viewPanel = main1;
    }
}
