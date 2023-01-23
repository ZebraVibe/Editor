package com.sk.editor.world.components;

import com.artemis.annotations.PooledWeaver;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

@PooledWeaver
public class Image extends BaseComponent{
    public TextureRegion region;
}
