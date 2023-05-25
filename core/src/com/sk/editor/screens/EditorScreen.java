package com.sk.editor.screens;

import com.artemis.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.sk.editor.Editor;
import com.sk.editor.EditorManager;
import com.sk.editor.assets.AssetDescriptors;
import com.sk.editor.assets.RegionNames;
import com.sk.editor.config.Config;
import com.sk.editor.ecs.ECSManager;
import com.sk.editor.scripting.ScriptManager;
import com.sk.editor.ui.*;
import com.sk.editor.ui.console.Console;
import com.sk.editor.ui.content.ContentBrowser;
import com.sk.editor.ui.listeners.ResizeListener;
import com.sk.editor.ui.inspector.Inspector;
import com.sk.editor.ui.logger.EditorLogger;
import com.sk.editor.ui.hierarchy.Hierarchy;
import com.sk.editor.ecs.components.Image;
import com.sk.editor.ecs.components.Transform;

public class EditorScreen extends ScreenAdapter {

    private static final EditorLogger log = new EditorLogger(EditorScreen.class.toString(), Logger.DEBUG);

    private Editor editor;
    private AssetManager assets;
    private ShapeRenderer renderer;
    private SpriteBatch batch;
    private UIStage uiStage;
    private Stage ecsStage;
    private NotifyingOrthographicCamera ecsCamera;
    private Viewport uiViewport, ecsViewport;
    private Skin skin;
    private Texture repeatingGridTexture;
    private ECSManager ecsManager;
    private EditorManager editorManager;
    private InputManager inputManager;
    private ScriptManager scriptManager;

    private Inspector inspector;
    private Console console;
    private Hierarchy hierarchy;
    private ContentBrowser contentBrowser;

    public EditorScreen(Editor editor) {
        this.editor = editor;
        this.assets = editor.getAssetManager();
        this.renderer = editor.getShapeRenderer();
        this.batch = editor.getBatch();
        this.skin = editor.getSkin();

        this.editorManager = editor.getEditorManager();
    }

    @Override
    public void show() {
        init();
    }

    private void init() {
        initAssets();
        initStages();
        initConsole();
        initManagers();
        initActors();
    }

    private void initAssets() {
        TextureAtlas atlas = assets.get(AssetDescriptors.UI);

        // create scene background texture
        //gridTexture = new Texture(Gdx.files.internal("grid.png"));
        TextureRegion gridRegion = atlas.findRegion(RegionNames.GRID);
        repeatingGridTexture = createRepeatingTextureFromRegion(gridRegion);

    }

    private void initStages() {
        // create stages
        this.ecsCamera = new NotifyingOrthographicCamera();
        ecsViewport = new ScreenViewport(ecsCamera){
            @Override
            public void update(int screenWidth, int screenHeight, boolean centerCamera) {
                super.update(screenWidth, screenHeight, centerCamera);
            }
        };
        uiViewport = new ScreenViewport();

        ecsStage = new Stage(ecsViewport, batch);
        uiStage = new UIStage(uiViewport, batch);

        // set gdx input processor
        Gdx.input.setInputProcessor(
                new InputMultiplexer(uiStage, ecsStage));
    }


    private void initConsole(){
        console = new Console(skin);
        setupUIActor(console);
        console.setVisible(false);

        EditorLogger.addListener(console);


        console.setSize(512, 256);
        console.setPosition(uiStage.getWidth() / 2, uiStage.getHeight() /2, Align.center);
    }

    private void initManagers(){
        // create script manager before console and before ecs manager to compile and load
        // uses default project path
        String projectPath = editorManager.getPrefKeys().PROJECT_PATH.get();
        /*
        if(projectPath.isEmpty()){
            FileHandle projDir = Gdx.files.external(Config.DEFAULT_PROJECT_DIR);
            if(projDir.exists() == false)projDir.mkdirs();
            projectPath = projDir.file().getAbsolutePath();
            editorManager.getPrefKeys().PROJECT_PATH.set(projectPath);
        }*/
        //TODO: when using default path Gdx.files.external would be needed. fix smh
        scriptManager = new ScriptManager(Gdx.files.absolute(projectPath));
        //setupScriptManager(scriptManager);

        // ecs
        // TODO: ecs manager has to inject dependencies for script manager loaded classes on each recompile and load
        // TODO: ecs manager has to handle script manager loaded classes' annotations on each recompile and load
        ecsManager = new ECSManager(editor, uiStage, ecsViewport);
        ecsCamera.addCameraListener(ecsManager);

        // input
        inputManager = new InputManager(uiStage, ecsStage, ecsManager);
        inputManager.setFillParent(true);
        inputManager.setTouchable(Touchable.enabled);
    }

