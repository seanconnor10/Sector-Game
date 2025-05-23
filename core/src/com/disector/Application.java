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
import com.disector.assets.SoundManager;
import com.disector.console.CommandExecutor;
import com.disector.console.Console;
import com.disector.editor.EditorInterface;
import com.disector.editor.Editor1;
import com.disector.editor.Editor2;
import com.disector.gameworld.GameWorld;
import com.disector.inputrecorder.InputChainInterface;
import com.disector.inputrecorder.InputChainNode;
import com.disector.inputrecorder.InputRecorder;
import com.disector.maploader.OldTextFormatMapLoader;
import com.disector.renderer.DimensionalRenderer;
import com.disector.renderer.GameMapRenderer;
import com.disector.renderer.SoftwareRenderer;
import com.disector.maploader.MapLoader;
import com.disector.maploader.TextFileMapLoader;

import java.util.Arrays;

public class Application extends ApplicationAdapter {
    public static String[] CLI_ARGS;
    public static Config config;

    private boolean printFPS;
    private boolean vsyncEnabled;

    public GameWorld gameWorld;

    private DimensionalRenderer renderer;
    private GameMapRenderer gameMapRenderer;
    private EditorInterface editor;
    public FileHandle activeMapFile;

    private Console console;

    private AppFocusTarget focus;

    private float deltaTime;

    public final Array<Wall> walls = new Array<>();
    public final Array<Sector> sectors = new Array<>();
    public final Array<Material> materials = new Array<>();
    public static Array<Material> materialStaticReference;

    public PixmapContainer textures;

    public Pixmap.Format pixelFormat;
    public static String paletteLocation;

    public int frameWidth = 400;  //Actual default in Config class
    public int frameHeight = 225;

    public SpriteBatch batch;
    public ShapeRenderer shape;

    public InputRecorder mainInput = new InputRecorder();
    public InputChainNode consoleInput;
    public InputChainInterface appInput; //Now a reference to the individual InputChainNode of each AppFocus

    public Application(String[] args) {
        CLI_ARGS = args;
    }

    @Override
    public void create () {
        System.out.println("Setting things up...");

        Arrays.stream(CLI_ARGS).forEach(System.out::println);

        long timeStamp = TimeUtils.millis();

        focus = AppFocusTarget.GAME;

        loadConfig("disector.config");

        Physics.walls = walls;
        Physics.sectors = sectors;

        batch = new SpriteBatch();
        shape = new ShapeRenderer();
        shape.setColor(Color.WHITE);

        textures = new PixmapContainer();

        InputRecorder.repopulateKeyCodeMap();
        Gdx.input.setInputProcessor(mainInput);
        Gdx.input.setCursorCatched(true);

        console = new Console( new CommandExecutor(this), mainInput);
        consoleInput = console.getInputAdapter();

        createTestMap();
        createTestMaterial();

        SoundManager.init();

        if (gameWorld==null) gameWorld = new GameWorld(this, mainInput);
        if (renderer==null) renderer = new SoftwareRenderer(this);
        if (gameMapRenderer==null) gameMapRenderer = new GameMapRenderer(this, gameWorld);

        materialStaticReference = materials;

        swapFocus(focus);

        System.out.println("Engine Booted in " + TimeUtils.timeSinceMillis(timeStamp) + "ms");

        if (config.startMap != null) {
            loadMap(config.startMap);
        }

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
            gameWorld.beforeMapLoad();
            mapLoader.load(filePath);
            gameWorld.afterMapLoad();
            success = true;
            gameWorld.refreshPlayerSectorIndex();
            float newFloorZ = sectors.get(gameWorld.getPlayerSectorIndex()).floorZ;
            if (gameWorld.getPlayerEyesPosition().z < newFloorZ) {
                gameWorld.player1.pos.z = newFloorZ;
            }
            if (editor != null) {
                //editor.shouldUpdateViewRenderer = true;
                //editor.onMapLoad();
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
            //if (editor != null) editor.shouldUpdateViewRenderer = true;
            return true;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return  false;
        }
    }

