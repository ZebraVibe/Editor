package com.sk.editor.assets;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;

public class AssetDescriptors {
	
	public static AssetDescriptor<TextureAtlas> UI = new AssetDescriptor<>(AssetPaths.UI, TextureAtlas.class);

	private AssetDescriptors() {}
	
	
}
