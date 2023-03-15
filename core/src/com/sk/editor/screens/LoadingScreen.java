package com.sk.editor.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.SpriteDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Logger;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.sk.editor.Editor;
import com.sk.editor.assets.*;
import com.sk.editor.config.Config;
import com.sk.editor.ui.UIBase;
import com.sk.editor.ui.UITree;
import com.sk.editor.ui.UIWindow;
import com.sk.editor.utils.FontSize;
import com.sk.editor.utils.Fonts;
import com.sk.editor.utils.Shaders;


public class LoadingScreen extends ScreenAdapter {

	private static final Logger log = new Logger(LoadingScreen.class.getSimpleName(), Logger.DEBUG);
	private static final int LOADING_BAR_WIDTH = 128, LOADING_BAR_HEIGHT = 32; // units

	private Editor editor;
	private AssetManager assets;
	private ShapeRenderer renderer;
	private Viewport viewport;
	private float waitSecAfterLoading = 1;

	public LoadingScreen(Editor editor) {
		this.editor = editor;
		this.assets = editor.getAssetManager();
		this.renderer = editor.getShapeRenderer();
	}
	
	@Override
	public void show() {
		viewport = new FitViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		log.debug("Loading Assets...");
		loadAssets();
	}

	private void loadAssets() {
		// texture atlas
		assets.load(AssetDescriptors.UI);

		// fonts
		Fonts fonts = editor.getFonts();
		fonts.initLoaders(assets);
		fonts.loadAllFontSizesToAssetManager(FontNames.DEFAULT_FONT); // takes a long time
		//fonts.loadToAssetManager(FontNames.DEFAULT_FONT, FontSize.x10);
		//fonts.getOrLoad(FontNames.DEFAULT_FONT, FontSize.x10);

		// shaders
		Shaders shaders = editor.getShaders();
		shaders.initLoaders(assets);
		shaders.loadAllShadersToAssetManager();
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
			initSkin();
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


	private void initSkin() {
		TextureAtlas atlas = assets.get(AssetDescriptors.UI);
		Fonts fonts = editor.getFonts();
		Skin skin = editor.getSkin();

		// -- texture regions --
		TextureRegion pixelRegion = atlas.findRegion(RegionNames.WHITE_PIXEL);
		TextureRegion checkBoxUnchecked = atlas.findRegion(RegionNames.CHECK_BOX_UNCHECKED);
		TextureRegion checkBoxChecked = atlas.findRegion(RegionNames.CHECK_BOX_CHECKED);

		//skin.add(SkinNames.PIXEL_REGION, pixelRegion, TextureRegion.class);
		skin.addRegions(atlas);

		// -- colors --

		// standard
		Color gray = Config.GRAY.cpy();
		Color lightGray = Config.LIGHT_GRAY.cpy();
		Color darkGray = Config.DARK_GRAY.cpy();
		Color darkestGray = Config.DARKEST_GRAY.cpy();
		Color green = Config.GREEN.cpy();

		// defaults
		Color defaultFontColor = green.cpy();
		Color overColor = lightGray.cpy().lerp(Color.WHITE, 0.3f);



		// -- sprites --

		Sprite whiteSprite = new Sprite(pixelRegion);

		Sprite lightGraySprite = new Sprite(pixelRegion);
		lightGraySprite.setColor(Config.LIGHT_GRAY);

		Sprite graySprite = new Sprite(pixelRegion);
		graySprite.setColor(Config.GRAY);

		Sprite darkGraySprite = new Sprite(pixelRegion);
		darkGraySprite.setColor(Config.DARK_GRAY);

		Sprite darkestGraySprite = new Sprite(pixelRegion);
		darkestGraySprite.setColor(Config.DARKEST_GRAY);

		Sprite greenSprite = new Sprite(pixelRegion);
		greenSprite.setColor(Config.GREEN);

		// -- sprite drawable
		SpriteDrawable whiteDrawable = new SpriteDrawable(whiteSprite); // used to call tint to create new drawables

		// -- fonts --
		BitmapFont defaultFont = fonts.get(FontNames.DEFAULT_FONT, FontSize.x24);
		BitmapFont defaultFont10 = fonts.get(FontNames.DEFAULT_FONT, FontSize.x10);
		BitmapFont defaultFont12 = fonts.get(FontNames.DEFAULT_FONT, FontSize.x12);

		skin.add(SkinNames.BITMAP_FONT, defaultFont);

		// -- styles --

		// ui style
		UIBase.UIStyle uiStyle = new UIBase.UIStyle();
		uiStyle.pixel = pixelRegion;
		uiStyle.backgroundShader = assets.get(ShaderNames.CORNER.getName(), ShaderProgram.class);
		uiStyle.shadowShader = assets.get(ShaderNames.CORNER_AND_SHADOW.getName(), ShaderProgram.class);
		uiStyle.cornerRadius = 0;//10;
		uiStyle.shadowSize = 0;//20;
		uiStyle.backgroundColor.set(gray);
		uiStyle.shadowColor.set(1,1,1,0.55f);

		skin.add(SkinNames.UI_STYLE , uiStyle);

		// label
		Label.LabelStyle labelStyle = new Label.LabelStyle();
		labelStyle.font = defaultFont;
		labelStyle.fontColor = defaultFontColor.cpy();

		Label.LabelStyle labelStyle10 = new Label.LabelStyle(labelStyle);
		labelStyle10.font = defaultFont10;

		Label.LabelStyle labelStyle12 = new Label.LabelStyle(labelStyle);
		labelStyle12.font = defaultFont12;

		skin.add(SkinNames.LABEL_STYLE, labelStyle);
		skin.add(SkinNames.LABEL_STYLE_10, labelStyle10);
		skin.add(SkinNames.LABEL_STYLE_12, labelStyle12);


		// text field
		Sprite selectionSprite = new Sprite(greenSprite);
		selectionSprite.setAlpha(0.3f);
		SpriteDrawable selectionDrawable = new SpriteDrawable(selectionSprite);

		Sprite cursorSprite = new Sprite(pixelRegion);
		SpriteDrawable cursorDrawable = new SpriteDrawable(cursorSprite);

		Sprite tfBackgroundSprite = new Sprite(darkestGraySprite);
		SpriteDrawable tfBackgroundDrawable = new SpriteDrawable(tfBackgroundSprite);

		TextField.TextFieldStyle textFieldStyle = new TextField.TextFieldStyle(defaultFont,
				defaultFontColor.cpy(), cursorDrawable, selectionDrawable, tfBackgroundDrawable);

		TextField.TextFieldStyle textFieldStyle10 = new TextField.TextFieldStyle(textFieldStyle);
		textFieldStyle10.font = defaultFont10;

		TextField.TextFieldStyle textFieldStyle12 = new TextField.TextFieldStyle(textFieldStyle);
		textFieldStyle12.font = defaultFont12;

		skin.add(SkinNames.TEXT_FIELD_STYLE , textFieldStyle);
		skin.add(SkinNames.TEXT_FIELD_STYLE10 , textFieldStyle10);
		skin.add(SkinNames.TEXT_FIELD_STYLE12 , textFieldStyle12);

		// check box
		CheckBox.CheckBoxStyle checkBoxStyle = new CheckBox.CheckBoxStyle();
		checkBoxStyle.checkboxOff = new TextureRegionDrawable(checkBoxUnchecked);
		checkBoxStyle.checkboxOn = new TextureRegionDrawable(checkBoxChecked);
		checkBoxStyle.font = defaultFont;
		checkBoxStyle.fontColor = defaultFontColor.cpy();

		CheckBox.CheckBoxStyle checkBoxStyle10 = new CheckBox.CheckBoxStyle(checkBoxStyle);
		checkBoxStyle10.font = defaultFont10;

		CheckBox.CheckBoxStyle checkBoxStyle12 = new CheckBox.CheckBoxStyle(checkBoxStyle);
		checkBoxStyle12.font = defaultFont12;

		skin.add(SkinNames.CHECK_BOX_STYLE, checkBoxStyle);
		skin.add(SkinNames.CHECK_BOX_STYLE10, checkBoxStyle10);
		skin.add(SkinNames.CHECK_BOX_STYLE12, checkBoxStyle12);

		// scroll pane
		Sprite scrollKnobSprite = new Sprite(graySprite);
		scrollKnobSprite.setSize(8,8);
		ScrollPane.ScrollPaneStyle scrollPaneStyle = new ScrollPane.ScrollPaneStyle();
		scrollPaneStyle.hScrollKnob = scrollPaneStyle.vScrollKnob = new SpriteDrawable(scrollKnobSprite);

		skin.add(SkinNames.SCROLL_PANE_STYLE, scrollPaneStyle); // name = "default"

		// button
		Button.ButtonStyle buttonStyle = new Button.ButtonStyle();
		buttonStyle.up = new SpriteDrawable(lightGraySprite);
		buttonStyle.over = whiteDrawable.tint(overColor);
		buttonStyle.down = new SpriteDrawable(darkGraySprite);

		skin.add(SkinNames.BUTTON_STYLE, buttonStyle); // name = "default"

		// text button
		TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
		textButtonStyle.up = new SpriteDrawable((SpriteDrawable) buttonStyle.up);
		textButtonStyle.over = new SpriteDrawable((SpriteDrawable) buttonStyle.over);
		textButtonStyle.down = new SpriteDrawable((SpriteDrawable) buttonStyle.down);
		textButtonStyle.font = defaultFont;
		textButtonStyle.fontColor = defaultFontColor.cpy();

		skin.add(SkinNames.TEXT_BUTTON_STYLE, textButtonStyle); // name = "default"

		// image text button
		ImageTextButton.ImageTextButtonStyle imageTextButtonStyle = new ImageTextButton.ImageTextButtonStyle(textButtonStyle);

		ImageTextButton.ImageTextButtonStyle imageTextButtonStyle10 = new ImageTextButton.ImageTextButtonStyle(imageTextButtonStyle);
		imageTextButtonStyle10.font = defaultFont10;

		ImageTextButton.ImageTextButtonStyle imageTextButtonStyle12 = new ImageTextButton.ImageTextButtonStyle(imageTextButtonStyle);
		imageTextButtonStyle12.font = defaultFont12;

		skin.add(SkinNames.IMAGE_TEXT_BUTTON_STYLE, imageTextButtonStyle);
		skin.add(SkinNames.IMAGE_TEXT_BUTTON_STYLE_10, imageTextButtonStyle10);
		skin.add(SkinNames.IMAGE_TEXT_BUTTON_STYLE_12, imageTextButtonStyle12);

		// window
		Window.WindowStyle windowStyle = new Window.WindowStyle();
		windowStyle.background = new SpriteDrawable(graySprite);
		///windowStyle.stageBackground = whiteDrawable.tint(new Color(0,0,0,0)); // smh does nothing
		windowStyle.titleFont = defaultFont;
		windowStyle.titleFontColor = defaultFontColor.cpy();

		skin.add(SkinNames.WINDOW_STYLE, windowStyle);

		// ui window
		UIWindow.UIWindowStyle uiWindowStyle = new UIWindow.UIWindowStyle(windowStyle);
		uiWindowStyle.uiStyle = new UIBase.UIStyle(uiStyle);
		uiWindowStyle.background = null;

		skin.add(SkinNames.DEFAULT, uiWindowStyle);

		// tree
		Tree.TreeStyle treeStyle = new Tree.TreeStyle();
		treeStyle.selection = new SpriteDrawable(darkestGraySprite);
		treeStyle.over = new SpriteDrawable((SpriteDrawable) buttonStyle.over);
		SpriteDrawable plusDrawable = new SpriteDrawable(new Sprite(atlas.findRegion(RegionNames.ARROW_RIGHT_WHITE)));
		SpriteDrawable minusDrawable = new SpriteDrawable(new Sprite(atlas.findRegion(RegionNames.ARROW_DOWN_WHITE)));
		treeStyle.plus = plusDrawable.tint(lightGray);
		treeStyle.plusOver = plusDrawable.tint(overColor);
		treeStyle.minus = minusDrawable.tint(lightGray);
		treeStyle.minusOver = minusDrawable.tint(overColor);

		skin.add(SkinNames.TREE_STYLE, treeStyle);

		// ui tree
		UITree.UITreeStyle uiTreeStyle = new UITree.UITreeStyle(treeStyle);
		uiTreeStyle.uiStyle = new UIBase.UIStyle(uiStyle);
		uiTreeStyle.lineColor.set(green);
		uiTreeStyle.background = null;

		skin.add(SkinNames.DEFAULT, uiTreeStyle);

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
