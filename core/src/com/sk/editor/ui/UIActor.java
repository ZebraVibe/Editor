package com.sk.editor.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Logger;
import com.badlogic.gdx.utils.Pools;

public class UIActor extends Table {

    private static final Logger log = new Logger(UIActor.class.toString(), Logger.DEBUG);

    TextureRegion region;
    ShaderProgram roundedCorners, roundedCornersShadow;

    float backgroundAlpha = 1f;
    float cornerRadius = 0;// = maxRadius / 2;
    float shadowSize = 20;
    float shadowAlpha = 0.55f;
    Color shadowColor = new Color(1,1,1,1), tmpColor = new Color();
    boolean enableShadow = false;


    public UIActor(TextureRegion pixelRegion, ShaderProgram roundedCorners, ShaderProgram roundedCornersShadow){
        this.region = pixelRegion;//new TextureRegion(pixelTexture);
        this.roundedCorners = roundedCorners;
        this.roundedCornersShadow = roundedCornersShadow;
        setSize(region.getRegionWidth(), region.getRegionHeight());
    }


    /**
     * Sets the radius which each corner is rounded to
     * default is {@link #cornerRadius}
     * @param radius
     */
    public void setCornerRadius(float radius) {
        this.cornerRadius = radius;
    }

    public float getCornerRadius(){
        return cornerRadius;
    }

    /**
     * default is {@link #shadowSize}
     * @param shadowSize
     */
    public void setShadowSize(float shadowSize) {
        this.shadowSize = shadowSize;
    }

    /**
     * default is {@link #shadowAlpha}
     * @param shadowAlpha
     */
    public void setShadowAlpha(float shadowAlpha) {
        this.shadowAlpha = shadowAlpha;
    }

    /**
     * default color is (1, 1, 1, 1);
     * @param color
     */
    public void setShadowColor(Color color){
        this.shadowColor.set(color);
    }

    /**
     * default color is (1, 1, 1, 1);
     */
    public void setShadowColor(float r, float g, float b , float a){
        this.shadowColor.set(r,g,b,a);
    }

    /**
     * default is false;
     * @param enableShadow
     */
    public void setEnableShadow(boolean enableShadow) {
        this.enableShadow = enableShadow;
    }

    public void setBackgroundAlpha(float alpha){
        this.backgroundAlpha = alpha;
    }

    public float getBackgroundAlpha() {
        return backgroundAlpha;
    }

    @Override
    protected void drawBackground(Batch batch, float parentAlpha, float x0, float y0) {
        super.drawBackground(batch, parentAlpha * backgroundAlpha, x0, y0);
        if(roundedCorners.isCompiled() == false ||
                roundedCornersShadow.isCompiled() == false)return;
        batch.flush();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        tmpColor.set(batch.getColor());
        Vector2 tmpCoords = Pools.obtain(Vector2.class);


        float x = getX();
        float y = getY();
        float width = getWidth();//region.getRegionWidth();
        float height = getHeight();//region.getRegionHeight();

        if(enableShadow){
            float localShadowX = -shadowSize;
            float localShadowY = -shadowSize;
            localToParentCoordinates(tmpCoords.set(localShadowX, localShadowY));
            float shadowX = tmpCoords.x;
            float shadowY = tmpCoords.y;
            float shadowWidth = width + 2 * shadowSize;
            float shadowHeight = height + 2 * shadowSize;
            float shadowRadius = cornerRadius + shadowSize;
            float shadowOriginX = ((shadowWidth + getOriginX()) / shadowWidth) * shadowWidth;
            float shadowOriginY = ((shadowHeight + getOriginY()) / shadowHeight) * shadowHeight;

            // draw shadow
            batch.setShader(roundedCornersShadow);
            batch.setColor(shadowColor.r, shadowColor.g, shadowColor.b, shadowColor.a * parentAlpha);
            // calculates screen coords with origin on top left but frag screen coord origin is on bottom left
            localToScreenCoordinates(tmpCoords.set(localShadowX, localShadowY));// bottom left
            tmpCoords.y = Gdx.graphics.getHeight() - tmpCoords.y;
            roundedCornersShadow.setUniformf("u_x", tmpCoords.x);
            roundedCornersShadow.setUniformf("u_y", tmpCoords.y);
            roundedCornersShadow.setUniformf("u_width", shadowWidth);
            roundedCornersShadow.setUniformf("u_height", shadowHeight);
            roundedCornersShadow.setUniformf("u_radius", shadowRadius);
            roundedCornersShadow.setUniformf("u_shadowSize", shadowSize);
            roundedCornersShadow.setUniformf("u_shadowAlpha", shadowAlpha);
            batch.draw(
                    region,
                    shadowX,
                    shadowY,
                    shadowOriginX,
                    shadowOriginY,
                    shadowWidth,
                    shadowHeight,
                    getScaleX(),
                    getScaleY(),
                    getRotation());
            batch.flush();
        }

        // draw texture
        batch.setShader(roundedCorners);
        Color color = getColor();
        batch.setColor(color.r, color.g, color.b, color.a * parentAlpha * backgroundAlpha);
        // calculates screen coords with origin on top left but frag screen coord origin is on bottom left
        localToScreenCoordinates(tmpCoords.set(0,0));// bottom left
        tmpCoords.y = Gdx.graphics.getHeight() - tmpCoords.y;
        roundedCorners.setUniformf("u_x", tmpCoords.x);
        roundedCorners.setUniformf("u_y", tmpCoords.y);
        roundedCorners.setUniformf("u_width", width);
        roundedCorners.setUniformf("u_height", height);
        roundedCorners.setUniformf("u_radius", cornerRadius);
        batch.draw(
                region,
                x,
                y,
                getOriginX(),
                getOriginY(),
                width,
                height,
                getScaleX(),
                getScaleY(),
                getRotation());
        batch.flush();
        batch.setShader(null);
        batch.setColor(tmpColor);
        Gdx.gl.glDisable(GL20.GL_BLEND);
        Pools.free(tmpCoords);
    }




}
