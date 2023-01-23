package com.sk.editor.config;

import com.badlogic.gdx.graphics.Color;
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


	private Config() {}
	
}