    private void loadConfig(String filePath) {
        try {
            config = new Config(Gdx.files.local(filePath));
            printFPS = config.printFps;
            vsyncEnabled = config.vsync;
            Gdx.graphics.setVSync(vsyncEnabled);
            frameWidth = config.frameWidth;
            frameHeight = config.frameHeight;
            pixelFormat = config.use32bitColor ? Pixmap.Format.RGBA8888 : Pixmap.Format.RGBA4444;
            paletteLocation = config.palette;
        } catch (Exception e) {
            System.out.println("Error Loading Config... Defaulting");
            config = new Config();
        }
    }

    // --------------------------------------------------------

    public void swapFocus(AppFocusTarget target) {

        if (appInput != null)
            appInput.off();

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
                appInput = gameWorld.getInputReference();
                break;
            case MENU:
                //appInput = ...
                break;
            case EDITOR:
                if (gameWorld == null) {
                    System.out.println("Must instance GameWorld before Editor.");
                    break;
                }
                if (editor==null) editor = new Editor2(this, mainInput);
                appInput = editor.getInputReference();
                break;
            default:
        }

        if (appInput != null && ( console == null || !console.isActive() ))
            appInput.on();

        focus = target;
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

    }

    private void game() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) //Toggle Mouse Locking
            Gdx.input.setCursorCatched( !Gdx.input.isCursorCatched() );

        gameWorld.step(deltaTime);

        SoundManager.ear_pos.set(gameWorld.getPlayerEyesPosition());
        SoundManager.update(deltaTime);

        if (renderer.camFOV != renderer.baseFOV * gameWorld.player1.zoom) {
            renderer.camFOV = renderer.baseFOV * gameWorld.player1.zoom;
        }

        if (renderer instanceof SoftwareRenderer) {
            ((SoftwareRenderer) renderer).addSpriteList(gameWorld.getSpriteList());
        }

        float vLook = (float) Math.tan(Math.toRadians(gameWorld.getPlayerVAngle())) * renderer.camFOV;
        renderer.placeCamera(gameWorld.getPlayerEyesPosition(), vLook, gameWorld.getPlayerSectorIndex());
        renderer.renderWorld();
        renderer.drawFrame();

        if (renderer instanceof SoftwareRenderer) {
            ((SoftwareRenderer) renderer).clearSprites();
        }

        if (gameWorld.shouldDisplayMap()) {
            gameMapRenderer.placeCamera(gameWorld.getPlayerEyesPosition(), 0, gameWorld.getPlayerSectorIndex());
            gameMapRenderer.renderWorld();
            gameMapRenderer.drawFrame();
        }

//        Fov Angle Experiments
//        if (false) {
//            Vector4 pPos = gameWorld.getPlayerEyesPosition();
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
            boolean consoleOn = console.toggle();
            if (consoleOn) {
                consoleInput.on();
                if (appInput != null) appInput.off();
            } else {
                consoleInput.off();;
                if (appInput != null) appInput.on();
            }
        }

    }

    // --------------------------------------------------------

    private void createTestMap() {
        walls.clear(); sectors.clear();

        Sector s = new Sector(); s.floorZ = 0; s.ceilZ = 50;
        walls.add(new Wall( 0, 0, 0, 128      )); s.walls.add(walls.size-1);
        walls.add(new Wall( 0, 128, 128, 128    )); s.walls.add(walls.size-1);
        walls.add(new Wall( 128, 128, 128, 0    )); s.walls.add(walls.size-1);
        walls.add(new Wall( 128, 0, 0, 0      )); s.walls.add(walls.size-1);

        sectors.add(s);

    }

    private void createTestMaterial() {
        materials.clear();
        materials.add(new Material());
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
        if (appInput == editor.getInputReference())
            appInput = null;
        editor = null;
    }

    public void setRenderFov(int fov) {
        if (renderer != null)
            renderer.setFov(fov);
    }

    public void swapEditor() {
        if (editor == null) return;
        if (editor instanceof Editor1) {
            editor = new Editor2(this, mainInput);
        } else {
            editor = new Editor1(this, mainInput);
        }
        if (focus == AppFocusTarget.EDITOR) {
            appInput = editor.getInputReference();
        }
    }
}
