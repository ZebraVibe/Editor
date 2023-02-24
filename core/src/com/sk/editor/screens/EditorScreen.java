package com.sk.editor.screens;

import com.artemis.EntityEdit;
import com.artemis.World;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.sk.editor.Editor;
import com.sk.editor.EditorManager;
import com.sk.editor.assets.AssetDescriptors;
import com.sk.editor.assets.RegionNames;
import com.sk.editor.assets.SkinNames;
import com.sk.editor.config.Config;
import com.sk.editor.ecs.ECSManager;
import com.sk.editor.scripting.ScriptManager;
import com.sk.editor.ui.*;
import com.sk.editor.ui.console.Console;
import com.sk.editor.ui.listeners.ResizeListener;
import com.sk.editor.ui.inspector.Inspector;
import com.sk.editor.ui.logger.EditorLogger;
import com.sk.editor.ui.logger.LoggerListener;
import com.sk.editor.utils.ArrayPool;
import com.sk.editor.world.components.Image;
import com.sk.editor.world.components.Transform;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class EditorScreen extends ScreenAdapter {

    private static final EditorLogger log = new EditorLogger(EditorScreen.class.toString(), Logger.DEBUG);

    private Editor editor;
    private AssetManager assets;
    private ShapeRenderer renderer;
    private SpriteBatch batch;
    private Stage uiStage, ecsStage;
    private Viewport uiViewport, ecsViewport;
    private Skin skin;
    private Texture repeatingGridTexture, badlogic;
    private ShaderProgram roundedCorners, roundedCornersShadow;
    private ECSManager ecsManager;
    private EditorManager editorManager;
    private ScriptManager scriptManager;

    private Inspector inspector;
    private Console console;


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

        // create shaders
        roundedCorners = new ShaderProgram(
                Gdx.files.internal("shaders/corner.vert"),
                Gdx.files.internal("shaders/corner.frag"));
        roundedCornersShadow = new ShaderProgram(
                Gdx.files.internal("shaders/cornerAndShadow.vert"),
                Gdx.files.internal("shaders/cornerAndShadow.frag"));

        // create scene background texture
        //gridTexture = new Texture(Gdx.files.internal("grid.png"));
        TextureRegion gridRegion = atlas.findRegion(RegionNames.GRID);
        repeatingGridTexture = createRepeatingTextureFromRegion(gridRegion);
        badlogic = new Texture(Gdx.files.internal("badlogic.jpg"));

    }

    private void initStages() {
        // create stages
        NotifyingOrthographicCamera ecsCamera = new NotifyingOrthographicCamera();
        ecsViewport = new ScreenViewport(ecsCamera);
        uiViewport = new ScreenViewport();

        ecsStage = new Stage(ecsViewport, batch);
        uiStage = new Stage(uiViewport, batch);

        // set gdx input processor
        Gdx.input.setInputProcessor(
                new InputMultiplexer(uiStage, ecsStage));
    }

    /**
     * currently console is toggled with {@link com.badlogic.gdx.Input.Keys#T}.
     * @return
     */
    private void initConsole(){
        console = new Console(skin);
        console.setName(UINames.CONSOLE);
        console.setVisible(false);

        EditorLogger.addListener(new LoggerListener(){
            @Override
            public void onLog(LoggerListener.LoggerEvent event) {
                String prefix = "[" + event.getTag() + "] ";
                String text = prefix + event.getMessage();
                console.print(text, event.getLevel());
            }
        });


        console.setSize(512, 256);
        console.setPosition(uiStage.getWidth() / 2, uiStage.getHeight() /2, Align.center);
        uiStage.addActor(console);
    }

    private void initManagers(){
        // create script manager before console and before ecs manager to compile and load
        initScriptManager();

        // TODO: ecs manager has to inject dependencies for script manager loaded classes on each recompile and load
        // TODO: ecs manager has to handle script manager loaded classes' annotations on each recompile and load
        this.ecsManager = new ECSManager(editor, ecsViewport, uiViewport);
    }

    private void initActors() {
        ECSProcessingActor ecsProcessingActor = new ECSProcessingActor(ecsManager);

        // create input handler actor
        InputHandlerActor inputActor = new InputHandlerActor(uiStage, ecsStage, ecsManager);
        inputActor.setFillParent(true);
        inputActor.setTouchable(Touchable.enabled);

        // create scene background
        GridBackgroundActor backgroundActor = new GridBackgroundActor(
                new TextureRegion(repeatingGridTexture), ecsViewport);
        backgroundActor.setTextureScaleFactor(32); //8
        backgroundActor.setFillViewport(true);

        ecsStage.addActor(backgroundActor);
        ecsStage.addActor(ecsProcessingActor);

        // add actors to their stage
        uiStage.addActor(inputActor); // else the actor would move with moving sceneCamera
        initUIActors();

    }

    private void initUIActors() {
        Actor menuBar = createMenuBar();
        Actor inspectorBounds = createInspectorClampedBounds();
        Actor inspector = createInspector();

        uiStage.addActor(menuBar);
        uiStage.addActor(inspectorBounds);
        uiStage.addActor(inspector);
        console.toFront();
    }

    private void initScriptManager(){
        this.scriptManager = new ScriptManager();

        String srcPath = editorManager.getSourcePathFromPrefs();
        String classPath = editorManager.getClassPathFromPrefs();
        String packageName = editorManager.getPackageNameFromPrefs();

        try {
            if(srcPath != null)scriptManager.setSrcPath(Paths.get(srcPath));
            else log.info("src path not set yet.");
            if(classPath != null)scriptManager.setClassPath(Paths.get(classPath));
            else log.info("cls path not set yet.");
        } catch (InvalidPathException e){
            log.error("Reading invalid paths from prefs.", e);
        }
        if(packageName != null)scriptManager.setPackageName(packageName);
        else log.info("package name not set yet.");

        if(srcPath != null && classPath != null && packageName != null){
            try {
                if (scriptManager.compileAndLoad()) {
                    scriptManager.debugLoadedClasses();
                }
            } catch (Exception e) {
                log.error("Could not initially compile and load!", e);
            }
        }
    }

    private Actor createInspectorClampedBounds(){
        Table actor = new Table();
        actor.setName(UINames.INSPECTOR_CLAMPED_BOUNDS);

        float width = uiViewport.getWorldWidth();
        float height = uiViewport.getWorldHeight();
        float padX = width / 5;
        float padY = height / 5;
        actor.setSize(width - 2*padX, height - 2*padY);
        actor.setPosition(width /2, height /2, Align.center);

        actor.addListener(new ResizeListener(){
            @Override
            public void resized(ResizeEvent event, float screenWidth, float screenHeight) {
                float worldWidth = uiViewport.getWorldWidth();
                float worldHeight = uiViewport.getWorldHeight();
                float width = worldWidth * 0.9f;
                float height = worldHeight * 0.9f;
                actor.setSize(width, height);
                actor.setPosition(worldWidth /2, worldHeight /2, Align.center);
            }
        });

        actor.setDebug(true);
        return actor;
    }

    private Actor createInspector() {
        inspector = new Inspector(
                skin, roundedCorners, roundedCornersShadow,
                ecsManager, editorManager, scriptManager);
        setupUIActor(inspector, UINames.INSPECTOR);
        inspector.setVisible(false);
        inspector.setSize(256,128);

        inspector.addListener(new ResizeListener(){
            @Override
            public void resized(ResizeEvent event, float screenWidth, float screenHeight) {
                inspector.updateInspectorPositionRelativeTo(ecsManager.getSelectedEntity(), ecsStage);
            }
        });

        return inspector;
    }

    private Actor createMenuBar() {
        // bar for meu options
        UIActor bar = createUIActor(UINames.MENU_BAR);
        bar.setSize(512, 32);
        bar.addListener(new ResizeListener(){
            @Override
            public void resized(ResizeEvent event, float screenWidth, float screenHeight) {
                bar.setPosition(
                        uiViewport.getWorldWidth() / 2f,
                        uiViewport.getWorldHeight() - bar.getHeight(), Align.center);
            }
        });


        // create entity button
        TextButton createButton = new TextButton("Create", skin);
        createButton.addListener(new ClickListener(Input.Buttons.LEFT) {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                int id = ecsManager.createEntity();
                Transform transform = ecsManager.getTransformMapper().get(id);
                transform.setSize(64, 64);
                Vector3 camCoords = ecsViewport.getCamera().position;
                transform.setPosition(camCoords.x, camCoords.y);
            }
        });

        // source path button
        TextButton srcPathButton = new TextButton("Src-path", skin);
        srcPathButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Window window = new Window("Set Source Path", skin);

                // src text field
                Label srcLabel = new Label("source path:", skin);
                TextField srcTF = new TextField("", skin);
                srcTF.setMessageText("gdx v1.11.0, artemis v2.30");
                String srcPath = editorManager.getSourcePathFromPrefs();
                if(srcPath != null)srcTF.setText(srcPath);

                Table srcTable = new Table();
                srcTable.add(srcLabel).spaceRight(1);
                srcTable.add(srcTF).expandX().fillX();

                // package name text field
                Label packageLabel = new Label("package name:", skin);
                TextField packageTF = new TextField("", skin);
                packageTF.setMessageText("i.e. com.my.game");
                String packageName = editorManager.getPackageNameFromPrefs();
                if(packageName != null)packageTF.setText(packageName);

                Table packageTable = new Table();
                packageTable.add(packageLabel).spaceRight(1);
                packageTable.add(packageTF).expandX().fillX();


                // ok button
                TextButton okButton = new TextButton("ok", skin);
                okButton.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        updateScriptManager(srcTF.getText(), packageTF.getText());
                        console.setVisible(true);
                    }
                });

                // close button
                TextButton closeButton = new TextButton("close", skin);
                closeButton.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        window.remove();
                    }
                });

                Table buttons = new Table();
                buttons.defaults().expandX().fillX();
                buttons.add(okButton).spaceRight(1);
                buttons.add(closeButton);

                window.pad(Config.DEFAULT_UI_PAD);
                window.defaults().spaceBottom(1).expandX().fillX();
                window.add(srcTable).row();
                window.add(packageTable).row();
                window.add(buttons);
                window.setSize(512,256);

                window.setPosition(event.getStage().getWidth() / 2f, event.getStage().getHeight() / 2f, Align.center);
                event.getStage().addActor(window);
            }
        });

        // add to bar
        bar.left();
        bar.defaults().spaceRight(3);
        bar.add(createButton, srcPathButton);
        //bar.debug(Table.Debug.cell);
        return bar;
    }

    private UIActor createUIActor(String name) {
        UIActor actor = new UIActor(
                skin.getRegion(SkinNames.PIXEL_REGION),
                roundedCorners,
                roundedCornersShadow);
        setupUIActor(actor, name);
        return actor;
    }

    private void setupUIActor(UIActor actor, String name) {
        actor.setName(name);
        actor.setTouchable(Touchable.enabled);
        actor.pad(Config.DEFAULT_UI_PAD);

        // add listener
        actor.addCaptureListener(new InputListener(){
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                event.getStage().setScrollFocus(event.getListenerActor());
            }
        });



        // shadow
        actor.setShadowColor(Color.BLACK);
        actor.setShadowAlpha(0.55f);
        actor.setShadowSize(20);
        actor.setEnableShadow(false);

        // actor
        actor.setColor(Config.GRAY);
        actor.setCornerRadius(0);//(10);
        actor.setTouchable(Touchable.enabled);
    }

    /**
     *
     * @param srcDir
     * @param packageName
     * @return true if the script manager could be successfully compiled and loaded
     */
    private boolean updateScriptManager(String srcDir, String packageName){
        Path newSrcPath = null;
        Path newClassPath = null;
        String newPackageName = packageName;

        try {
            newSrcPath = Paths.get(srcDir);
            newClassPath = Paths.get(newSrcPath.getParent().toString(), Config.CLASS_PATH_DIR_NAME);
        } catch (InvalidPathException e){
            log.error("Path in invalid." + e.toString());
            return false;
        }

        if(scriptManager.setSrcPath(newSrcPath)){
            // update prefs
            editorManager.setSourcePathOfPrefs(newSrcPath.toString());
        }
        if(scriptManager.setClassPath(newClassPath)){
            // update prefs
            editorManager.setClassPathOfPrefs(newClassPath.toString());
        }
        if(scriptManager.setPackageName(packageName)){
            // update prefs
            editorManager.setPackageNameOfPrefs(packageName);
        }

        boolean success = false;

        try {
            success = scriptManager.compileAndLoad();
        } catch (Exception e) {
            log.error("Could not compile and load. ", e);
            return false;
        }
        if (success)scriptManager.debugLoadedClasses();
        return true;
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

        updateDebugInput();
        ecsViewport.apply();
        ecsStage.act();
        ecsStage.draw();

        uiViewport.apply();
        uiStage.act();
        uiStage.draw();
    }

    private void updateDebugInput() {
        // toggle console
        if(Gdx.input.isKeyJustPressed(Input.Keys.T)){
            Actor focus = uiStage.getKeyboardFocus();
            if(focus != null && focus.isDescendantOf(console) && focus != console)return;
            console.setVisible(!console.isVisible());
        }

        // check shader compilation
        if (Gdx.input.isKeyJustPressed(Input.Keys.K)) {
            log.debug(roundedCorners.getLog());
            log.debug(roundedCornersShadow.getLog());
        }

        // create entity
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            World world = ecsManager.getWorld();
            int entity = world.create();
            EntityEdit edit = world.edit(entity);
            Transform transform = edit.create(Transform.class);
            transform.width = 100;
            transform.height = 100;
            transform.x = ecsViewport.getCamera().position.x;
            transform.y = ecsViewport.getCamera().position.y;
            Image image = edit.create(Image.class);
            image.region = new TextureRegion(badlogic);
        }
        // manipulate shader
        /*
        float maxRadius = 50;
        if (Gdx.input.isKeyPressed(Input.Keys.P)) {
            float radius = MathUtils.clamp(testUIActor.getCornerRadius() + 1, 0, maxRadius);
            testUIActor.setCornerRadius(radius);
        } else if (Gdx.input.isKeyPressed(Input.Keys.M)) {
            float radius = MathUtils.clamp(testUIActor.getCornerRadius() - 1, 0, maxRadius);
            testUIActor.setCornerRadius(radius);
        }*/
    }

    @Override
    public void resize(int width, int height) {
        uiViewport.update(width, height, true);
        ecsViewport.update(width, height, false);

        fireResizeEvent();
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
        badlogic.dispose();
        roundedCorners.dispose();
        roundedCornersShadow.dispose();
        ecsManager.dispose();
    }


}
