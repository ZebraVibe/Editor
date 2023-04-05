package com.sk.editor.ecs.components;

import com.artemis.PooledComponent;
import com.artemis.annotations.PooledWeaver;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Image extends PooledComponent {
    public TextureRegion region;
    public float a, b, c, d, e;

    @Override
    protected void reset() {
        region = null;
    }
}
