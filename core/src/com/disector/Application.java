package com.disector;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.TimeUtils;

import com.disector.Config.Config;
import com.disector.assets.PixmapContainer;
import com.disector.console.CommandExecutor;
import com.disector.console.Console;
import com.disector.editor.Editor;
import com.disector.gameworld.GameWorld;
import com.disector.inputrecorder.InputChainNode;
import com.disector.inputrecorder.InputRecorder;
import com.disector.maploader.OldTextFormatMapLoader;
import com.disector.renderer.DimensionalRenderer;
import com.disector.renderer.GameMapRenderer;
import com.disector.renderer.SoftwareRenderer;
import com.disector.maploader.MapLoader;
import com.disector.maploader.TextFileMapLoader;

public class Application extends ApplicationAdapter {
    public static Config config;

    private boolean printFPS;
    private boolean vsyncEnabled;

    public GameWorld gameWorld;

    private DimensionalRenderer renderer;
    private GameMapRenderer gameMapRenderer;
    private Editor editor;
    public FileHandle activeMapFile;

    private Console console;

    private AppFocusTarget focus;

    private float deltaTime;

    public final Array<Wall> walls = new Array<>();
    public final Array<Sector> sectors = new Array<>();
    public final Array<Material> materials = new Array<>();

    public PixmapContainer textures;

    public Pixmap.Format pixelFormat;

    public int frameWidth = 400;  //Actual default in Config class
    public int frameHeight = 225;

    public SpriteBatch batch;
    public ShapeRenderer shape;

    public InputRecorder mainInput = new InputRecorder();
    public InputChainNode consoleInput;
    public InputChainNode appInput; //Given to the AppFocusTarget (i.e. Game, Editor, Menu)

    @Override
    public void create () {
        System.out.println("Setting things up...");

        long timeStamp = TimeUtils.millis();

        focus = AppFocusTarget.GAME;

        loadConfig("disector.config");

        Physics.walls = walls;
        Physics.sectors = sectors;

        batch = new SpriteBatch();
        shape = new ShapeRenderer();
        shape.setColor(Color.WHITE);

        textures = new PixmapContainer();
        textures.loadFolder("");

        InputRecorder.repopulateKeyCodeMap();
        Gdx.input.setInputProcessor(mainInput);
        Gdx.input.setCursorCatched(true);

        console = new Console( new CommandExecutor(this), mainInput);

        consoleInput = console.getInputAdapter();
        appInput = new InputChainNode(mainInput, "AppFocusInput");
        appInput.on();

        createTestMap();
        createTestMaterial();

        if (gameWorld==null) gameWorld = new GameWorld(this, appInput);
        if (renderer==null) renderer = new SoftwareRenderer(this);
        if (gameMapRenderer==null) gameMapRenderer = new GameMapRenderer(this, gameWorld);

        swapFocus(focus);

        System.out.println("Engine Booted in " + TimeUtils.timeSinceMillis(timeStamp) + "ms");
    }

    @Override
    public void render () {
        updateDeltaTime();

        InputRecorder.updateKeys();

        functionKeyInputs();

        //Run Screen
        switch(focus) {
            case MENU: menu(); break;
            case GAME: game(); break;
            case EDITOR:
                if (editor != null) editor();
                    else ScreenUtils.clear(Color.SLATE);
                break;
            default: break;
        }

        console.updateAndDraw(deltaTime);
    }

    @Override
    public void dispose () {
        batch.dispose();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);

        batch.setProjectionMatrix(new Matrix4().setToOrtho2D(0,0, width, height));
        shape.setProjectionMatrix(batch.getProjectionMatrix());

