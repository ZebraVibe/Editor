package com.sk.editor.screens;

import com.artemis.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.utils.Logger;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.sk.editor.Editor;
import com.sk.editor.ui.*;
import com.sk.editor.world.components.Image;
import com.sk.editor.world.components.Transform;

public class EditorScreen extends ScreenAdapter {

	private static final Logger log = new Logger(EditorScreen.class.toString(), Logger.DEBUG);
	
	private Editor editor;
	private AssetManager assets;
	private ShapeRenderer renderer;
	private SpriteBatch batch;
	private Stage uiStage, ecsStage;
	private Viewport uiViewport, ecsViewport;
	private Texture gridTexture, repeatingGridTexture, badlogic;
	private ECSHandler ecsHandler;

	
	public EditorScreen(Editor editor) {
		this.editor = editor;
		this.assets = editor.getAssetManager();
		this.renderer = editor.getShapeRenderer();
		this.batch = editor.getBatch();
	}
	
	@Override
	public void show() {
		init();
	}
	
	private void init() {
		initStages();
		initActors();
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

	private void initActors() {
		this.ecsHandler = new ECSHandler(editor, ecsViewport, uiViewport);
		ECSProcessingActor ecsProcessingActor = new ECSProcessingActor(ecsHandler);

		// create scene background texture
		gridTexture = new Texture(Gdx.files.internal("grid.png"));
		repeatingGridTexture = createRepeatingTextureFromRegion(new TextureRegion(gridTexture));
		badlogic = new Texture(Gdx.files.internal("badlogic.jpg"));

		// create scene camera actor
		InputHandlerActor inputActor = new InputHandlerActor(uiViewport, ecsViewport, ecsHandler);
		inputActor.setFillParent(true);
		inputActor.setTouchable(Touchable.enabled);


		// create scene background
		GridBackgroundActor backgroundActor = new GridBackgroundActor(
				new TextureRegion(repeatingGridTexture), ecsViewport);
		backgroundActor.setTextureScaleFactor(32); //8
		backgroundActor.setFillViewport(true);

		// add actors to their stage
		uiStage.addActor(inputActor); // else the actor would move with moving sceneCamera

		ecsStage.addActor(backgroundActor);
		ecsStage.addActor(ecsProcessingActor);
	}

	// create texture

	private Texture createRepeatingTextureFromRegion(TextureRegion region){
		TextureData data = region.getTexture().getTextureData();
		if (!data.isPrepared())data.prepare();
		Pixmap oldPixmap = data.consumePixmap();

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

		Texture texture = new Texture(newPixmap);
		texture.setWrap(
				Texture.TextureWrap.Repeat,
				Texture.TextureWrap.Repeat);
		oldPixmap.dispose();
		return texture;
	}


	
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
		if(Gdx.input.isKeyJustPressed(Input.Keys.SPACE)){
			World world = ecsHandler.getWorld();
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
	}

	@Override
	public void resize(int width, int height) {
		uiViewport.update(width, height, true);
		ecsViewport.update(width, height, false);
	}

	@Override
	public void hide() {
		dispose();
	}
	
	@Override
	public void dispose() {
		ecsStage.dispose();
		uiStage.dispose();
		gridTexture.dispose();
		repeatingGridTexture.dispose();
		badlogic.dispose();
		ecsHandler.dispose();
	}
	
	
	
	
}
