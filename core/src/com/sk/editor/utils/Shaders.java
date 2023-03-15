package com.sk.editor.utils;

import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.ShaderProgramLoader;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.sk.editor.assets.FontNames;
import com.sk.editor.assets.ShaderNames;

public class Shaders implements IAssetManagerHelper{


    private AssetManager manager;

    public Shaders(AssetManager manager) {
        this.manager = manager;
    }



    public ShaderProgram get(ShaderNames name){
        return get(name.getName(), ShaderProgram.class, manager);
    }

    public ShaderProgram getOrLoad(ShaderNames name) {
        return getOrLoad(name.getName(), ShaderProgram.class, manager, name);
    }

    public void loadToAssetManager(ShaderNames name) {
        loadToAssetManager(name.getName(), ShaderProgram.class, manager, name);
    }

    public void loadAllShadersToAssetManager(){
        for(ShaderNames name : ShaderNames.values())loadToAssetManager(name);
    }



    @Override
    public AssetLoaderParameters createAssetLoaderParameters(Object... assetLoaderParameterObjects) {
        ShaderProgramLoader.ShaderProgramParameter parameter = new ShaderProgramLoader.ShaderProgramParameter();
        if (assetLoaderParameterObjects == null)
            throw new GdxRuntimeException("Asset loader parameter objects can not be null.");
        for (Object obj : assetLoaderParameterObjects) {
            // the internal path / file that needs to exist in asset dir
            if (obj instanceof ShaderNames) {
                parameter.vertexFile = ((ShaderNames) obj).getVertexPath();
                parameter.fragmentFile = ((ShaderNames) obj).getFragmentPath();
            }
        }
        return parameter;
    }

    /**
     * assumes the shaders have ".vert" and ".frag" extensions
     * @param manager
     */
    @Override
    public void initLoaders(AssetManager manager) {
        FileHandleResolver resolver = new InternalFileHandleResolver();
        ShaderProgramLoader loader = new ShaderProgramLoader(resolver);
        manager.setLoader(ShaderProgram.class, loader);
    }

    @Override
    public GdxRuntimeException checkIfLoadersArePresent(AssetManager manager) {
        if(manager.getLoader(ShaderProgram.class) == null) return new GdxRuntimeException("ShaderProgramLoader is missing");
        return  null;
    }
}
