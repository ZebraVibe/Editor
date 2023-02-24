package com.sk.editor.ui.logger;

import com.badlogic.gdx.utils.Logger;
import com.badlogic.gdx.utils.Null;
import com.badlogic.gdx.utils.Pools;
import com.badlogic.gdx.utils.SnapshotArray;

public class EditorLogger extends Logger {

    private static final Logger log = new Logger(EditorLogger.class.toString(), Logger.DEBUG);

    private static SnapshotArray<LoggerListener> listeners = new SnapshotArray<>();
    private final String tag;

    public EditorLogger(String tag, int level) {
        super(tag, level);
        this.tag = tag;
    }


    public static void addListener(LoggerListener listener){
        if(listener != null)listeners.add(listener);
    }

    public static boolean removeListener(LoggerListener listener){
        return listeners.removeValue(listener, true);
    }

    // -- private --

    private void notifyListeners(String message, @Null String metaInfo, int level) {
        notifyListeners(message, metaInfo, null, level);
    }

    private void notifyListeners(String message, int level) {
        notifyListeners(message, null, null, level);
    }

    private void notifyListeners(String message, Exception exception, int level) {
        notifyListeners(message, null, exception, level);
    }

    /**
     *
     * @param message nullable
     * @param metaInfo nullable
     * @param exception nullable
     */
    private void notifyListeners(@Null String message, @Null String metaInfo, @Null Exception exception, int level){
        LoggerListener.LoggerEvent event = Pools.obtain(LoggerListener.LoggerEvent.class);
        event.setTag(tag);
        event.setLevel(level);
        event.setMessage(message);
        event.setMetaInfo(metaInfo);
        event.setException(exception);

        Object[] items = listeners.begin();
        for(int i = 0, n = listeners.size; i < n; i++){
            LoggerListener item = (LoggerListener) items[i];
            item.onLog(event);
        }

        Pools.free(event);
    }


    // -- public --


    // debug
    @Override
    public void debug(String message) {
        if(getLevel() >= DEBUG)notifyListeners(message, Logger.DEBUG);
        super.debug(message);
    }

    @Override
    public void debug(String message, Exception exception) {
        if(getLevel() >= DEBUG)notifyListeners(message, exception, Logger.DEBUG);
        super.debug(message, exception);
    }

    public void debug(String message, String metaInfo, Exception exception) {
        if(getLevel() >= DEBUG)notifyListeners(message, metaInfo, exception, Logger.DEBUG);
        super.debug(message, exception);
    }

    // info

    @Override
    public void info(String message) {
        if(getLevel() >= INFO)notifyListeners(message, Logger.INFO);
        super.info(message);
    }

    @Override
    public void info(String message, Exception exception) {
        if(getLevel() >= INFO)notifyListeners(message, exception, Logger.INFO);
        super.info(message, exception);
    }

    public void info(String message, String metaInfo, Exception exception) {
        if(getLevel() >= INFO)notifyListeners(message, metaInfo, exception, Logger.INFO);
        super.info(message, exception);
    }

    // error

    @Override
    public void error(String message) {
        if(getLevel() >= ERROR)notifyListeners(message, Logger.ERROR);
        super.error(message);
    }

    @Override
    public void error(String message, Throwable exception) {
        if(getLevel() >= ERROR)notifyListeners(message, (Exception)exception, Logger.ERROR);
        super.error(message, exception);
    }

    public void error(String message, String metaInfo, Throwable exception) {
        if(getLevel() >= ERROR)notifyListeners(message, metaInfo, (Exception)exception, Logger.ERROR);
        super.error(message, exception);
    }
}
