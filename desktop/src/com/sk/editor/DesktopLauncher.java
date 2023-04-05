package com.sk.editor;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.sk.editor.config.Config;

// Please note that on macOS your application needs to be started with the -XstartOnFirstThread JVM argument
public class DesktopLauncher {
	public static void main (String[] arg) {
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setForegroundFPS(60);
		config.setTitle("Editor Redone");
		config.setWindowedMode(Config.WIDTH, Config.HEIGHT);
		config.setPreferencesConfig(Config.EDITOR_PREFERENCES_DIR, Files.FileType.External);
		new Lwjgl3Application(new Editor(), config);
	}
}
