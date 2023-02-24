package com.sk.editor.world.components;

import com.artemis.annotations.PooledWeaver;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Image extends BaseComponent{
    public TextureRegion region;
    public float a, b, c, d, e;

    @Override
    protected void reset() {
        region = null;
    }
}
