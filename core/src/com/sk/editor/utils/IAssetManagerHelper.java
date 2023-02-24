package com.sk.editor.utils;

import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.Null;

/**
 * Asset Manager helping class
 */
public interface IAssetManagerHelper {

    /**
     * to be called in {@link com.sk.editor.screens.LoadingScreen} before loading anything
     * @param manager
     */
    void initLoaders(AssetManager manager);

    /**
     * checks if all loaders are present
     * @param manager
     * @return the exception to throw if a loader is missing
     */
    GdxRuntimeException checkIfLoadersArePresent(AssetManager manager);

    default @Null <T> T get(String key, Class<T> type, AssetManager manager) {
        return manager.get(key, type);
    }
    default @Null <T> T getOrLoad(String key, Class<T> type, AssetManager manager, AssetLoaderParameters parameters) {
        if(manager.contains(key) == false){
            loadToAssetManager(key, type, manager, parameters);
            manager.finishLoadingAsset(key);
        }
        return get(key, type, manager);
    }



    default <T> void loadToAssetManager(String key, Class<T> typeToLoad, AssetManager manager, @Null AssetLoaderParameters parameters){
        GdxRuntimeException e =  checkIfLoadersArePresent(manager);
        if(e != null)throw e;
        if (manager.contains(key)) return;
        manager.load(key, typeToLoad, parameters);
    }

}
