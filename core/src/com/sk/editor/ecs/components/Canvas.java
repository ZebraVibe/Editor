package com.sk.editor.ecs.components;

import com.artemis.Component;
import com.artemis.Entity;
import com.artemis.PooledComponent;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Logger;
import com.badlogic.gdx.utils.SnapshotArray;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.sk.editor.EditorManager;
import com.sk.editor.ui.UIStage;
import com.sk.editor.ui.logger.EditorLogger;
import com.sk.editor.utils.RenderMode;

public class Canvas extends Script{

    private transient static final EditorLogger log = new EditorLogger(Canvas.class.toString(), Logger.DEBUG);

    private transient Viewport viewport;

    /**
     * whether the camera should be centered on resize
     */
    public boolean centerCamera;
    /**
     * the viewports units per pixel
     */
    public float uppX = 1, uppY = 1;

    public Canvas(){
        this.viewport = createViewport();
    }


    // -- viewport --
    private Viewport createViewport(){
        Viewport viewport = new ScreenViewport();
        return viewport;
    }

    /**
     * sets the viewports' units per pixel
     * @param uppX
     * @param uppY
     */
    public void setUnitsPerPixel(float uppX, float uppY){
        this.uppX = uppX;
        this.uppY = uppY;
    }

    public void updateViewport(int width, int height, RenderMode mode, Viewport ecsViewport){
        if(mode.isEdit()) {
            OrthographicCamera ecsCamera = (OrthographicCamera) ecsViewport.getCamera();
            OrthographicCamera worldCamera = (OrthographicCamera) viewport.getCamera();
            worldCamera.position.set(ecsCamera.position);
            worldCamera.position.scl(uppX, uppY, 1);
            worldCamera.zoom = ecsCamera.zoom;
            worldCamera.update();

            // screen viewport update logic
            viewport.setScreenBounds(0, 0, width, height);
            viewport.setWorldSize(width * uppX, height * uppY);
            viewport.apply(false);

        } else { // also game mode
            log.info("No viewport update logic configured yet for RenderMode." + mode.name());
            viewport.update(width, height, centerCamera);
        }
    }

    /**
     * calls {@link #apply(boolean)} with false
     */
    public void apply(){
        apply(false);
    }
    /**
     * applies the viewport
     * @param centerCamera
     */
    public void apply(boolean centerCamera){
        viewport.apply(centerCamera);
    }

    /**
     * @param worldCoord
     * @return passed in vector transformed to screen coord
     */
    public Vector2 project(Vector2 worldCoord){
        viewport.project(worldCoord);
        worldCoord.y = viewport.getScreenHeight() - worldCoord.y;
        return worldCoord;
    }
    /**
     * @param screenCoord
     * @return passed in vector transformed to world coord
     */
    public Vector2 unproject(Vector2 screenCoord){
        return viewport.unproject(screenCoord);
    }

    /**
     * @return the combined projection and view matrix of the camera
     */
    public Matrix4 combined(){
        return viewport.getCamera().combined;
    }

    public int getScreenWidth(){
        return viewport.getScreenWidth();
    }
    public int getScreenHeight(){
        return viewport.getScreenHeight();
    }

}
