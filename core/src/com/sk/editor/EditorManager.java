package com.sk.editor;

import com.sk.editor.utils.PrefKeys;

public class EditorManager {
    private PrefKeys prefKeys;

    public EditorManager(){
        prefKeys = new PrefKeys();
    }

    public PrefKeys getPrefKeys(){
        return prefKeys;
    }


}
