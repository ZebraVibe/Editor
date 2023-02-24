package com.sk.editor;

import com.badlogic.gdx.tools.texturepacker.TexturePacker;
import com.badlogic.gdx.tools.texturepacker.TexturePacker.Settings;

public class AssetPacker {

	private static final String RAW_ASSETS = "C:\\dev\\git\\Editor Redone Workspace\\Editor Redone Project\\assets-raw";

	private static final String ASSETS = "C:\\dev\\git\\Editor Redone Workspace\\Editor Redone Project\\assets";

	public static void main(String[] args) {
		
//		Settings settings = new Settings();
//		settings.combineSubdirectories = true;// adds sub dir immages to parent dirs images into the atlas
//		settings.flattenPaths = true; // strips sub dir prefixes ( names must be unique)
//		TexturePacker.process(settings, "from", "to", "fileName", processListener);
//		TexturePacker.processIfModified(null, ASSETS, RAW_ASSETS, ASSETS);
		
		
		Settings settings = new Settings();
//		settings.useIndexes = false;
		packUI(settings);
	}


	
	private static void packUI(Settings settings) {
		TexturePacker.process(RAW_ASSETS + "/ui", ASSETS + "/ui", "ui");
	}
	

}
