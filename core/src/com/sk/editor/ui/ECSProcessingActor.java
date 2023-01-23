package com.sk.editor.ui;

import com.artemis.World;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

public class ECSProcessingActor extends Table {

    private ECSHandler ecsHandler;

    public ECSProcessingActor(ECSHandler ecsHandler){
        this.ecsHandler = ecsHandler;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        super.draw(batch, parentAlpha);
        batch.flush();

        World world = ecsHandler.getWorld();

        // render world
        world.setDelta(Gdx.graphics.getDeltaTime());
        world.process();
    }

}
