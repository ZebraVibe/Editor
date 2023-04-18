package com.sk.editor.ecs.components;

import com.artemis.PooledComponent;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FileTextureData;
import com.badlogic.gdx.utils.Null;

/**
 * uses @{@link com.sk.editor.ecs.TextureRegionSerializer} to serialize region
 */
public class Image extends Script {
    private @Null TextureRegion region;

    public void setRegion(TextureRegion region) {
        this.region = region;
    }

    public @Null TextureRegion getRegion() {
        return region;
    }

    @Override
    protected void reset() {
        region = null;
    }
}
