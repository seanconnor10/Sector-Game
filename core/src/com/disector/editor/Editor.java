package com.disector.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

import com.disector.*;
import com.disector.editor.actions.EditAction;
import com.disector.inputrecorder.InputChainInterface;
import com.disector.inputrecorder.InputChainNode;
import com.disector.renderer.EditingSoftwareRenderer;

import java.util.Stack;

public class Editor implements I_AppFocus {
    static BitmapFont font = new BitmapFont( Gdx.files.local("assets/font/fira.fnt") );

    final Application app;
    final Array<Wall> walls;
    final Array<Sector> sectors;
    final Array<Material> materials;
    final ShapeRenderer shape;
    final SpriteBatch batch;

    final InputChainInterface input;

    final NewEditorMapRenderer mapRenderer;
    final EditingSoftwareRenderer viewRenderer;

    final int MAX_RENDER_WIDTH = 400;
    final int MAX_RENDER_HEIGHT = 225;
    static final int MENU_BAR_HEIGHT = 32;

    final MapPanel mapPanel;
    final ViewPanel viewPanel;
    final MenuPanel menuPanel;
    final PropertiesPanel propertiesPanel;

    final Panel[] panels;

    Layouts layout = Layouts.DEFAULT;
    Button clickedButton = null;
    Panel focusedPanel;

    final Stack<EditAction> undoStack = new Stack<>();

    final EditorMessageLog messageLog = new EditorMessageLog();
    Panel logPanel;

    final ActiveSelection selection;

    EditorState state;
    float mouseX, mouseY;
    private int width, height;
    public boolean shouldUpdateViewRenderer = true;
    boolean isGridSnapping = true;
    int gridSize = 32;

    private float animationCycle = 0f;
    float animationFactor = 0f;

