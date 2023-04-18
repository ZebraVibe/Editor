package com.sk.editor.config;

import com.badlogic.gdx.graphics.Color;
import com.sk.editor.Editor;
import com.sk.editor.utils.ColorConverter;

public class Config {

	// window size
	public static final int WIDTH = 1280, HEIGHT = 720;

	// colors
	public static final Color
			LIGHT_GRAY = ColorConverter.hexToRGBA8888(0x505050),
			GRAY = ColorConverter.hexToRGBA8888(0x3c3c3c),
			DARK_GRAY = ColorConverter.hexToRGBA8888(0x282828),
			DARKEST_GRAY = ColorConverter.hexToRGBA8888(0x191919),
			GREEN = ColorConverter.hexToRGBA8888(0x65ff00);


	public static final float DEFAULT_UI_PAD = 8;

	public static final String CLASS_PATH_DIR_NAME = "compiledClasses";


	// -- preferences --


	public static String EDITOR_PREFERENCES_NAME = Editor.class.getSimpleName();

	/**
	 * used with Files.FileType.External
	 */
	public static String
			EDITOR_PREFERENCES_DIR = "/.prefs/" + EDITOR_PREFERENCES_NAME +"/",
			WORLD_SAVE_FILE = EDITOR_PREFERENCES_DIR + "world",
			PROJECTS_DIR = EDITOR_PREFERENCES_DIR + "projects/",
			DEFAULT_PROJECT_DIR = PROJECTS_DIR + "My-Project/";


	private Config() {}
	
}
