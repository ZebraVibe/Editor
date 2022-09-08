package com.sk.editor.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.scenes.scene2d.ui.Button.ButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox.CheckBoxStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ImageTextButton.ImageTextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox.SelectBoxStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.SplitPane.SplitPaneStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Tree.TreeStyle;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.SpriteDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Logger;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.sk.editor.Editor;
import com.sk.editor.assets.AssetDescriptors;


public class LoadingScreen extends ScreenAdapter {

	private static final Logger log = new Logger(LoadingScreen.class.getSimpleName(), Logger.DEBUG);
	public static final int LOADING_BAR_WIDTH = 128, LOADING_BAR_HEIGHT = 32; // units

	private Editor editor;
	private AssetManager assets;
	private ShapeRenderer renderer;
	private Viewport viewport;
	private float waitSecAfterLoading = 1;

	public LoadingScreen(Editor editor) {
		this.assets = editor.getAssetManager();
		this.renderer = editor.getShapeRenderer();
	}
	
	@Override
	public void show() {
		viewport = new FitViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		assets.load(AssetDescriptors.UI);
		log.debug("Loading Assets...");
	}

	@Override
	public void render(float delta) {
		ScreenUtils.clear(Color.BLACK);
		renderProgressBar();

		if (assets.update()) {
			if (waitSecAfterLoading > 0) {
				waitSecAfterLoading -= delta;
				return;
			}
			
			log.debug("...Done Loading Assets");
			editor.setScreen(new EditorScreen(editor));
		}
	}

	private void renderProgressBar() {
		renderer.setProjectionMatrix(viewport.getCamera().combined);
		float progress = assets.getProgress();
		renderer.setColor(Color.WHITE);
		renderer.begin(ShapeType.Filled);
		renderer.rect(
				(viewport.getWorldWidth() - LOADING_BAR_WIDTH) / 2f,
				(viewport.getWorldHeight() - LOADING_BAR_HEIGHT) / 2f, 
				LOADING_BAR_WIDTH * progress,
				LOADING_BAR_HEIGHT);
		renderer.end();
	}


	@Override
	public void resize(int width, int height) {
		viewport.update(width, height, true);
	}

	@Override
	public void hide() {
		dispose();
	}

	@Override
	public void dispose() {
		
	}

}
