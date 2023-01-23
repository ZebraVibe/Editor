package com.sk.editor.ui;

import com.artemis.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Logger;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.Pools;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.sk.editor.world.components.Transform;

public class InputHandlerActor extends Table {

    private static final Logger log = new Logger(InputHandlerActor.class.toString(), Logger.DEBUG);

    private final int RIGHT_MOUSE_BUTTON = Input.Buttons.RIGHT;
    private final int LEFT_MOUSE_BUTTON = Input.Buttons.LEFT;
    private OrthographicCamera ecsCamera;
    private Viewport ecsViewport;
    private ECSHandler ecsHandler;

    public InputHandlerActor(Viewport uiViewport, Viewport ecsViewport, ECSHandler ecsHandlerActor){
        this.ecsViewport = ecsViewport;
        this.ecsCamera = (OrthographicCamera)(ecsViewport.getCamera());
        this.ecsHandler = ecsHandlerActor;
        initListeners();
    }

    private void initListeners() {
        initECSInputListener();
        initECSCameraListener();
    }


    private void initECSInputListener(){
        // scene / World listener
        InputListener inputListener = new InputListener(){

            Entity dragged;
            Vector2 delta = new Vector2();

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if(button != LEFT_MOUSE_BUTTON)return false;

                Vector2 tmp = Pools.obtain(Vector2.class);
                // get mouse position
                tmp.set(Gdx.input.getX(), Gdx.input.getY());
                // screen to world coord
                ecsViewport.unproject(tmp);
                // check if is hitting an entity
                Entity hit = ecsHandler.hitWorld(tmp.x, tmp.y);
                if(hit != null){
                    Entity selected = ecsHandler.getSelectedEntity();
                    // 2 clicks to drag
                    if(selected != null && hit == selected){
                        dragged = hit;
                        Transform transform = ecsHandler.getTransformMapper().get(selected);
                        delta.set(transform.x - tmp.x, transform.y - tmp.y);
                    }
                }
                //selected = hit;
                ecsHandler.setSelectedEntity(hit);
                Pools.free(tmp);
                return hit != null;
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                if(dragged == null)return;
                Entity selected = ecsHandler.getSelectedEntity();
                Transform transform = ecsHandler.getTransformMapper().get(selected);
                Vector2 tmp = Pools.obtain(Vector2.class);
                // get mouse position
                tmp.set(Gdx.input.getX(), Gdx.input.getY());
                // screen to world coord
                ecsViewport.unproject(tmp);
                // calc drag position of transform
                tmp.add(delta);
                transform.setPosition(tmp.x, tmp.y);

                Pools.free(tmp);
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                dragged = null;
            }

        };
        addListener(inputListener);
    }

    private void initECSCameraListener() {
        // camera input listener
        InputListener cameraListener = new InputListener(){
            private float lastMouseX, lastMouseY;

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
                float dx = (lastMouseX - mouseX) * ecsCamera.zoom;
                float dy = (lastMouseY - mouseY) * ecsCamera.zoom;

                ecsCamera.position.add(dx, -dy, 0);
                // camera notifies listeners. update canvases in here
                ecsCamera.update(); // cam in different stage

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
                screenCamPre.set(ecsCamera.position.x, ecsCamera.position.y);
                ecsViewport.project(screenCamPre);
                screenDelta.set(screenTouch);
                screenDelta.sub(screenCamPre);

                float maxZoom = 10;//5; // zoom out
                float minZoom = 1/400f; //1 / 50f; // zoom in
                final float zoom = MathUtils.clamp(ecsCamera.zoom * factor, minZoom, maxZoom);
                ecsCamera.zoom = zoom;
                ecsViewport.unproject(worldTouch.set(screenTouch));
                ecsCamera.position.set(worldTouch, 0);
                ecsCamera.position.sub(screenDelta.x * zoom, -screenDelta.y * zoom, 0);
                // camera notifies listeners. update canvases in here
                ecsCamera.update();

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
                event.getStage().setScrollFocus(InputHandlerActor.this);
            }
        };
        addListener(cameraListener);
    }




}
