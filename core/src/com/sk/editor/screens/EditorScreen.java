package com.sk.editor.screens;

import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Logger;
import com.sk.editor.Editor;

public class EditorScreen extends ScreenAdapter {

	private static final Logger log = new Logger(EditorScreen.class.toString(), Logger.DEBUG);
	
	private Editor editor;
	private AssetManager assets;
	private ShapeRenderer renderer;
	private SpriteBatch batch;
	
	public EditorScreen(Editor editor) {
		this.editor = editor;
		this.assets = editor.getAssetManager();
		this.renderer = editor.getShapeRenderer();
		this.batch = editor.getBatch();
	}
	
	@Override
	public void show() {
		
	}
	
	@Override
	public void render(float delta) {
	}
	
	@Override
	public void hide() {
		dispose();
	}
	
	@Override
	public void dispose() {
	}
	
	
	
	
}
