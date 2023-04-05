package com.sk.editor.utils;

public enum RenderMode {
    EDIT,GAME;

    public boolean isEdit() { return this == EDIT; }
    public boolean isGame() { return this == GAME; }

}
