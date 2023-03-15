package com.sk.editor.ui;

import com.badlogic.gdx.graphics.g2d.BitmapFontCache;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Null;

public class UILabel extends Label {
    public UILabel(@Null CharSequence text, Skin skin) {
        super(text, skin);
    }

    public UILabel (@Null CharSequence text, LabelStyle style) {
        super(text, style);
    }

    @Override
    public BitmapFontCache getBitmapFontCache() {
        return super.getBitmapFontCache();
    }
}
