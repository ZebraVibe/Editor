package com.sk.editor.utils;

import com.badlogic.gdx.files.FileHandle;
import com.sk.editor.config.Config;

public class FileUtils {

    private FileUtils(){}

    public static boolean isFont(FileHandle file) {
        return hasExtension(file, "ttf");
    }

    public static boolean isImage(FileHandle file) {
        return hasExtension(file, "png","jpg","jpeg");
    }

    public static boolean isAudio(FileHandle file) {
        return hasExtension(file, "mp3","wav","OGG");
    }

    /**returns true if the file extension equals one of the given extensions*/
    public static boolean hasExtension(FileHandle file, String ...extensions) {
        String fileExtension = file.extension().toLowerCase();
        if(extensions != null&& file != null)
            for(String ext : extensions)
                if(ext != null && fileExtension.equals(ext))return true;
        return false;
    }

}