    @Deprecated
    private void setupScriptManager(ScriptManager manager){
        /*
        String projPath = editorManager.getPrefKeys().PROJECT_PATH.get();
        String srcPath =  editorManager.getPrefKeys().SRC_PATH.get();
        String classPath =  editorManager.getPrefKeys().CLASS_PATH.get();
        String packageName =  editorManager.getPrefKeys().PACKAGE_NAME.get();
        log.debug("-----------------------------------------------");

        try {
            if(!srcPath.isEmpty())manager.setSrcPath(Paths.get(srcPath));
            else log.info("src path not set yet.");
            if(!classPath.isEmpty())manager.setClassPath(Paths.get(classPath));
            else log.info("cls path not set yet.");
        } catch (InvalidPathException e){
            log.error("Reading invalid paths from prefs.", e);
        }
        if(!packageName.isEmpty())manager.setPackageName(packageName);
        else log.info("package name not set yet.");

        if(!srcPath.isEmpty() && !classPath.isEmpty() && !packageName.isEmpty()){
            try {
                if (manager.compileAndLoad()) {
                    manager.debugLoadedClasses();
                }
            } catch (Exception e) {
                log.error("Could not initially compile and load!", e);
            }
        }*/
    }

    private void initActors() {
        // -- ecs stage --

        // ecs world processor
        ECSProcessingActor ecsProcessingActor = new ECSProcessingActor(ecsManager);

        // create scene background
        GridBackgroundActor backgroundActor = new GridBackgroundActor(
                new TextureRegion(repeatingGridTexture), ecsViewport);
        backgroundActor.setTextureScaleFactor(32); //8
        backgroundActor.setFillViewport(true);

        ecsStage.addActor(backgroundActor);
        ecsStage.addActor(ecsProcessingActor);


        // -- ui stage --
        uiStage.addUIActor(inputManager, inputManager.getClass()); // else the actor would move with moving sceneCamera
        initUIActors(); // init other ui actors

    }


    private void initUIActors() {
        Actor menuBar = createMenuBar();
        Actor inspector = createInspector();
        Actor hierarchy = createHierarchy();
        Actor contentBrowser = createContentBrowser();
        //contentBrowser.setX(hierarchy.getWidth());

        uiStage.addUIActor(console, console.getClass());
        uiStage.addUIActor(menuBar, menuBar.getClass());
        uiStage.addUIActor(hierarchy, hierarchy.getClass());
        uiStage.addUIActor(inspector, inspector.getClass());
        uiStage.addUIActor(contentBrowser, contentBrowser.getClass());
    }

    private Actor createContentBrowser() {
        contentBrowser = new ContentBrowser(skin, scriptManager, assets);
        setupUIActor(contentBrowser);
        contentBrowser.padTop(contentBrowser.getPadTop() * 2);
        return contentBrowser;
    }


    private Actor createInspector() {
        // create bounds
        Actor boundsActor = new Actor();
        boundsActor.setDebug(true);
        boundsActor.setTouchable(Touchable.disabled);
        uiStage.addActor(boundsActor);

        // create inspector
        inspector = new Inspector(skin, ecsStage, ecsManager, editorManager, scriptManager);
        setupUIActor(inspector);
        inspector.setVisible(false);
        inspector.setSize(256,128);

        ResizeListener listener = new ResizeListener(){
            @Override
            public void resized(ResizeEvent event, float screenWidth, float screenHeight) {
                float worldWidth = uiStage.getWidth();
                float worldHeight = uiStage.getHeight();
                float width = worldWidth * 0.9f;
                float height = worldHeight * 0.9f;
                boundsActor.setSize((int)width, (int)height);
                boundsActor.setPosition(worldWidth /2, worldHeight /2, Align.center);

                // position inspector anew
                inspector.setClampedBounds(boundsActor.getX(), boundsActor.getY(), boundsActor.getWidth(), boundsActor.getHeight());
                inspector.setPositionRelativeTo(inputManager.getFocusedEntity());
            }
        };
        inspector.addListener(listener);
        // to conveniently size position and set the bounds for the inspector
        listener.resized(null, 0, 0);
        return inspector;
    }

