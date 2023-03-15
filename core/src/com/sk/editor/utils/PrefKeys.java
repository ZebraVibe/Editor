package com.sk.editor.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.Logger;
import com.badlogic.gdx.utils.Null;
import com.sk.editor.config.Config;

public class PrefKeys{
    private final Preferences PREFS = Gdx.app.getPreferences(Config.EDITOR_PREFERENCES);
    public final PrefKey<String> SRC_PATH = newKey("srcPath", String.class);
    public final PrefKey<String> CLASS_PATH = newKey("classPath", String.class);
    public final PrefKey<String> PACKAGE_NAME = newKey("packageName", String.class);
    public final PrefKey<String> GPT3_API_KEY = newKey("gpt3APIKey", String.class);
    public final PrefKey<String> GPT_3_CONVERSATION_ID = newKey("gpt3ConvID", String.class);


    // -- methods --

    private <T> PrefKey<T> newKey(String key, Class<T> type){
        if(PREFS == null)throw new GdxRuntimeException("prefs null");
        return new PrefKey<>(key, type, PREFS);
    }

    /**
     * constants must be appendable with and integer nd stay unique
     * @param <T> must be of type String, Boolean, Integer, Long or Float
     */
    public static class PrefKey<T>{

        private Preferences prefs;
        private String key;
        private Class<T> type;

        public PrefKey(String key, Class<T> type, Preferences prefs){
            this.key = key;
            this.type = type;
            this.prefs = prefs;
        }

        /**
         * @param appendix to append the type with. < 0 to indicate no suffix
         * @return Maybe null. Else the value set in the preferences
         * @throws GdxRuntimeException
         */
        public T get(int appendix) throws GdxRuntimeException{
            String prefKey = key + (appendix < 0 ? "" : "" + appendix);
            Object value = null;
            try {
                if(type == Boolean.class)value = prefs.getBoolean(prefKey);
                else if(type == String.class)value = prefs.getString(prefKey);
                else if(type == Long.class)value = prefs.getLong(prefKey);
                else if(type == Integer.class)value = prefs.getInteger(prefKey);
                else if(type == Float.class)value = prefs.getFloat(prefKey);
                else throw new GdxRuntimeException("PrefsType must be of type String, boolean, long, int or float.");
            }catch (Exception e){
                throw new GdxRuntimeException("Could not get value from preferences.", e);
            }
            return (T)value;
        }

        public T get() throws GdxRuntimeException{
            return get(-1);
        }


        /**
         * @param value
         * @param appendix to append the type with. < 0 to indicate no suffix
         * @throws GdxRuntimeException
         */
        public void set(@Null int appendix, T value) throws GdxRuntimeException {
            String prefKey = key + (appendix < 0 ? "" : appendix);
            try {
                if(type == Boolean.class) prefs.putBoolean(prefKey, (boolean) value);
                else if(type == String.class) prefs.putString(prefKey, (String)value);
                else if(type == Long.class) prefs.putLong(prefKey, (long)value);
                else if(type == Integer.class) prefs.putInteger(prefKey, (int)value);
                else if(type == Float.class) prefs.putFloat(prefKey, (float)value);
                else throw new GdxRuntimeException("PrefsType must be of type String, boolean, long, int or float.");
            }catch (Exception e){
                throw new GdxRuntimeException("Could not set value to preferences.", e);
            }
            prefs.flush();
        }


        /**
         * @param value
         * @throws GdxRuntimeException
         */
        public void set(T value) throws GdxRuntimeException {
            set(-1, value);
        }
    }


}
