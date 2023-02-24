package com.sk.editor.ui.logger;

import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.utils.Logger;
import com.badlogic.gdx.utils.Null;

public class LoggerListener implements EventListener {

    @Override
    public boolean handle(Event event) {
        if(!(event instanceof LoggerEvent))return false;
        onLog((LoggerEvent) event);
        return true;
    }

    public void onLog(LoggerEvent event){}


    public static class LoggerEvent extends Event{
        private String tag;
        private int level = Logger.NONE;
        private String message = "", metaInfo = "";
        private Exception exception;

        public String getTag(){
            return tag;
        }

        protected void setTag(String tag){
            this.tag = tag;
        }

        /**
         *
         * @return the level of the logged message. I.e. {@link com.badlogic.gdx.utils.Logger#DEBUG}
         */
        public int getLevel() {
            return level;
        }

        protected void setLevel(int level) {
            this.level = level;
        }

        /**
         *
         * @return never null. Might be an empty string though
         */
        public String getMessage() {
            return message;
        }

        /**
         *
         * @param message if null ignores setting the message
         */
        protected void setMessage(String message) {
            if(message == null)return;
            this.message = message;
        }

        /**
         * additional custom information that can be set by the user
         * @return never null. Might be an empty string
         */
        public String getMetaInfo() {
            return metaInfo;
        }

        /**
         * additional custom information that can be set by the user
         * @param metaInfo if null ignores setting the meta information
         */
        protected void setMetaInfo(String metaInfo) {
            if(metaInfo == null)return;
            this.metaInfo = metaInfo;
        }

        /**
         *
         * @return Maybe null
         */
        public @Null Exception getException() {
            return exception;
        }

        /**
         *
         * @param exception nullable
         */
        protected void setException(@Null Exception exception) {
            this.exception = exception;
        }

        @Override
        public void reset() {
            super.reset();
            level = Logger.NONE;
            message = "";
            metaInfo = "";
            exception = null;
        }
    }



}
