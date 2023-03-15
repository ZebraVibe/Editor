package com.sk.editor.assets;

public enum ShaderNames {


    CORNER("corner"),

    CORNER_AND_SHADOW("cornerAndShadow");

    /**
     *
     * @param shaderFileNameWithoutExtension assumed to be used for both .frag and .vert shader: i.e. "defaultShader"
     *                                       for"defaultShader.vert" and "defaultShader.frag"
     */
    ShaderNames(String shaderFileNameWithoutExtension){
        name = shaderFileNameWithoutExtension;
    }
    String name;

    /**
     * @return the internal path of the ".vert" file of the shader
     */
    public String getVertexPath() {
        return AssetPaths.SHADERS_DIR + name + ".vert";
    }

    /**
     * @return the internal path of the ".frag file" of the shader
     */
    public String getFragmentPath() {
        return AssetPaths.SHADERS_DIR + name + ".frag";
    }

    /**
     *
     * @return the name of the shader
     */
    public String getName() {
        return name;
    }
}
