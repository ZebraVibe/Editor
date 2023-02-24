package com.sk.editor.ui.old;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Logger;

public class UIActorOld extends Table {

    private static final Logger log = new Logger(UIActorOld.class.toString(), Logger.DEBUG);

    TextureRegion region;
    ShaderProgram roundedCorners, roundedCornersShadow;

    float maxRadius = 50;
    float cornerRadius;// = maxRadius / 2;
    float shadowSize = 20;
    float shadowAlpha = 0.55f;
    Color shadowColor = new Color(1,1,1,1), tmp = new Color();



    public UIActorOld(Texture pixelTexture, ShaderProgram roundedCorners, ShaderProgram roundedCornersShadow){
        //new ShapeRenderer(5000, new ShaderProgram("",""));
        this.region = new TextureRegion(pixelTexture);
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


    @Override
    protected void drawBackground(Batch batch, float parentAlpha, float x, float y) {
        super.drawBackground(batch, parentAlpha, x, y);

    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if(roundedCorners.isCompiled() == false ||
        roundedCornersShadow.isCompiled() == false)return;
        batch.flush();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        tmp.set(batch.getColor());

        float width = getWidth();//region.getRegionWidth();
        float height = getHeight();//region.getRegionHeight();
        float x = getX();
        float y = getY();

        float shadowX = x - shadowSize;
        float shadowY = y - shadowSize;
        float shadowWidth = width + 2 * shadowSize;
        float shadowHeight = height + 2 * shadowSize;
        float shadowRadius = cornerRadius + shadowSize;
        float shadowOriginX = ((shadowWidth + getOriginX()) / shadowWidth) * shadowWidth;
        float shadowOriginY = ((shadowHeight + getOriginY()) / shadowHeight) * shadowHeight;

        // draw shadow
        batch.setShader(roundedCornersShadow);
        batch.setColor(shadowColor);
        //fragment
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


        // draw texture
        batch.setShader(roundedCorners);
        batch.setColor(getColor());
        //fragment
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
        batch.setColor(tmp);
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // draw children
        super.draw(batch, parentAlpha);
    }
}
