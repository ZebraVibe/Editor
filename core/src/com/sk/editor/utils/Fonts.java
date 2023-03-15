package com.sk.editor.utils;

import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGeneratorLoader;
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.Logger;
import com.badlogic.gdx.utils.Null;
import com.sk.editor.assets.FontNames;

public class Fonts implements IAssetManagerHelper {
    private AssetManager manager;

    public Fonts(AssetManager manager) {
        this.manager = manager;
    }


    /**
     * @return Maybe null if not yet loaded to AssetManager
     */
    public @Null BitmapFont get(FontNames fontName, FontSize size) {
        return get(fontName.getIdentifier(size), BitmapFont.class, manager);
    }

    /**
     * If font not loaded yet loads it and holds the thread
     *
     * @param fontName
     * @param fontSize
     * @return Maybe null
     */
    public @Null BitmapFont getOrLoad(FontNames fontName, FontSize fontSize) {
        return getOrLoad(fontName.getIdentifier(fontSize), BitmapFont.class, manager, fontName, fontSize);
    }

    /**
     * loads the given font into the asset manager
     */
    public void loadToAssetManager(FontNames fontName, FontSize fontSize) {
        loadToAssetManager(fontName.getIdentifier(fontSize), BitmapFont.class, manager, fontName, fontSize);
    }

    public void loadAllFontSizesToAssetManager(FontNames fontName) {
        for (FontSize fontSize : FontSize.values()) loadToAssetManager(fontName, fontSize);
    }


    @Override
    public AssetLoaderParameters createAssetLoaderParameters(Object... assetLoaderParameterObjects) {
        FreetypeFontLoader.FreeTypeFontLoaderParameter parameter = new FreetypeFontLoader.FreeTypeFontLoaderParameter();
        if(assetLoaderParameterObjects == null)throw new GdxRuntimeException("Asset loader parameter objects can not be null.");
        for(Object obj : assetLoaderParameterObjects){
            // the internal path / file that needs to exist in asset dir
            if(obj instanceof FontNames) parameter.fontFileName = ((FontNames) obj).getPath();
            else if(obj instanceof FontSize) parameter.fontParameters.size = ((FontSize) obj).toInt();
        }
        return parameter;
    }

    @Override
    public void initLoaders(AssetManager manager) {
        FileHandleResolver resolver = new InternalFileHandleResolver(); // to resolve the file as internal
        manager.setLoader(FreeTypeFontGenerator.class, new FreeTypeFontGeneratorLoader(resolver));
        manager.setLoader(BitmapFont.class, ".ttf", new FreetypeFontLoader(resolver));
    }

    @Override
    public @Null GdxRuntimeException checkIfLoadersArePresent(AssetManager manager) {
        // check if loader is missing
        if (manager.getLoader(FreeTypeFontGenerator.class, ".ttf") == null)
            return new GdxRuntimeException("FreeTypeFontGeneratorLoader is missing!");
        if (manager.getLoader(BitmapFont.class, ".ttf") == null)
            return new GdxRuntimeException("FreetypeFontLoader is missing!");
        return null;
    }


}
