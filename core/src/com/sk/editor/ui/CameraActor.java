package com.sk.editor.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.viewport.Viewport;

public class CameraActor extends Table {

    private static final Logger log = new Logger(CameraActor.class.toString(), Logger.DEBUG);
    private final int RIGHT_MOUSE_BUTTON = Input.Buttons.RIGHT;
    private OrthographicCamera camera;
    private Viewport sceneViewport;

    private float lastMouseX, lastMouseY;

    public CameraActor(Viewport sceneViewport){
        this.sceneViewport = sceneViewport;
        this.camera = (OrthographicCamera)(sceneViewport.getCamera());
        initListeners();
    }

    private void initListeners() {
        // camera input listener
        InputListener cameraListener = new InputListener(){
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if(button == RIGHT_MOUSE_BUTTON){
                    lastMouseX = Gdx.input.getX();
                    lastMouseY = Gdx.input.getY();
                    return true;
                }
                return false;
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                float mouseX = Gdx.input.getX();
                float mouseY = Gdx.input.getY();
                float dx = (lastMouseX - mouseX) * camera.zoom;
                float dy = (lastMouseY - mouseY) * camera.zoom;

                camera.position.add(dx, -dy, 0);
                // camera notifies listeners. update canvases in here
                camera.update(); // cam in different stage

                lastMouseX = mouseX;
                lastMouseY = mouseY;
            }

            @Override
            public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY) {
                if (amountY == 0)return false;

                float factor;
                if(amountY < 0)factor = (float) Math.pow(3/4f , Math.abs(amountY)); // zoom in
                else factor = (float) Math.pow(4/3f , Math.abs(amountY)); // zoom out

                Pool<Vector2> pool = Pools.get(Vector2.class);
                Vector2 screenTouch = pool.obtain();
                Vector2 worldTouch = pool.obtain();
                Vector2 screenCamPre = pool.obtain();
                Vector2 screenDelta = pool.obtain();

                // scene viewport
                screenTouch.set(Gdx.input.getX(), Gdx.input.getY());
                screenCamPre.set(camera.position.x, camera.position.y);
                sceneViewport.project(screenCamPre);
                screenDelta.set(screenTouch);
                screenDelta.sub(screenCamPre);

                float maxZoom = 5; // zoom out
                float minZoom = 1 / 50f; // zoom in
                final float zoom = MathUtils.clamp(camera.zoom * factor, minZoom, maxZoom);
                camera.zoom = zoom;
                sceneViewport.unproject(worldTouch.set(screenTouch));
                camera.position.set(worldTouch, 0);
                camera.position.sub(screenDelta.x * zoom, -screenDelta.y * zoom, 0);
                // camera notifies listeners. update canvases in here
                camera.update();

                //updateCanvasCameras();



                pool.free(screenTouch);
                pool.free(worldTouch);
                pool.free(screenCamPre);
                pool.free(screenDelta);
                Pools.free(pool);
                return true;

            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                event.getStage().setScrollFocus(CameraActor.this);
            }
        };
        addListener(cameraListener);
    }


}
