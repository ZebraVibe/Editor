package com.sk.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.sk.editor.config.Config;
import com.sk.editor.utils.PrefKeys;
import com.sk.editor.utils.RenderMode;

public class EditorManager {
    private PrefKeys prefKeys;
    private RenderMode renderMode;

    public EditorManager(){
        prefKeys = new PrefKeys();
        setRenderMode(RenderMode.EDIT);
    }

    public PrefKeys getPrefKeys(){
        return prefKeys;
    }

    public void setRenderMode(RenderMode mode) {
        this.renderMode = mode;
    }

    public RenderMode getRenderMode() {
        return renderMode;
    }

}