    private Actor createHierarchy(){
        hierarchy = new Hierarchy(skin, ecsManager, inputManager);
        setupUIActor(hierarchy);
        hierarchy.padTop(hierarchy.getPadTop() * 2);
        //hierarchy.setDebug(true);
        return hierarchy;
    }

    private Actor createMenuBar() {
        // bar for meu options
        MenuBar bar = new MenuBar(skin, uiStage, ecsViewport, ecsManager, editorManager, scriptManager);
        setupUIActor(bar);
        bar.setSize(512, 32);
        bar.addListener(new ResizeListener(){
            @Override
            public void resized(ResizeEvent event, float screenWidth, float screenHeight) {
                bar.setPosition(
                        uiViewport.getWorldWidth() / 2f,
                        uiViewport.getWorldHeight() - bar.getHeight(), Align.center);
            }
        });

        //bar.debug(Table.Debug.cell);
        return bar;
    }


    private void setupUIActor(Table actor) {
        actor.setTouchable(Touchable.enabled);
        actor.pad(Config.DEFAULT_UI_PAD);

        // add listener
        actor.addCaptureListener(new InputListener(){
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                event.getStage().setScrollFocus(event.getListenerActor());
            }
        });

    }




    // create texture

    private Texture createRepeatingTextureFromRegion(TextureRegion region) {
        TextureData data = region.getTexture().getTextureData();
        if (!data.isPrepared()) data.prepare();
        Pixmap oldPixmap = data.consumePixmap();

        // create pixmap from region for texture
        Pixmap newPixmap = new Pixmap(
                region.getRegionWidth(),
                region.getRegionHeight(),
                Pixmap.Format.RGBA8888);
        newPixmap.drawPixmap(
                oldPixmap,
                0,
                0,
                region.getRegionX(),
                region.getRegionY(),
                region.getRegionWidth(),
                region.getRegionHeight());

        // turn pixmap into repeating texture
        Texture texture = new Texture(newPixmap);
        texture.setWrap(
                Texture.TextureWrap.Repeat,
                Texture.TextureWrap.Repeat);
        oldPixmap.dispose();
        return texture;
    }

    /**
     * fires a resize event on all immediate children of the stage root
     */
    private void fireResizeEvent(){
        Event event = Pools.obtain(ResizeListener.ResizeEvent.class);
        SnapshotArray array = uiStage.getRoot().getChildren();
        Object[] children = array.begin();
        for(int i = 0, n = array.size; i < n; i++){
            Actor child = (Actor)children[i];
            child.fire(event);
        }
        array.end();
        Pools.free(event);
    }


    // -- public --

    @Override
    public void render(float delta) {
        ScreenUtils.clear(Color.GRAY);

        updateInput();
        ecsViewport.apply();
        ecsStage.act();
        ecsStage.draw();

        uiViewport.apply();
        uiStage.act();
        uiStage.draw();
    }

    private void updateInput() {
        updateDebugInput();

        // saving (ctrl + s)
        if(Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) && Gdx.input.isKeyJustPressed(Input.Keys.S)){
            ecsManager.saveWorld();
        }
    }

    private void updateDebugInput(){
        // create entity
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            Entity entity = ecsManager.createCanvas();
            Transform transform = entity.getComponent(Transform.class);
            transform.setSize(100,100);
            transform.setPosition(ecsCamera.position.x, ecsCamera.position.y);
            Image image = ecsManager.edit(entity).create(Image.class);
            image.setRegion(new TextureRegion(assets.get("badlogic.jpg", Texture.class)));
        }

        // print window size
        if(Gdx.input.isKeyJustPressed(Input.Keys.P)){
            //log.debug("window size: " + Gdx.graphics.getWidth() + ", " + Gdx.graphics.getHeight());
        }
    }

    @Override
    public void resize(int width, int height) {
        uiViewport.update(width, height, true);
        ecsViewport.update(width, height, false);//world properties' viewport updated in here
        fireResizeEvent(); // ui
    }

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        ecsStage.dispose();
        uiStage.dispose();
        repeatingGridTexture.dispose();
        //roundedCorners.dispose();
        //roundedCornersShadow.dispose();
        ecsManager.dispose();
    }


}