    public Editor(Application app, InputChainInterface input) {
        this.app = app;
        this.walls = app.walls;
        this.sectors = app.sectors;
        this.materials = app.materials;
        this.shape = app.shape;
        this.batch = app.batch;

        this.input = new InputChainNode(input, "Editor") {
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    selection.clear();
                    return true;
                }
                return super.keyDown(keycode);
            }
        };

        mapPanel        = new MapPanel(this);
        viewPanel       = new ViewPanel(this);
        menuPanel       = new MenuPanel(this);
        propertiesPanel = new PropertiesPanel(this);

        logPanel = mapPanel;
        focusedPanel = mapPanel;
        panels = new Panel[] { mapPanel, viewPanel, menuPanel, propertiesPanel };

        this.mapRenderer = new NewEditorMapRenderer(app, this, mapPanel.rect);
        this.viewRenderer = new EditingSoftwareRenderer(app);
        this.viewRenderer.placeCamera(100, 30, -(float)Math.PI/4f);
        this.viewRenderer.camZ = 20;
        this.selection = new ActiveSelection(sectors, walls, this);

        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        for (Panel panel : panels)
            panel.rearrangeButtons();
    }

    @Override
    public InputChainInterface getInputReference() {
        return input;
    }

    // -----------------------------------------------

    public void step(float dt) {
        messageLog.stepLifeTime(dt);

        cycleAnimation(dt);

        updateMouse();

        stepStateObject();

        updatePanel(dt);    //Update Panel before StateObject so the StateObject's onClick() isn't
                            //called immediately if clicking created the StateObject?
                            //Not anymore I guess...
        callMouseClickActions();

        temporaryControls(dt);

        updateRenderers();

    }

    public void draw() {
        //Draw Backgrounds of Panels and Buttons
        shape.begin(ShapeRenderer.ShapeType.Filled);
        Color col = new Color(0x100508FF);
        //Color col = new Color(0x050608FF);
        for (Panel panel : panels) {
            col = col.mul(1.5f);
            shape.setColor(col);
            drawRect(panel.rect);
            for (Button button : panel.buttons) {
                if (!button.active) continue;
                shape.setColor(button.pressed ? Color.MAROON : Color.TEAL);
                drawRect(offsetRectBy(button.rect, panel.rect));
            }
        }

        //Switch from shapeRenderer to SpriteBatch
        shape.end();
        batch.begin();

        //Draw Renderers' frames
        TextureRegion viewTex = viewRenderer.copyPixels();
        drawFrameBuffer(mapPanel.rect, new TextureRegion(mapRenderer.frame.getColorBufferTexture()));
        drawFrameBuffer(viewPanel.rect, viewTex);
        drawFrameBuffer(propertiesPanel.rect, new TextureRegion(propertiesPanel.frame.getColorBufferTexture()));
        drawFrameBuffer(propertiesPanel.rect, new TextureRegion(propertiesPanel.frame.getColorBufferTexture()));

        //Draw Button Text
        for (Panel panel : panels) {
            for (Button button : panel.buttons) {
                if (!button.active) continue;
                font.setColor(button.pressed ? Color.YELLOW : Color.GREEN);
                font.draw(batch, button.text, button.rect.x + panel.rect.x+4, button.rect.y + panel.rect.y + font.getCapHeight()+4);
            }
        }

        //DrawLogText
        drawLog();

        //Draw Name of active state if any
        drawStateName();

        //Draw Current Highlighted Sector and Wall
        font.draw(batch, "" + selection.highlightedWallIndex,
            mapPanel.rect.x + mapPanel.rect.width - 48,
            mapPanel.rect.y + mapPanel.rect.height - 24
        );
        font.draw(batch, "" + selection.highlightedSectorIndex,
            mapPanel.rect.x + mapPanel.rect.width - 48,
            mapPanel.rect.y + mapPanel.rect.height - 72
        );

        //End Batch and clear frame Textures
        batch.end();
        viewTex.getTexture().dispose();

        //Draw Borders
        shape.setColor(Color.CORAL);
        shape.begin(ShapeRenderer.ShapeType.Line);
        for (Panel panel : panels) {
            shape.rect(panel.rect.x+1, panel.rect.y, panel.rect.width-1, panel.rect.height-1);
        }

        if (focusedPanel != null) {
            shape.setColor(Color.WHITE);
            shape.rect(focusedPanel.rect.x+1, focusedPanel.rect.y, focusedPanel.rect.width-1, focusedPanel.rect.height-1);
        }

        shape.end();
    }

    // -----------------------------------------------

    private void onMouseClick() {
        Panel panelClicked = getPanelUnderMouse();

        if (state != null && focusedPanel==mapPanel)
            state.click();
        if (state == null || !state.ignoreEditorClick )
            panelClicked.clickedIn();
    }

    private void onMouseRightClick() {
        if (state != null) {
            state.rightClick();
        }
    }

    private void onMouseRelease() {
        clickedButton.pressed = false;

        if (mouseIn(clickedButton)) {
            clickedButton.releaseAction.apply(null);
        }

        clickedButton = null;
    }

    private void updateMouse() {
        mouseX = Gdx.input.getX();
        mouseY = height - Gdx.input.getY();
    }

    private Panel getPanelUnderMouse() {
        for (Panel p : panels) {
            if (mouseIn(p)) {
                return p;
            }
        }
        return focusedPanel;
    }

    // -----------------------------------------------

    boolean mouseIn(Panel p) {
        return /*p != null &&*/ p.rect.contains(mouseX, mouseY);
    }

    boolean mouseIn(Button b) {
        return /*b != null && b.panelRect != null &&*/ new Rectangle(
                b.panelRect.x + b.rect.x,
                b.panelRect.y + b.rect.y,
                b.rect.width,
                b.rect.height
        ).contains(mouseX, mouseY);
    }

    // -----------------------------------------------

    public void resize(int w, int h) {
        width = w;
        height = h;

        switch (layout) {
            case SWAP:       resizeSWAP();       break;
            case RENDER:     resizeRENDER();     break;
            case MAP:        resizeMAP();        break;
            case PROPERTIES: resizePROPERTIES(); break;
            default:         resizeDEFAULT();    break;
        }

        int view3DviewWidth = Math.min( MAX_RENDER_WIDTH, Math.round(viewPanel.rect.width));
        int view3DviewHeight = Math.min( MAX_RENDER_HEIGHT, Math.round(viewPanel.rect.height));

        propertiesPanel.refreshPanelSize(propertiesPanel.rect);
        mapRenderer.refreshPanelSize(mapPanel.rect);
        viewRenderer.resizeFrame(view3DviewWidth, view3DviewHeight);

        mapRenderer.render();
        viewRenderer.renderWorld();
    }

    private void resizeDEFAULT() {
        menuPanel.rect.height = MENU_BAR_HEIGHT;
        menuPanel.rect.width = width;
        menuPanel.rect.x = 0;
        menuPanel.rect.y = height-menuPanel.rect.height;

        mapPanel.rect.height = height-menuPanel.rect.height;
        mapPanel.rect.width = (int) (width*0.6f);
        mapPanel.rect.x = width-mapPanel.rect.width;
        mapPanel.rect.y = 0;

        viewPanel.rect.height = (int) ( (height-menuPanel.rect.height)*0.5f);
        viewPanel.rect.width = mapPanel.rect.x;
        viewPanel.rect.x = 0;
        viewPanel.rect.y = viewPanel.rect.height;

        propertiesPanel.rect.height = viewPanel.rect.y;
        propertiesPanel.rect.width = mapPanel.rect.x;
        propertiesPanel.rect.x = 0;
        propertiesPanel.rect.y = 0;

        logPanel = mapPanel;
    }

    private void resizeSWAP() {
        menuPanel.rect.height = MENU_BAR_HEIGHT;
        menuPanel.rect.width = width;
        menuPanel.rect.x = 0;
        menuPanel.rect.y = height-menuPanel.rect.height;

        viewPanel.rect.height = height-menuPanel.rect.height;
        viewPanel.rect.width = (int) (width*0.6f);
        viewPanel.rect.x = width-viewPanel.rect.width;
        viewPanel.rect.y = 0;

        mapPanel.rect.height = (int) ( (height-menuPanel.rect.height)*0.5f);
        mapPanel.rect.width = viewPanel.rect.x;
        mapPanel.rect.x = 0;
        mapPanel.rect.y = mapPanel.rect.height;

        propertiesPanel.rect.height = mapPanel.rect.y;
        propertiesPanel.rect.width = viewPanel.rect.x;
        propertiesPanel.rect.x = 0;
        propertiesPanel.rect.y = 0;

        logPanel = viewPanel;
    }

    private void resizeRENDER() {
        menuPanel.rect.height = MENU_BAR_HEIGHT;
        menuPanel.rect.width = width;
        menuPanel.rect.x = 0;
        menuPanel.rect.y = height-menuPanel.rect.height;

        viewPanel.rect.x = 0;
        viewPanel.rect.y = 0;
        viewPanel.rect.width = width;
        viewPanel.rect.height = height - MENU_BAR_HEIGHT;

        propertiesPanel.rect.set(-1,-1,-1,-1);
        mapPanel.rect.set(-1,-1,-1,-1);

        logPanel = viewPanel;
    }

    private void resizeMAP() {
        menuPanel.rect.height = MENU_BAR_HEIGHT;
        menuPanel.rect.width = width;
        menuPanel.rect.x = 0;
        menuPanel.rect.y = height-menuPanel.rect.height;

        mapPanel.rect.height = height-menuPanel.rect.height;
        mapPanel.rect.width = (int) (width*0.75f);
        mapPanel.rect.x = width-mapPanel.rect.width;
        mapPanel.rect.y = 0;

        propertiesPanel.rect.height = mapPanel.rect.height;
        propertiesPanel.rect.width = mapPanel.rect.x;
        propertiesPanel.rect.x = 0;
        propertiesPanel.rect.y = 0;

        viewPanel.rect.set(-1, -1, -1, -1);

        logPanel = mapPanel;
    }

    private void resizePROPERTIES() {
        menuPanel.rect.height = MENU_BAR_HEIGHT;
        menuPanel.rect.width = width;
        menuPanel.rect.x = 0;
        menuPanel.rect.y = height-menuPanel.rect.height;

        propertiesPanel.rect.height = height-menuPanel.rect.height;
        propertiesPanel.rect.width = (int) (width*0.6f);
        propertiesPanel.rect.x = 0;
        propertiesPanel.rect.y = 0;

        viewPanel.rect.height = (int) ( (height-menuPanel.rect.height)*0.5f);
        viewPanel.rect.width = width - propertiesPanel.rect.width;
        viewPanel.rect.x = propertiesPanel.rect.width;
        viewPanel.rect.y = viewPanel.rect.height;

        mapPanel.rect.height = viewPanel.rect.y;
        mapPanel.rect.width = propertiesPanel.rect.width;
        mapPanel.rect.x = propertiesPanel.rect.width;
        mapPanel.rect.y = 0;

        logPanel = propertiesPanel;
    }

    private void cycleLayout() {
        int nextLayoutOrdinal = layout.ordinal() + 1;
        if (nextLayoutOrdinal == Layouts.values().length)
            nextLayoutOrdinal = 0;
        layout = Layouts.values()[nextLayoutOrdinal];
        resize(width, height);
    }

    private void cycleLayoutBackward() {
        int nextLayoutOrdinal = layout.ordinal() - 1;
        if (nextLayoutOrdinal < 0)
            nextLayoutOrdinal = Layouts.values().length-1;
        layout = Layouts.values()[nextLayoutOrdinal];
        resize(width, height);
    }

    // -----------------------------------------------

    private void drawRect(Rectangle rect) {
        shape.rect(rect.x, rect.y, rect.width, rect.height);
    }

    private void drawFrameBuffer(Rectangle rect, TextureRegion tex) {
        tex.flip(false, true);
        batch.draw(tex, rect.x, rect.y, rect.width, rect.height);
    }

    private void drawLog() {
        int logSize = messageLog.size();
        int lineSpace = (int) font.getLineHeight();

        for (int i=logSize-1; i>=0; i--) {
            //Draw Text-Shadow
            font.setColor(Color.TEAL);
            font.getColor().lerp(Color.CLEAR, (logSize-1-i) * (1.f/(logSize+1)) );
            //font.getColor().lerp(Color.CLEAR, messageLog.messageLifetime.get(i) / EditorMessageLog.MAX_LIFE );
            font.draw(batch, messageLog.get(i),
                    logPanel.rect.x+9,
                    logPanel.rect.y+logPanel.rect.height-15-lineSpace*(logSize-1-i)
            );
            //Draw Main-Text
            font.setColor(Color.GOLDENROD);
            font.getColor().lerp(Color.CLEAR, (logSize-1-i) * (1.f/(logSize+1)) );
            //font.getColor().lerp(Color.CLEAR, messageLog.messageLifetime.get(i) / EditorMessageLog.MAX_LIFE );
            font.draw(batch, messageLog.get(i),
                    logPanel.rect.x+12,
                    logPanel.rect.y+logPanel.rect.height-12-lineSpace*(logSize-1-i)
            );
        }
    }

    private void drawStateName() {
        int lineSpace = (int) font.getLineHeight();

        if (state == null)
            return;

        String text = state.visibleName;

        //Draw Text-Shadow
        font.setColor(Color.TEAL);
        font.draw(batch, text,
                logPanel.rect.x+9,
                logPanel.rect.y+lineSpace+4
        );
        //Draw Main-Text
        font.setColor(Color.GOLDENROD);
        font.draw(batch, text,
                logPanel.rect.x+12,
                logPanel.rect.y+lineSpace
        );
    }

    private Rectangle offsetRectBy(Rectangle rect, Rectangle offset) {
        return new Rectangle(offset.x + rect.x, offset.y+rect.y, rect.width, rect.height);
    }

    private void constrainMouseToRect(Rectangle rect) {
        if (rect.width < 0 || rect.height < 0)
            return;
        if (mouseX < rect.x)
            mouseX = rect.x;
        else if (mouseX > rect.x + rect.width)
            mouseX = rect.x + rect.width;
        if (mouseY < rect.y)
            mouseY = rect.y;
        else if (mouseY > rect.y + rect.height)
            mouseY = rect.y + rect.height;
        Gdx.input.setCursorPosition((int) mouseX, height - (int) mouseY);
    }

    // -----------------------------------------------

    private void stepStateObject() {
        if (state != null && state.shouldFinish) {
            state.finish();
            state = null;
        }
        if (state != null) state.step();
    }

    private void updateRenderers() {
        if (shouldUpdateViewRenderer) {
            viewRenderer.renderWorld();
            mapRenderer.viewCamPosition = new CameraMapDraw(
                (int) viewRenderer.camX,
                (int) viewRenderer.camY,
                viewRenderer.camR,
                viewRenderer.getDegFromFov() / 2.0f
            );
        }
        mapRenderer.render();
        propertiesPanel.render();
        shouldUpdateViewRenderer = false;
    }

    private void updatePanel(float dt) {
        focusedPanel.stepFocused(dt);

        for (Panel p : panels)
            p.step(dt);

        if (focusedPanel.isForcingMouseFocus) {
            constrainMouseToRect(focusedPanel.rect);
        } else {
            if (/*state == null &&*/ !Gdx.input.isButtonPressed(Input.Buttons.LEFT))
               setPanelFocus(getPanelUnderMouse());
        }
    }

    private void setPanelFocus(Panel newTarget) {
        focusedPanel.input.off();
        focusedPanel = newTarget;
        focusedPanel.input.on();
    }

    private void cycleAnimation(float dt) {
        animationCycle += 2*dt;
        while (animationCycle > Math.PI) {
            animationCycle -= (float) Math.PI;
        }
        animationFactor = (float) Math.sin(animationCycle);
    }

    private void callMouseClickActions() {
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT))
            onMouseClick();
        else if (clickedButton != null && !Gdx.input.isButtonPressed(Input.Buttons.LEFT))
            onMouseRelease();

        if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT))
            onMouseRightClick();
    }

    private void temporaryControls(float dt) {
        final boolean CTRL = input.isDown(Input.Keys.CONTROL_LEFT);

        if (focusedPanel == mapPanel)
            moveMapWithKeyBoard(dt);
        else if (focusedPanel == viewPanel)
            moveViewWithKeyBoard(dt);

        //Cycle Layout
        if (CTRL && input.isJustPressed(Input.Keys.TAB)) {
            if (input.isDown(Input.Keys.SHIFT_LEFT))
                cycleLayoutBackward();
            else
                cycleLayout();
        }

        //Temporary Toggle FullBright
        if (CTRL && input.isJustPressed(Input.Keys.I)) {
            viewRenderer.fullBright = !viewRenderer.fullBright;
            messageLog.log("Full-Bright " + (viewRenderer.fullBright ? "ON" : "OFF"));
            shouldUpdateViewRenderer = true;
        }
         if (input.isJustPressed(Input.Keys.U)) {
            viewRenderer.drawFog = !viewRenderer.drawFog;
            messageLog.log("Distance-Fog " + (viewRenderer.drawFog ? "ON" : "OFF"));
            shouldUpdateViewRenderer = true;
        }

        //GridSize
        if (input.isJustPressed(Input.Keys.G)) {
            if (CTRL) {
                isGridSnapping = !isGridSnapping;
                messageLog.log("Snapping " + (isGridSnapping ? "Enabled" : "Disabled"));
            } else {
                if (!input.isDown(Input.Keys.SHIFT_LEFT))
                    gridSize /= 2;
                else
                    gridSize *= 2;
                if (gridSize < 4)
                    gridSize = 4;
                messageLog.log("GridSize " + gridSize);
            }
        }
    }

    private void moveMapWithKeyBoard(float dt) {
        boolean shift = input.isDown(Input.Keys.SHIFT_LEFT);
        float speed = shift ? 1000 : 300;

        //Temporary Movement
        float mapZoom = mapRenderer.zoom;
        if (input.isDown(Input.Keys.W)) {
            mapRenderer.camY += speed * dt / (float)Math.sqrt(mapZoom);
        }
        if (input.isDown(Input.Keys.D)) {
            mapRenderer.camX += speed * dt / (float)Math.sqrt(mapZoom);
        }
        if (input.isDown(Input.Keys.S)) {
            mapRenderer.camY -= speed * dt / (float)Math.sqrt(mapZoom);
        }
        if (input.isDown(Input.Keys.A)) {
            mapRenderer.camX -= speed * dt / (float)Math.sqrt(mapZoom);
        }

        //Temporary Zoom
        float amount = mapZoom < 1 ? 0.5f : mapZoom < 4 ? 2.5f : 10;
        amount *= dt;
        if (input.isDown(Input.Keys.EQUALS)) mapZoom += amount;
        if (input.isDown(Input.Keys.MINUS)) mapZoom -= amount;
        if (mapZoom < 0.1f) mapZoom = 0.1f;
        if (mapZoom > 20) mapZoom = 20;

        mapRenderer.zoom = mapZoom;
    }

    private void moveViewWithKeyBoard(float dt) {
        boolean shift = input.isDown(Input.Keys.SHIFT_LEFT);

        //Looking
        if (input.isDown(Input.Keys.UP)) {
            viewRenderer.camVLook += 200 * dt;
            shouldUpdateViewRenderer = true;
        }
        if (input.isDown(Input.Keys.DOWN)) {
            viewRenderer.camVLook -= 200 * dt;
            shouldUpdateViewRenderer = true;
        }
        if (input.isDown(Input.Keys.LEFT)) {
            viewRenderer.camR += 2*dt;
            shouldUpdateViewRenderer = true;
        }
        if (input.isDown(Input.Keys.RIGHT)) {
            viewRenderer.camR -= 2*dt;
            shouldUpdateViewRenderer = true;
        }

        //Moving
        float moveDist = 100*dt;
        if (shift) moveDist*=3;

        if (input.isDown(Input.Keys.W)) {
            viewRenderer.camX += (float) Math.cos(viewRenderer.camR) * moveDist;
            viewRenderer.camY += (float) Math.sin(viewRenderer.camR) * moveDist;
            shouldUpdateViewRenderer = true;
        }
        if (input.isDown(Input.Keys.S)) {
            viewRenderer.camX -= (float) Math.cos(viewRenderer.camR) * moveDist;
            viewRenderer.camY -= (float) Math.sin(viewRenderer.camR) * moveDist;
            shouldUpdateViewRenderer = true;
        }
        if (input.isDown(Input.Keys.A)) {
            viewRenderer.camX += (float) Math.cos(viewRenderer.camR + Math.PI/2) * moveDist;
            viewRenderer.camY += (float) Math.sin(viewRenderer.camR + Math.PI/2) * moveDist;
            shouldUpdateViewRenderer = true;
        }
        if (input.isDown(Input.Keys.D)) {
            viewRenderer.camX -= (float) Math.cos(viewRenderer.camR + Math.PI/2) * moveDist;
            viewRenderer.camY -= (float) Math.sin(viewRenderer.camR + Math.PI/2) * moveDist;
            shouldUpdateViewRenderer = true;
        }
        if (input.isDown(Input.Keys.E)) {
            viewRenderer.camZ += (shift ? 200 : 80) * dt;
            shouldUpdateViewRenderer = true;
        }
        if (input.isDown(Input.Keys.Q)) {
            viewRenderer.camZ -= (shift ? 200 : 80) * dt;
            shouldUpdateViewRenderer = true;
        }

        if (shouldUpdateViewRenderer) {
            viewRenderer.camCurrentSector = Physics.findCurrentSectorBranching(
                    viewRenderer.camCurrentSector,
                    viewRenderer.camX,
                    viewRenderer.camY
            );
        }
         
        //Temporary Zoom
        if (input.isDown(Input.Keys.EQUALS)) {
            viewRenderer.camFOV *= 1 + dt;
            shouldUpdateViewRenderer = true;
        }
        if (input.isDown(Input.Keys.MINUS)) {
            viewRenderer.camFOV *= 1 - Math.min(1, dt);
            shouldUpdateViewRenderer = true;
        }
        if (viewRenderer.camFOV < 50) viewRenderer.camFOV = 50;
        if (viewRenderer.camFOV > 1000) viewRenderer.camFOV = 1000;

    }

    // -----------------------------------------------

    void loadMap(String path) {
        //For the Load Menu bar button
        if (app.loadMap(path)) {
            messageLog.log("Loaded from " + path);
            shouldUpdateViewRenderer = true;
        } else
            messageLog.log("FAILED TO LOAD " + path);
    }

    void placeViewCamera(float x, float y) {
        viewRenderer.placeCamera(x, y);
        viewRenderer.camCurrentSector = Physics.findCurrentSectorBranching(
                viewRenderer.camCurrentSector,
                viewRenderer.camX,
                viewRenderer.camY
        );
        shouldUpdateViewRenderer = true;
    }

    int snap(int val) {
        return snap( (float) val );
    }

    int snap(float val) {
        return Math.round(val/(float)gridSize) * gridSize;
    }

    public void onMapLoad() {
        propertiesPanel.stage.onMapLoad();
    }

    private boolean wallsConnectOneWay(Wall a, Wall b) {
        return a.x2 == b.x1 && a.y2 == b.y1;
    }

    void joinSelectionAsPortals() {
        //Maybe make this be able to join multiple segments...?
        if (selection.selectedWalls.size != 2)
            return;

        int wInd1 = selection.wallIndices.get(0);
        int wInd2 = selection.wallIndices.get(1);
        Wall wall1 = walls.get(wInd1);
        Wall wall2 = walls.get(wInd2);

        if (wall1.isPortal || wall2.isPortal)
            return;

        int secInd1 = -1, secInd2 =-1;
        Sector sec1 = null, sec2 = null;

        for (int i=0; i<sectors.size; i++){
            Sector s = sectors.get(i);
            if (s.walls.contains(wInd1)) {
                secInd1 = i;
                sec1 = s;
            } else if (s.walls.contains(wInd2)) {
                secInd2 = i;
                sec2 = s;
            }
            if (secInd1 != -1 && secInd2 != -1)
                break;
        }

        if (secInd1 == -1 || secInd2 == -1)
            return;

        int newSectorIndex = sectors.size;
        Sector newSector = new Sector(sec1, false);

        Wall newWallOne = new Wall(wall1);
        newWallOne.x1 = wall1.x1;
        newWallOne.y1 = wall1.y1;
        newWallOne.x2 = wall2.x2;
        newWallOne.y2 = wall2.y2;
        newWallOne.setNormalAngle();
        newSector.walls.add(walls.size);
        walls.add(newWallOne);

        Wall newWallTwo = new Wall(wall1);
        newWallTwo.x1 = wall2.x1;
        newWallTwo.y1 = wall2.y1;
        newWallTwo.x2 = wall1.x2;
        newWallTwo.y2 = wall1.y2;
        newWallTwo.setNormalAngle();
        newSector.walls.add(walls.size);
        walls.add(newWallTwo);

        newSector.walls.add(wInd1);
        newSector.walls.add(wInd2);

        wall1.isPortal = true;
        wall1.linkA = newSectorIndex;
        wall1.linkB = secInd1;

        wall2.isPortal = true;
        wall2.linkA = newSectorIndex;
        wall2.linkB = secInd2;

        sectors.add(newSector);

        selection.clearWalls();
    }

}