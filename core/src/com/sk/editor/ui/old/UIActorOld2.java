package com.sk.editor.ui.old;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Logger;

public class UIActorOld2 extends Table {

    private static final Logger log = new Logger(UIActorOld2.class.toString(), Logger.DEBUG);

    //TextureRegion region;
    ShaderProgram roundedCorners, roundedCornersShadow;

    ShapeRenderer roundedCornersRenderer, roundedCornersShadowRenderer;

    float maxRadius = 50;
    float cornerRadius;// = maxRadius / 2;
    float shadowSize = 20;
    float shadowAlpha = 0.55f;
    Color shadowColor = new Color(1,1,1,1), tmpColor = new Color();



    public UIActorOld2(ShapeRenderer roundedCornersRenderer,
                       ShapeRenderer roundedCornersShadowRender,
                       ShaderProgram roundedCorners, ShaderProgram roundedCornersShadow){
        //new ShapeRenderer(5000, new ShaderProgram("",""));
        //this.region = new TextureRegion(pixelTexture);
        this.roundedCorners = roundedCorners;
        this.roundedCornersShadow = roundedCornersShadow;
        this.roundedCornersRenderer = roundedCornersRenderer;
        this.roundedCornersShadowRenderer = roundedCornersShadowRender;
        //setSize(region.getRegionWidth(), region.getRegionHeight());
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
    protected void drawBackground(Batch batch, float parentAlpha, float x0, float y0) {
        super.drawBackground(batch, parentAlpha, x0, y0);
        if(roundedCorners.isCompiled() == false ||
                roundedCornersShadow.isCompiled() == false)return;
        batch.flush();
        Gdx.gl.glEnable(GL20.GL_BLEND);

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
        //batch.setShader(roundedCornersShadow);
        //batch.setColor(shadowColor);
        tmpColor.set(roundedCornersShadowRenderer.getColor());
        roundedCornersShadowRenderer.setColor(shadowColor);
        roundedCornersShadowRenderer.setProjectionMatrix(batch.getProjectionMatrix());
        roundedCornersShadowRenderer.setTransformMatrix(batch.getTransformMatrix());
        roundedCornersShadowRenderer.begin(ShapeRenderer.ShapeType.Filled);
        roundedCornersShadow.bind();
        roundedCornersShadow.setUniformf("u_width", shadowWidth);
        roundedCornersShadow.setUniformf("u_height", shadowHeight);
        roundedCornersShadow.setUniformf("u_radius", shadowRadius);
        roundedCornersShadow.setUniformf("u_shadowSize", shadowSize);
        roundedCornersShadow.setUniformf("u_shadowAlpha", shadowAlpha);
        roundedCornersShadowRenderer.rect(
                shadowX,
                shadowY,
                shadowOriginX,
                shadowOriginY,
                shadowWidth,
                shadowHeight,
                getScaleX(),
                getScaleY(),
                getRotation());
        roundedCornersShadowRenderer.end();
        roundedCornersShadowRenderer.setColor(tmpColor);


        // draw texture
        //batch.setShader(roundedCorners);
        //batch.setColor(getColor());
        tmpColor.set(roundedCornersRenderer.getColor());
        roundedCornersRenderer.setColor(getColor());
        roundedCornersRenderer.setProjectionMatrix(batch.getProjectionMatrix());
        roundedCornersRenderer.setTransformMatrix(batch.getTransformMatrix());
        roundedCornersRenderer.begin(ShapeRenderer.ShapeType.Filled);
        roundedCorners.bind();
        roundedCorners.setUniformf("u_width", width);
        roundedCorners.setUniformf("u_height", height);
        roundedCorners.setUniformf("u_radius", cornerRadius);
        roundedCornersRenderer.rect(
                x,
                y,
                getOriginX(),
                getOriginY(),
                width,
                height,
                getScaleX(),
                getScaleY(),
                getRotation());
        roundedCornersRenderer.end();
        roundedCornersRenderer.setColor(tmpColor);
        Gdx.gl.glDisable(GL20.GL_BLEND);

    }

}
