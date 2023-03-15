package com.sk.editor.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Null;
import com.badlogic.gdx.utils.Pools;

public class UIBase extends Table {

    private @Null UIStyle style;
    private Color tmpColor = new Color();



    /**
     * creates an actor with a copy of the provided style
     */
    public UIBase(Skin skin){
        this(skin.get(UIStyle.class));
        setSkin(skin);
    }

    /**
     * creates an actor with a copy of the provided style
     */
    public UIBase(Skin skin, String styleName){
        this(skin.get(styleName, UIStyle.class));
        setSkin(skin);
    }

    /**
     * creates an actor with a copy of the provided style
     * @param style
     */
    public UIBase(UIStyle style){
        setStyle(style);
    }

    public UIStyle getStyle(){
        return style;
    }

    /**
     * copies the values from the given style
     * @param style
     */
    public void setStyle(UIStyle style){
        if(this.style == null)this.style = new UIStyle(style);
        else this.style.set(style);
    }

    @Override
    protected void drawBackground(Batch batch, float parentAlpha, float x, float y) {
        if(style == null || style.pixel == null)return;
        batch.flush();
        //Gdx.gl.glEnable(GL20.GL_BLEND);
        tmpColor.set(batch.getColor());

        Vector2 tmp = Pools.obtain(Vector2.class);
        Color bgColor = style.backgroundColor;
        float cornerRadius = style.cornerRadius;
        ShaderProgram backgroundShader = null;


        // draw shadow
        drawShadow(batch, parentAlpha);


        // draw background
        if(style.backgroundShader != null && style.backgroundShader.isCompiled()){
            backgroundShader = style.backgroundShader;
            batch.setShader(backgroundShader);
        }

        if(backgroundShader != null){
            // calculates screen coords with origin on top left but frag screen coord origin is on bottom left
            localToScreenCoordinates(tmp.set(0,0));// bottom left
            tmp.y = Gdx.graphics.getHeight() - tmp.y;
            backgroundShader.setUniformf("u_x", tmp.x);
            backgroundShader.setUniformf("u_y", tmp.y);
            backgroundShader.setUniformf("u_width", getWidth());
            backgroundShader.setUniformf("u_height", getHeight());
            backgroundShader.setUniformf("u_radius", cornerRadius);
        }

        float alpha = getColor().a * bgColor.a * parentAlpha;
        batch.setColor(bgColor.r, bgColor.g, bgColor.b, alpha);
        batch.draw(
                style.pixel,
                getX(),
                getY(),
                getOriginX(),
                getOriginY(),
                getWidth(),
                getHeight(),
                getScaleX(),
                getScaleY(),
                getRotation());

        batch.flush();

        batch.setShader(null);
        batch.setColor(tmpColor);
        //Gdx.gl.glDisable(GL20.GL_BLEND);
        Pools.free(tmp);
    }


    private void drawShadow(Batch batch, float parentAlpha){
        Vector2 tmp = Pools.obtain(Vector2.class);
        float shadowSize = style.shadowSize;
        float cornerRadius = style.cornerRadius;
        Color shadowColor = style.shadowColor;
        ShaderProgram shadowShader = null;

        float localShadowX = -style.shadowSize;
        float localShadowY = -style.shadowSize;
        localToParentCoordinates(tmp.set(localShadowX, localShadowY));
        float shadowX = tmp.x;
        float shadowY = tmp.y;
        float shadowWidth = shadowSize == 0 ? 0 : getWidth() + 2 * shadowSize;
        float shadowHeight = shadowSize == 0 ? 0 : getHeight() + 2 * shadowSize;
        float shadowRadius = cornerRadius + shadowSize;
        float shadowOriginX = ((shadowWidth + getOriginX()) / shadowWidth) * shadowWidth;
        float shadowOriginY = ((shadowHeight + getOriginY()) / shadowHeight) * shadowHeight;

        if(style.shadowShader != null && style.shadowShader.isCompiled()){
            shadowShader = style.shadowShader;
            batch.setShader(shadowShader);
        }


        if(shadowShader != null){
            // calculates screen coord with origin on top left but frag screen coord origin is on bottom left
            localToScreenCoordinates(tmp.set(localShadowX, localShadowY));// bottom left
            tmp.y = Gdx.graphics.getHeight() - tmp.y;
            shadowShader.setUniformf("u_x", tmp.x);
            shadowShader.setUniformf("u_y", tmp.y);
            shadowShader.setUniformf("u_width", shadowWidth);
            shadowShader.setUniformf("u_height", shadowHeight);
            shadowShader.setUniformf("u_radius", shadowRadius);
            shadowShader.setUniformf("u_shadowSize", shadowSize);
            shadowShader.setUniformf("u_shadowAlpha", shadowColor.a);
        }

        float alpha = getColor().a * shadowColor.a * parentAlpha;
        batch.setColor(shadowColor.r, shadowColor.g, shadowColor.b, alpha);
        batch.draw(
                style.pixel,
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
        batch.setShader(null);
        Pools.free(tmp);
    }


    public static class UIStyle {
        /**
         * a white pixel used for drawing the shadow and background
         */
        public @Null TextureRegion pixel;
        public @Null ShaderProgram shadowShader;
        public @Null ShaderProgram backgroundShader;
        public final Color shadowColor = new Color(1, 1, 1, 1);//new Color(1, 1, 1, 0.55f);
        public final Color backgroundColor = new Color(1, 1, 1, 1);
        public int shadowSize = 0;//20;
        public int cornerRadius = 0;//10;

        public UIStyle(){}

        public UIStyle(UIStyle style){
            set(style);
        }

        public void set(UIStyle style){
            pixel = style.pixel;
            shadowShader = style.shadowShader;
            backgroundShader = style.backgroundShader;
            shadowColor.set(style.shadowColor);
            backgroundColor.set(style.backgroundColor);
            shadowSize = style.shadowSize;
            cornerRadius = style.cornerRadius;
        }
    }

}
