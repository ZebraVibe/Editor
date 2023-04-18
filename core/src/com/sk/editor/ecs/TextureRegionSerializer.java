package com.sk.editor.ecs;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.Logger;

/**
 * all to be de/-serialized texture regions must be loaded to the assetManager
 */
public class TextureRegionSerializer implements Json.Serializer<TextureRegion> {

    private static final Logger log = new Logger(TextureRegionSerializer.class.toString(), Logger.DEBUG);

    AssetManager assetManager;

    public TextureRegionSerializer(AssetManager assetManager){
        this.assetManager = assetManager;
    }

    @Override
    public void write(Json json, TextureRegion object, Class knownType) {
        boolean hasAtlas = object instanceof TextureAtlas.AtlasRegion;
        json.writeObjectStart();
        json.writeValue("hasAtlas", hasAtlas);
        json.writeValue("texture", object.getTexture().toString());

        // atlas region
        if(hasAtlas){
            TextureAtlas.AtlasRegion atlasRegion = (TextureAtlas.AtlasRegion)object;
            json.writeValue("name", atlasRegion.name);
            json.writeValue("index", atlasRegion.index);

        } else { // texture region (maybe sprite)
            json.writeValue("u", object.getU());
            json.writeValue("v", object.getV());
            json.writeValue("u2", object.getU2());
            json.writeValue("v2", object.getV2());
            json.writeValue("regionWidth", object.getRegionWidth());
            json.writeValue("regionHeight", object.getRegionHeight());
        }
        json.writeObjectEnd();
    }

    @Override
    public TextureRegion read(Json json, JsonValue jsonData, Class type) {

        boolean hasAtlas = json.readValue("hasAtlas", boolean.class, jsonData);
        String texturePath = json.readValue("texture", String.class, jsonData);

        // atlas region
        if(hasAtlas){
            String name = json.readValue("name", String.class, jsonData);
            int index = json.readValue("index", int.class, jsonData);
            TextureAtlas atlas = assetManager.get(texturePath, TextureAtlas.class, true);
            return atlas.findRegion(name, index);
        }

        // texture region (maybe sprite)
        float u = json.readValue("u", float.class, jsonData);
        float v = json.readValue("v", float.class, jsonData);
        float u2 = json.readValue("u2", float.class, jsonData);
        float v2 = json.readValue("v2", float.class, jsonData);
        int regionWidth = json.readValue("regionWidth", int.class, jsonData);
        int regionHeight = json.readValue("regionHeight", int.class, jsonData);

        TextureRegion region = new TextureRegion();
        //textures and atlases have to be loaded to the asset manager
        region.setTexture(assetManager.get(texturePath, Texture.class, true));
        region.setRegion(u, v, u2, v2);
        region.setRegionWidth(regionWidth);
        region.setRegionHeight(regionHeight);
        return region;
    }



}
