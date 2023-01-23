package com.sk.editor.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Logger;
import com.badlogic.gdx.utils.viewport.Viewport;

public class GridBackgroundActor extends Table {

    private static final Logger log = new Logger(GridBackgroundActor.class.toString(), Logger.DEBUG);

    private Sprite sprite;
    private Viewport sceneViewport;
    private float u, v, u2, v2;
    private int textureRepeatFactor = 1;
    private float textureScaleFactor = 1.0f;
    private boolean fillViewport;

    public GridBackgroundActor(TextureRegion region, Viewport sceneViewport) {
        this.sprite = new Sprite(region);
        this.sceneViewport = sceneViewport;

        u = region.getU();
        v = region.getV();
        u2 = region.getU2();
        v2 = region.getV2();

        calcRegionUV(); // in case default repeat factor != 1
    }

    // -- public methods --

    /**
     * How many times the texture should repeat itself inside the
     * actors bounds
     *
     * @param textureRepeatFactor 1 by default. must be >= 1
     */
    public void setTextureRepeatFactor(int textureRepeatFactor) {
        this.textureRepeatFactor = MathUtils.clamp(textureRepeatFactor, 1, Integer.MAX_VALUE);
        calcRegionUV();
    }

    public int getTextureRepeatFactor() {
        return textureRepeatFactor;
    }

    /**
     * @param textureScaleFactor scales the size of the texture's sprite by this factor
     */
    public void setTextureScaleFactor(float textureScaleFactor) {
        this.textureScaleFactor = textureScaleFactor;

        // update sprite size
        float regionWidth = sprite.getRegionWidth();
        float regionHeight = sprite.getRegionHeight();
        float newWidth = textureScaleFactor * regionWidth;
        float newHeight = textureScaleFactor * regionHeight;
        sprite.setSize(newWidth, newHeight);

    }

    public float getTextureScaleFactor() {
        return textureScaleFactor;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        OrthographicCamera camera = (OrthographicCamera) (sceneViewport.getCamera());
        float zoom = camera.zoom;

        float worldWidth = sceneViewport.getWorldWidth();
        float worldHeight = sceneViewport.getWorldHeight();
        float worldWidthZoomed = worldWidth * zoom;
        float worldHeightZoomed = worldHeight * zoom;

        float width = fillViewport ? worldWidthZoomed : getWidth();
        float height = fillViewport ? worldHeightZoomed : getHeight();

        float x0 = fillViewport ? camera.position.x - width / 2f : getX();
        float y0 = fillViewport ? camera.position.y - height / 2f : getY();

        int xMin = ((int) (x0) - (int) sprite.getWidth()) / (int) sprite.getWidth() * (int) sprite.getWidth();
        int yMin = ((int) (y0) - (int) sprite.getHeight()) / (int) sprite.getHeight() * (int) sprite.getHeight();

        int xMax = xMin
                + (int) Math.ceil(width / (float) sprite.getWidth()) * (int) sprite.getWidth()
                + (int) sprite.getWidth();
        int yMax = yMin + (int) Math.ceil(height / (float) sprite.getHeight())
                * (int) sprite.getHeight() + (int) sprite.getHeight();

        batch.flush();
        Color color = batch.getColor();
        batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);
        // draws the sprite (which may or may not have a repeated texture) repeatedly
        // to fill out the given bounds (there the texture doesnt need to be repeated actually)
        for (int x = xMin; x <= xMax; x += sprite.getWidth()) {
            for (int y = yMin; y <= yMax; y += sprite.getHeight()) {
                sprite.setPosition(x, y);
                sprite.draw(batch);
            }
        }
        batch.flush();
        batch.setColor(color);
        //super.draw(batch, parentAlpha);
    }

    /**
     *
     * @param fillViewport if true fills out the viewport else fills out the actor
     */
    public void setFillViewport(boolean fillViewport) {
        this.fillViewport = fillViewport;
    }


    // -- private --


    /**
     * handles the region repetitions
     */
    private void calcRegionUV() {
        calcRegionUV(this.textureRepeatFactor);
    }

    /**
     * handles the region repetitions
     */
    private void calcRegionUV(float textureRepeatFactor) {
        // the region used must obviously be also the instance
        // of the region used by the image actor
        float repeatX = u2 * textureRepeatFactor;
        float repeatY = v2 * textureRepeatFactor;
        // recalculates the uv values
        sprite.setRegion(
                u,
                v,
                repeatX,
                repeatY);
    }

}

