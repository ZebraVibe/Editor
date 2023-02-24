package com.sk.editor.ui.listeners;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Logger;

public class ResizeListener implements EventListener {
    @Override
    public boolean handle(Event event) {
        if(!(event instanceof ResizeEvent))return false;
        resized((ResizeEvent)event, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        return false; // never handle to inform all listeners
    }

    public void resized(ResizeEvent event, float screenWidth, float screenHeight){}

    public static class ResizeEvent extends Event {
    }
}