        switch(focus) {
            case GAME:
                if (gameMapRenderer != null) gameMapRenderer.resizeFrame(width, height);
                break;
            case EDITOR:
                if (editor != null) editor.resize(width, height);
                break;
            case MENU:
                break;
            default:
        }

    }

    // --------------------------------------------------------

    public boolean loadMap(String filePath) {
        TextFileMapLoader mapLoader = new TextFileMapLoader(this);
        boolean success = false;
        try {
            mapLoader.load(filePath);
            success = true;
            gameWorld.refreshPlayerSectorIndex();
            float newFloorZ = sectors.get(gameWorld.getPlayerSectorIndex()).floorZ;
            if (gameWorld.getPlayerPosition().z < newFloorZ) {
                gameWorld.player1.setZ(newFloorZ);
            }
            if (editor != null) {
                editor.shouldUpdateViewRenderer = true;
                editor.onMapLoad();
            }
            activeMapFile = Gdx.files.local(filePath);
        } catch (Exception e) {
            System.out.println( "Error when loading map! " + e.getCause() + " " + e.getMessage() );
        }

        return success;
    }

    public boolean saveMap(String filePath) {
        //if (!filePath.contains(".")) filePath += ".txt";
        return new TextFileMapLoader(this).save(filePath);
    }

    public boolean loadMapOldFormat(String filePath) {
        MapLoader mapLoader = new OldTextFormatMapLoader(this);
        try {
            mapLoader.load(filePath);
            if (editor != null) editor.shouldUpdateViewRenderer = true;
            return true;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return  false;
        }
    }

    private void loadConfig(String filePath) {
        config = new Config( Gdx.files.local(filePath) );
        printFPS = config.printFps;
        vsyncEnabled = config.vsync; Gdx.graphics.setVSync(vsyncEnabled);
        frameWidth = config.frameWidth;
        frameHeight = config.frameHeight;
        pixelFormat = config.use32bitColor ? Pixmap.Format.RGBA8888 : Pixmap.Format.RGBA4444;
    }

    // --------------------------------------------------------

    public void swapFocus(AppFocusTarget target) {

        switch(focus) {
            case GAME:
                Gdx.input.setCursorCatched(false);
                break;
            case MENU:
                break;
            case EDITOR:
                break;
            default:
        }

        switch (target) {
            case GAME:
                if (gameWorld==null) gameWorld = new GameWorld(this, appInput);
                if (renderer==null) renderer = new SoftwareRenderer(this);
                if (gameMapRenderer==null) gameMapRenderer = new GameMapRenderer(this, gameWorld);
                Gdx.input.setCursorCatched(true);
                break;
            case MENU:
                break;
            case EDITOR:
                if (gameWorld == null) {
                    System.out.println("Must instance GameWorld before Editor.");
                    break;
                }
                if (editor==null) editor = new Editor(this, appInput);
                break;
            default:
        }

        focus = target;
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

    }

    private void game() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) //Toggle Mouse Locking
            Gdx.input.setCursorCatched( !Gdx.input.isCursorCatched() );

        gameWorld.step(deltaTime);

        renderer.placeCamera(gameWorld.getPlayerPosition(), gameWorld.getPlayerVLook(), gameWorld.getPlayerSectorIndex());
        renderer.renderWorld();
        renderer.drawFrame();

        /*if (renderer.screenHasEmptySpace()) {
            gameWorld.refreshPlayerSectorIndex();
        }*/

        if (gameWorld.shouldDisplayMap()) {
            gameMapRenderer.placeCamera(gameWorld.getPlayerPosition(), 0, gameWorld.getPlayerSectorIndex());
            gameMapRenderer.renderWorld();
            gameMapRenderer.drawFrame();
        }

