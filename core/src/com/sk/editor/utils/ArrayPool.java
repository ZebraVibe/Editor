package com.sk.editor.utils;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;



public class ArrayPool extends Pool<Array> {

    @Override
    protected Array newObject() {
        return new Array();
    }

    @Override
    protected void reset(Array object) {
        super.reset(object);
        object.clear();
    }
}
