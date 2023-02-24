package com.sk.editor.utils;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGeneratorLoader;
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.Logger;
import com.badlogic.gdx.utils.Null;
import com.sk.editor.assets.FontNames;

public class Fonts implements IAssetManagerHelper {

    private final Logger log = new Logger(Fonts.class.getName(), Logger.DEBUG);
    private AssetManager manager;
    private boolean initLoader;

    public Fonts(AssetManager manager) {
        this.manager = manager;
    }

    // -- font --

    private FreeTypeFontGenerator createFontGenerator(FileHandle fontFile) {
        return new FreeTypeFontGenerator(fontFile);
    }


    private BitmapFont createFont(FileHandle fontFile, FreeTypeFontParameter param) {
        FreeTypeFontGenerator gen = createFontGenerator(fontFile);
        BitmapFont font = gen.generateFont(param);
        gen.dispose();
        return font;
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
        /*
        if(manager.contains(fontName.getIdentifier(size)) == false){
            loadToAssetManager(fontName, size);
            manager.finishLoadingAsset(fontName.getIdentifier(size));
        }
        return get(fontName, size);*/

        // create parameters
        FreetypeFontLoader.FreeTypeFontLoaderParameter parameter = new FreetypeFontLoader.FreeTypeFontLoaderParameter();
        parameter.fontFileName = fontName.getPath(); // the internal path / file that needs to exist in asset dir
        parameter.fontParameters.size = fontSize.toInt();
        return getOrLoad(fontName.getIdentifier(fontSize), BitmapFont.class, manager, parameter);
    }


    /**
     * loads the given font into the asset manager
     */
    public void loadToAssetManager(FontNames fontName, FontSize fontSize) {

        /*
        // already loaded
        String loadedName = fontName.getIdentifier(fontSize);
        if (manager.contains(loadedName))return;

        if(initLoader == false){
            FileHandleResolver resolver = new InternalFileHandleResolver(); // to resolve the file as internal
            manager.setLoader(FreeTypeFontGenerator.class, new FreeTypeFontGeneratorLoader(resolver));
            manager.setLoader(BitmapFont.class, ".ttf", new FreetypeFontLoader(resolver));
            initLoader = true;
        }*/

        /*
        FreetypeFontLoader.FreeTypeFontLoaderParameter parameter = new FreetypeFontLoader.FreeTypeFontLoaderParameter();
        parameter.fontFileName = fontName.getPath(); // the internal path / file that needs to exist in asset dir
        parameter.fontParameters.size = fontSize.toInt();
        String key = fontName.getIdentifier(fontSize);
        manager.load(key, BitmapFont.class, parameter);
        */


        // create loader parameter
        FreetypeFontLoader.FreeTypeFontLoaderParameter parameter = new FreetypeFontLoader.FreeTypeFontLoaderParameter();
        parameter.fontFileName = fontName.getPath(); // the internal path / file that needs to exist in asset dir
        parameter.fontParameters.size = fontSize.toInt();
        loadToAssetManager(fontName.getIdentifier(fontSize), BitmapFont.class, manager, parameter);
    }

    public void loadAllFontSizesToAssetManager(FontNames fontName) {
        for (FontSize fontSize : FontSize.values()) {
            loadToAssetManager(fontName, fontSize);
        }
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