//        Fov Angle Experiments
//        if (false) {
//            Vector4 pPos = gameWorld.getPlayerPosition();
//            float angleToPointOne = 90f + (float) (-(180 / Math.PI) * (Math.atan2(100 - pPos.x, 20 - pPos.y) + pPos.w));// + pPos.w; // w is player angle
//            float halfFrame = frameWidth / 2f;
//            float fov = renderer.getFov();
//            float angleLeftScreenEdge = (float) Math.atan(halfFrame / fov); //This is angle in rad
//            float horizonScreenDistVert = (frameHeight / 2.f) - renderer.camVLook;
//            float angleBottomScreenEdge = (float) Math.atan(horizonScreenDistVert / fov);
//            float distToFloorAtScreenBottom = (renderer.camZ /*minus Sector Floor Height*/) / (float) Math.sin(angleBottomScreenEdge);
//            // ^ This ^ is the one-dimensional-distance from viewPlace for floor row...
//            angleLeftScreenEdge *= (float) (180.0 / Math.PI);
//            //System.out.printf("AngToPoint2: %f\nArcCot:%f\nDist to floor %f\n\n", angleToPointOne, angleLeftScreenEdge, distToFloorAtScreenBottom);
//            System.out.println("Floor Dist Screen Bottom: " + distToFloorAtScreenBottom);
//        }
    }

    private void menu() {

    }

    private void editor(){
        editor.step(deltaTime);
        editor.draw();
    }

    // --------------------------------------------------------

    private void updateDeltaTime() {
        deltaTime = Gdx.graphics.getDeltaTime();
        if (printFPS) {
            int fps = (int) (1.f / deltaTime);
            System.out.println("Fps: " + fps);
        }
        if (deltaTime > 0.04f) deltaTime = 0.04f; //If below 25 frames/second only advance time as if it were running at 25fps
    }

    private void functionKeyInputs() {
        if (mainInput.isJustPressed(Input.Keys.F1)) {
            swapFocus(AppFocusTarget.GAME);
        } else if (mainInput.isJustPressed(Input.Keys.F2)) {
            if (mainInput.isJustPressed(Input.Keys.SHIFT_LEFT))
                editor = null;
            else
                swapFocus(AppFocusTarget.EDITOR);
        }

        if (mainInput.isJustPressed(Input.Keys.F4) ) {
			if (Gdx.graphics.isFullscreen())
				Gdx.graphics.setWindowedMode( frameWidth*2, frameHeight*2 );
			else
				Gdx.graphics.setFullscreenMode( Gdx.graphics.getDisplayMode() );
		}

        if (mainInput.isJustPressed(Input.Keys.GRAVE)) {
            console.toggle();
            appInput.toggle();
            consoleInput.toggle();
        }

    }

    // --------------------------------------------------------

    private void createTestMap() {
        walls.clear(); sectors.clear();

        Sector s = new Sector(); s.floorZ = 0; s.ceilZ = 50;
        walls.add(new Wall( 20, 20, 100, 20     )); s.walls.add(walls.size-1);
        walls.add(new Wall( 100, 20, 100, 80    )); s.walls.add(walls.size-1);
        Wall firstPortal = new Wall( 100, 80, 175, 80 );
            firstPortal.isPortal = true;
            firstPortal.linkA = 1;
            firstPortal.linkB = 0;
            walls.add(firstPortal);
            s.walls.add(walls.size-1);
        walls.add(new Wall( 175, 80, 175, -20   )); s.walls.add(walls.size-1);
        walls.add(new Wall( 175, -20, 75, -125  )); s.walls.add(walls.size-1);
        walls.add(new Wall( 75, -125, 20, -125  )); s.walls.add(walls.size-1);
        walls.add(new Wall( 20, -125, 20, -80   )); s.walls.add(walls.size-1);
        walls.add(new Wall( 20, -80, 50, -80    )); s.walls.add(walls.size-1);
        walls.add(new Wall( 50, -80, 50, -50    )); s.walls.add(walls.size-1);
        walls.add(new Wall( 50, -50, 20, 20     )); s.walls.add(walls.size-1);
        sectors.add(s);

        s = new Sector(); s.floorZ = -15; s.ceilZ = 40;
        s.walls.add(2); //Index 2 is First Portal
        walls.add(new Wall(100, 80, 90, 125 )); s.walls.add(walls.size-1);
        Wall secondPortal = new Wall(90, 125, 185, 125);
            secondPortal.isPortal = true;
            secondPortal.linkA = 2;
            secondPortal.linkB = 1;
            walls.add(secondPortal);
            s.walls.add(walls.size-1);
        walls.add(new Wall(185, 125,  175, 80 )); s.walls.add(walls.size-1);
        sectors.add(s);

        s = new Sector(); s.floorZ = -40; s.ceilZ = 20;
        s.walls.add(walls.size-2); //Add 'secondPortal'
        walls.add(new Wall( 90, 125, 90, 145    )); s.walls.add(walls.size-1);
        walls.add(new Wall( 90, 145, 135, 145   )); s.walls.add(walls.size-1);
        walls.add(new Wall( 135, 145, 135, 200   )); s.walls.add(walls.size-1);
        walls.add(new Wall( 135, 200, 150, 200   )); s.walls.add(walls.size-1);
        walls.add(new Wall( 150, 200, 150, 145   )); s.walls.add(walls.size-1);
        walls.add(new Wall( 150, 145, 185, 125  )); s.walls.add(walls.size-1);
        sectors.add(s);

    }

    private void createTestMaterial() {
        materials.clear();
        String texName = "WOOD2";
        materials.add(new Material( textures.get(texName), texName, false));
    }

    private void randomizeTextures() {
        final int lastIndex = materials.size-1;

        for (Sector s : sectors) {
            s.matCeil = (int) Math.round(Math.random()*lastIndex);
            s.matFloor = (int) Math.round(Math.random()*lastIndex);
        }

        for (Wall w : walls) {
            w.mat = (int) Math.round(Math.random()*lastIndex);
            w.matLower = (int) Math.round(Math.random()*lastIndex);
            w.matUpper = (int) Math.round(Math.random()*lastIndex);
        }
    }

    // ------ CONSOLE COMMANDS HELPERS --------------------------

    public void setGameRenderFrameSize(int w, int h) {
        renderer.resizeFrame(w, h);
    }

    public boolean toggleVsync() {
        vsyncEnabled = !vsyncEnabled;
        Gdx.graphics.setVSync(vsyncEnabled);
        return vsyncEnabled;
    }

    public void toggleEditor() {
        if (focus == AppFocusTarget.EDITOR && editor != null ) {
            swapFocus(AppFocusTarget.GAME);
        }
        else {
            swapFocus(AppFocusTarget.EDITOR);
        }
    }

    public void destroyEditor() {
        editor = null;
    }

    public void setRenderFov(int fov) {
        if (renderer != null)
            renderer.setFov(fov);
    }

}
