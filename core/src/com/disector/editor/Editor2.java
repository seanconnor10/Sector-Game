package com.disector.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import com.disector.*;
import com.disector.inputrecorder.InputChainInterface;
import com.disector.inputrecorder.InputChainNode;
import com.disector.inputrecorder.InputChainStage;
import com.disector.inputrecorder.InputRecorder;
import com.disector.renderer.EditingSoftwareRenderer;

import java.util.ArrayList;
import java.util.List;

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

    private Table materialsPanel;
    private List<MaterialBlock> materialBlocks = new ArrayList<>();

    public ActiveSelection activeSelection;

    public EditingSoftwareRenderer viewRenderer;
    Table viewPanel;

    public EditorMode mode = EditorMode.NORMAL;
    public enum EditorMode {
        NORMAL, VIEW_MOVEMENT;
    }

    public Editor2(Application app, InputChainInterface inputParent) {
        this.app = app;
        this.walls = app.walls;
        this.sectors = app.sectors;
        this.materials = app.materials;
        this.shape = app.shape;
        this.batch = app.batch;
        this.input = new InputChainNode(inputParent, "Editor2");

        this.activeSelection = new ActiveSelection(app.sectors, app.walls, this);

        this.viewRenderer = new EditingSoftwareRenderer(app, this);
        this.viewRenderer.placeCamera(100, 30, -(float) Math.PI / 4f);
        this.viewRenderer.camZ = 30f;
        viewRenderer.resizeFrame(400, 225);

        setupStage();
    }

    @Override
    public void step(float deltaTime) {
        Rectangle viewRect = getViewPanelRect();

        if (mode == EditorMode.VIEW_MOVEMENT) {
            //shouldUpdateViewRenderer = true;
            viewRenderer.camR -= InputRecorder.mouseDeltaX / 250;
            viewRenderer.camVLook -= InputRecorder.mouseDeltaY / 2;
            Gdx.input.setCursorPosition( (int) (viewRect.x + viewRect.width / 2), Gdx.graphics.getHeight() - (int) (viewRect.y + viewRect.height / 2) );
            if (!Gdx.input.isButtonPressed(Input.Buttons.MIDDLE)) {
                mode = EditorMode.NORMAL;
            }
        }

        if (Gdx.input.isButtonJustPressed(Input.Buttons.MIDDLE)) {
            if (mouseIn(viewRect)) {
                mode = EditorMode.VIEW_MOVEMENT;
                System.out.println("VIEW MOVE START");
            }
        }

        if(mouseIn(viewRect)) {
            moveViewWithKeyBoard(deltaTime);
        }
    }

    @Override
    public void draw() {
        //Let software renderer draw to buffer
        viewRenderer.renderWorld();

        //Space of view rectangle on screen
        Rectangle viewRect = getViewPanelRect();

        //If proportions of panel are different than the view's,
        //determine what portion of the buffer to draw without stretching it
        float panelAspectRatio = (float) viewRect.width / (float) viewRect.height;
        float viewAspectRatio = (float) viewRenderer.getWidth() / (float) viewRenderer.getHeight();
        float regionW, regionH;
        if (panelAspectRatio < viewAspectRatio) {
            regionW = panelAspectRatio / viewAspectRatio;
            regionH = 1.f;
        } else {
            regionW = 1.f;
            regionH =  viewAspectRatio / panelAspectRatio;
        }

        Texture viewTex = viewRenderer.copyPixelsAsTexture();

        Array<Texture> toDispose = new Array<>();

        batch.begin();
        ScreenUtils.clear(Color.GRAY);
        stage.act();
        stage.draw();

        //Draw 3D view
        batch.draw(viewTex, viewRect.x, viewRect.y, viewRect.width, viewRect.height,(int)(((1.f - regionW)*viewTex.getWidth())/2f), (int)(((1.f -regionH)*viewTex.getHeight())/2f), (int)(viewTex.getWidth()*regionW), (int)(viewTex.getHeight()*regionH), false, true);

        toDispose.addAll(drawMaterialsPanel(batch));

        batch.end();

        for (Texture t : toDispose) {
            t.dispose();
        }
        viewTex.dispose();
    }


    private Array<Texture> drawMaterialsPanel(SpriteBatch batch) {
        Array<Texture> toDispose = new Array<>();
        for (MaterialBlock e : materialBlocks) {
            toDispose.add(drawMaterialBlock(e, batch));
        }
        return toDispose;
    }

    private class MaterialBlock extends Table {
        Material mat;

    }

    private Texture drawMaterialBlock(MaterialBlock materialBlock, SpriteBatch batch) {
        //Material m = app.materials.get();
        Pixmap tex = materialBlock.mat.tex[0];

        Vector2 pos = materialBlock.localToScreenCoordinates(new Vector2());
        pos.y = Gdx.graphics.getHeight() - pos.y;
        Rectangle r = new Rectangle(pos.x, pos.y, materialBlock.getWidth(), materialBlock.getHeight());

        //Gen and store texture preferably...
        Texture toDispose = new Texture(tex);
        //batch.draw(toDispose, r.x, r.y, r.width, r.height);
        //batch.draw(toDispose, r.x, r.y, r.width, r.height);

        return toDispose;

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
                new TextureAtlas(Gdx.files.local("assets/skin/2/skin2.atlas"))
        );

        Color BG_COLOR = new Color(0.46f, 0.52f, 0.49f, 1.f);

        Table mainContainer = new Table(skin);
        mainContainer.setColor(BG_COLOR);
        mainContainer.setFillParent(true);
        stage.addActor(mainContainer);

        Table topSection = new Table(skin);
        Table topToolbar = new Table(skin);
        topToolbar.setBackground(skin.getDrawable("button-down-small"));
        TextButton button1 = new TextButton(" Save ", skin, "red");
        TextButton button2 = new TextButton(" Load ", skin, "red");
        TextButton button11 = new TextButton(" Play ", skin, "red");
        TextButton button22 = new TextButton(" Undo ", skin, "red");
        topToolbar.add(button1);
        topToolbar.add(button2);
        topToolbar.add(button11);
        topToolbar.add(button22);
        topSection.add(topToolbar).left().expand();
        Table topRightToolbar = new Table(skin);
        TextButton closeButton = new TextButton(" X ", skin, "red");
        closeButton.setFillParent(true);
        topRightToolbar.add(closeButton);
        topSection.add(topRightToolbar).right();

        Table midSection = new Table(skin);

        Table midToolbar = new Table(skin);
        TextButton button3 = new TextButton("3", skin, "red");
        TextButton button4 = new TextButton("4", skin, "red");
        midToolbar.add(button3).width(50);
        midToolbar.row();
        midToolbar.add(button4).width(50);

        Table main1 = new Table(skin);
        Table main2 = new Table(skin);
        Table main3 = new Table(skin);
        materialsPanel = setupMaterialsPanel();
        main1.setColor(BG_COLOR);
        main2.setColor(BG_COLOR);
        main3.setColor(BG_COLOR);
        materialsPanel.setColor(BG_COLOR);
        main1.setBackground("headerless_window");
        main2.setBackground("headerless_window");
        main3.setBackground("headerless_window");
        materialsPanel.setBackground("headerless_window");
        Table midRightContainer = new Table(skin);
        SplitPane midSecondarySplitR = new SplitPane(main2, main3, true, skin);
        SplitPane midSecondarySplitL = new SplitPane(materialsPanel, main1, true, skin);
        SplitPane midMainSplit = new SplitPane(midSecondarySplitL, midSecondarySplitR, false, skin);
        midMainSplit.setMinSplitAmount(0.1f);
        midSecondarySplitL.setMinSplitAmount(0.1f);
        midSecondarySplitR.setMinSplitAmount(0.1f);
        midMainSplit.setMaxSplitAmount(0.9f);
        midSecondarySplitR.setMaxSplitAmount(0.9f);
        midSecondarySplitL.setMaxSplitAmount(0.9f);
        midRightContainer.add(midMainSplit).fill().expand();

        midSection.add(midToolbar).width(50).top().expandY();
        midSection.add(midRightContainer).right().expand().fill();

        Table lowerSection = new Table(skin);
        TextButton button5 = new TextButton("5", skin, "red");
        TextButton button6 = new TextButton("6", skin, "red");
        TextButton button7 = new TextButton("7", skin, "red");
        TextButton button8 = new TextButton("8", skin, "red");

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

    private Table setupMaterialsPanel() {
        if (skin == null)
            return null;

        Table main = new Table(skin);

        int i=0;
        for (Material m : app.materials) {
            i++;
            MaterialBlock block = setupMaterialBlock(m);
            materialBlocks.add(block);
            main.add(block).width(96).height(96);
            if (i%5 == 0) {
                main.row();
            }
        }

        return main;
    }

    private MaterialBlock setupMaterialBlock(Material mat) {
        MaterialBlock block = new MaterialBlock();
        block.mat = mat;
        Image img = new Image(new Scene2DTexture(mat.tex[0]));
        block.add(img).width(96).height(96);
        return block;
    }

    private Rectangle getViewPanelRect() {
        Vector2 pos = viewPanel.localToStageCoordinates(new Vector2());
        return new Rectangle(
                pos.x,
                pos.y,
                viewPanel.getWidth(),
                viewPanel.getHeight()
        );
    }

    private boolean mouseIn(Rectangle rect) {
        int x = Gdx.input.getX();
        int y = Gdx.graphics.getHeight() - Gdx.input.getY();
        return x > rect.x && x < rect.x+rect.width && y > rect.y && y < rect.y+rect.height;
    }

    private void moveViewWithKeyBoard(float dt) {
        boolean shift = input.isDown(Input.Keys.SHIFT_LEFT);

        //Looking
        if (input.isDown(Input.Keys.UP)) {
            viewRenderer.camVLook += 200 * dt;
            //shouldUpdateViewRenderer = true;
        }
        if (input.isDown(Input.Keys.DOWN)) {
            viewRenderer.camVLook -= 200 * dt;
            //shouldUpdateViewRenderer = true;
        }
        if (input.isDown(Input.Keys.LEFT)) {
            viewRenderer.camR += 2*dt;
            //shouldUpdateViewRenderer = true;
        }
        if (input.isDown(Input.Keys.RIGHT)) {
            viewRenderer.camR -= 2*dt;
            //shouldUpdateViewRenderer = true;
        }

        //Moving
        float moveDist = 100*dt;
        if (shift) moveDist*=3;

        if (input.isDown(Input.Keys.W)) {
            viewRenderer.camX += (float) Math.cos(viewRenderer.camR) * moveDist;
            viewRenderer.camY += (float) Math.sin(viewRenderer.camR) * moveDist;
            //shouldUpdateViewRenderer = true;
        }
        if (input.isDown(Input.Keys.S)) {
            viewRenderer.camX -= (float) Math.cos(viewRenderer.camR) * moveDist;
            viewRenderer.camY -= (float) Math.sin(viewRenderer.camR) * moveDist;
            //shouldUpdateViewRenderer = true;
        }
        if (input.isDown(Input.Keys.A)) {
            viewRenderer.camX += (float) Math.cos(viewRenderer.camR + Math.PI/2) * moveDist;
            viewRenderer.camY += (float) Math.sin(viewRenderer.camR + Math.PI/2) * moveDist;
            //shouldUpdateViewRenderer = true;
        }
        if (input.isDown(Input.Keys.D)) {
            viewRenderer.camX -= (float) Math.cos(viewRenderer.camR + Math.PI/2) * moveDist;
            viewRenderer.camY -= (float) Math.sin(viewRenderer.camR + Math.PI/2) * moveDist;
            //shouldUpdateViewRenderer = true;
        }
        if (input.isDown(Input.Keys.E)) {
            viewRenderer.camZ += (shift ? 200 : 80) * dt;
            //shouldUpdateViewRenderer = true;
        }
        if (input.isDown(Input.Keys.Q)) {
            viewRenderer.camZ -= (shift ? 200 : 80) * dt;
            //shouldUpdateViewRenderer = true;
        }

        if (/*shouldUpdateViewRenderer*/ true) {
            viewRenderer.camCurrentSector = Physics.findCurrentSectorBranching(
                    viewRenderer.camCurrentSector,
                    viewRenderer.camX,
                    viewRenderer.camY
            );
        }

        //Temporary Zoom
        if (input.isDown(Input.Keys.EQUALS)) {
            viewRenderer.camFOV *= 1 + dt;
            //shouldUpdateViewRenderer = true;
        }
        if (input.isDown(Input.Keys.MINUS)) {
            viewRenderer.camFOV *= 1 - Math.min(1, dt);
            //shouldUpdateViewRenderer = true;
        }
        if (viewRenderer.camFOV < 50) viewRenderer.camFOV = 50;
        if (viewRenderer.camFOV > 1000) viewRenderer.camFOV = 1000;

    }
}
