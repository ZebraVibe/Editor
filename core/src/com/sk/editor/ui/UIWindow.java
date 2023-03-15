package com.sk.editor.ui;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.utils.Null;

public class UIWindow extends Window {

    private UIBase backgroundUI;
    private boolean drawBackgroundUI;

    public UIWindow(String title, Skin skin) {
        super(title, skin.get(UIWindowStyle.class));
        setSkin(skin);
        UIBase.UIStyle uiStyle = ((UIWindowStyle)getStyle()).uiStyle;

        // create uiBase background
        if(uiStyle != null){
            backgroundUI = new UIBase(skin){
                @Override
                protected void drawBackground(Batch batch, float parentAlpha, float x, float y) {
                    if(drawBackgroundUI)super.drawBackground(batch, parentAlpha, x, y);
                }
            };
            addActor(backgroundUI); // for scaling, rotation & positioning
        }

        setResizable(true);
        //setResizeBorder(8); // 8 is typically windows resize border - 8 by default
    }

    @Override
    protected void drawBackground(Batch batch, float parentAlpha, float x, float y) {
        // draw manually
        if(backgroundUI != null){
            drawBackgroundUI = true;
            backgroundUI.setPosition(x,y);
            backgroundUI.setSize(getWidth(), getHeight());
            backgroundUI.draw(batch, parentAlpha);
            drawBackgroundUI = false; // avoid drawing background again in children
        }
        super.drawBackground(batch, parentAlpha, x, y);
    }


    public static class UIWindowStyle extends WindowStyle{
        public @Null UIBase.UIStyle uiStyle;

        public UIWindowStyle(){}

        public UIWindowStyle(WindowStyle style){
            super(style);
            if(style instanceof UIWindowStyle){
                uiStyle = ((UIWindowStyle)style).uiStyle;
            }
        }

    }
}
