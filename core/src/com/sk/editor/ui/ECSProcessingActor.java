package com.sk.editor.ui;

import com.artemis.World;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.sk.editor.ecs.ECSManager;

public class ECSProcessingActor extends Table {

    private ECSManager ecsManager;

    public ECSProcessingActor(ECSManager ecsManager){
        this.ecsManager = ecsManager;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        super.draw(batch, parentAlpha);
        batch.flush();

        // render world
        ecsManager.processWorld(Gdx.graphics.getDeltaTime());
    }

}
