package com.sk.editor.utils;

import com.badlogic.gdx.graphics.Color;

public class ColorConverter {
    public static Color hexToRGBA8888(int hexRGB) {
        java.awt.Color jColor = new java.awt.Color(hexRGB);
        Color c = new Color(jColor.getRed() / 255f, jColor.getGreen() / 255f, jColor.getBlue() / 255f, 1);
        return c;
    }

    private ColorConverter() {}

}
