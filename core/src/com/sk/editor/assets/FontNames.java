package com.sk.editor.assets;

import com.sk.editor.utils.FontSize;

public enum FontNames {

    DEFAULT_FONT("Montserrat-Regular.ttf"),

    MONTSERRAT_REGULAR("Montserrat-Regular.ttf"),
    MONTSERRAT_MEDIUM("Montserrat-Medium.ttf"),
    MONTSERRAT_LIGHT("Montserrat-Light.ttf");

   FontNames(String fontFileNameWithExtension){
       int index = fontFileNameWithExtension.indexOf(".");
       name = fontFileNameWithExtension.substring(0, index);
       extension = fontFileNameWithExtension.substring(index, fontFileNameWithExtension.length());
   }
   String name, extension;

    /**
     *
     * @return the internal path
     */
    public String getPath() {
        return AssetPaths.FONTS_DIR + name + extension;
    }

    /**
     *
     * @return the regular font name without extension
     */
   public String getRegularName(){
        return name;
   }

   public String getExtension(){
       return extension;
   }

    /**
     *  To be used by asset manager. The extension might be needed for the asset manager to now
     *  which loader to use
     * @return name = {@link #getRegularName()} + fontSize + {@link #getExtension()}
     */
    public String getIdentifier(FontSize fontSize){
        return name + fontSize.toInt() + extension;
    }


}
