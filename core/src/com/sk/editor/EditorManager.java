package com.sk.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.Null;
import com.sk.editor.config.Config;

public class EditorManager {

    private Preferences editorPrefs;

    public EditorManager(){
        editorPrefs = Gdx.app.getPreferences(Editor.class.getSimpleName());
    }

    public Preferences getPrefs(){
        return editorPrefs;
    }

    /**
     * @return null if absent
     */
    public @Null String getSourcePathFromPrefs(){
        String path = editorPrefs.getString(Config.SOURCE_PATH_PREFS_KEY);
        if(path == null || path.isEmpty())return null;
        return path;
    }

    public void setSourcePathOfPrefs(String path){
        editorPrefs.putString(Config.SOURCE_PATH_PREFS_KEY, path);
        editorPrefs.flush();
    }

    /**
     * @return null if absent
     */
    public @Null String getClassPathFromPrefs(){
        String path = editorPrefs.getString(Config.CLASS_PATH_PREFS_KEY);
        if(path == null || path.isEmpty())return null;
        return path;
    }

    public void setClassPathOfPrefs(String path){
        editorPrefs.putString(Config.CLASS_PATH_PREFS_KEY, path);
        editorPrefs.flush();
    }


    /**
     * @return null if absent
     */
    public @Null String getPackageNameFromPrefs(){
        String name = editorPrefs.getString(Config.PACKAGE_NAME_PREFS_KEY);
        if(name == null || name.isEmpty())return null;
        return name;
    }

    public void setPackageNameOfPrefs(String name){
        editorPrefs.putString(Config.PACKAGE_NAME_PREFS_KEY, name);
        editorPrefs.flush();
    }




}
