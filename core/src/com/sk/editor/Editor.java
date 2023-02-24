package com.sk.editor;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Logger;
import com.badlogic.gdx.utils.Null;
import com.sk.editor.screens.EditorScreen;
import com.sk.editor.screens.LoadingScreen;
import com.sk.editor.scripting.ScriptManager;
import com.sk.editor.utils.Fonts;

//
public class Editor extends Game {
	
	private static final Logger log = new Logger(Editor.class.toString(), Logger.DEBUG);
	private SpriteBatch batch;
	private AssetManager assets;
	private Skin skin;
	private Fonts fonts;
	private ShapeRenderer renderer;
	private Screen nextScreen;
	private EditorManager editorManager;
	
	@Override
	public void create () {
		log.debug("Creating Application");
		batch = new SpriteBatch();
		skin = new Skin();
		assets = new AssetManager();
		fonts = new Fonts(assets);
		renderer = new ShapeRenderer();

		editorManager = new EditorManager();

		Gdx.app.setLogLevel(Logger.DEBUG);
		setScreen(new LoadingScreen(this));
	}

	@Override
	public void render () {
		super.render();
		
		if(nextScreen != null) {
			super.setScreen(nextScreen);
			nextScreen = null;
		}
	}
	
	@Override
	public void dispose () {
		log.debug("Disposing Application");
		super.dispose();
		batch.dispose();
		assets.dispose();
		renderer.dispose();
		//skin.dispose();
	}
	
	public AssetManager getAssetManager() {
		return assets;
	}

	public Fonts getFonts() {
		return fonts;
	}

	public SpriteBatch getBatch() {
		return batch;
	}
	
	public ShapeRenderer getShapeRenderer() {
		return renderer;
	}

	public Skin getSkin() {
		return skin;
	}

	public EditorManager getEditorManager(){
		return editorManager;
	}

	@Override
	public void setScreen(Screen screen) {
		nextScreen = screen;
	}
	
	/** @return Maybe null */
	public @Null EditorScreen getEditorScreenIfActive() {
		return isEditorScreenActive() ? (EditorScreen)this.screen : null;
	}
	
	public boolean isEditorScreenActive() {
		return this.screen instanceof EditorScreen;
	}
	
}
