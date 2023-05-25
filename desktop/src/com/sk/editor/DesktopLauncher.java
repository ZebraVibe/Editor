package com.sk.editor;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.backends.lwjgl.LwjglAWTCanvas;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.Color;
import com.sk.editor.config.Config;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.platform.WindowUtils;
import com.sun.jna.platform.win32.Wdm;
import com.sun.jna.platform.win32.WinDef;
import org.lwjgl.glfw.GLFW;

import javax.swing.*;
import java.awt.*;

// Please note that on macOS your application needs to be started with the -XstartOnFirstThread JVM argument
public class DesktopLauncher {
	public static void main (String[] arg) {
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setForegroundFPS(60);
		config.setTitle("Editor Redone");
		config.setWindowedMode(Config.WIDTH, Config.HEIGHT);
		//config.setResizable(false);
		config.setPreferencesConfig(Config.EDITOR_PREFERENCES_DIR, Files.FileType.External);
		Editor editor = new Editor();
		Lwjgl3Application app = new Lwjgl3Application(editor, config);
	}


}
