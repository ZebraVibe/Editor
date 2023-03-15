package com.sk.editor.ui;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.Logger;
import com.badlogic.gdx.utils.Null;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.sk.editor.ui.logger.EditorLogger;

public class UIStage extends Stage {

    private static final EditorLogger log = new EditorLogger(UIStage.class.toString(), Logger.DEBUG);

    private final ArrayMap<String, Actor> uiActors = new ArrayMap<>();


    public UIStage(Viewport viewport, Batch batch){
        super(viewport, batch);
    }

    /**
     * adds the actor normally but also registers its class name via @{@link Class#getName()}
     * @param actor
     */
    public void addUIActor(Actor actor, Class<?> cls){
        addUIActor(actor, cls.getName());
    }

    public void addUIActor(Actor actor, String key){
        if(actor == null)return;
        uiActors.put(key, actor);
        addActor(actor);
    }

    /**
     * @param cls
     * @return maybe null
     * @param <T>
     */
    public @Null <T extends Actor> T findUIActor(Class<T> cls){
        return (T)uiActors.get(cls.getName());
    }

    public @Null Actor findUIActor(String key){
        return uiActors.get(key);
    }


}
