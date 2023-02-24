package com.sk.editor.ui;

import com.artemis.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.sk.editor.ecs.ECSManager;
import com.sk.editor.ui.inspector.Inspector;
import com.sk.editor.world.components.Transform;

public class InputHandlerActor extends Table {

    private static final Logger log = new Logger(InputHandlerActor.class.toString(), Logger.DEBUG);

    private final int RIGHT_MOUSE_BUTTON = Input.Buttons.RIGHT;
    private final int LEFT_MOUSE_BUTTON = Input.Buttons.LEFT;
    private Stage ecsStage, uiStage;
    private OrthographicCamera ecsCamera;
    private Viewport ecsViewport, uiViewport;
    private ECSManager ecsManager;
    private @Null Inspector hasInspector;

    public InputHandlerActor(Stage uiStage, Stage ecsStage, ECSManager ecsManager) {
        this.uiStage = uiStage;
        this.uiViewport = uiStage.getViewport();
        this.ecsStage = ecsStage;
        this.ecsViewport = ecsStage.getViewport();
        this.ecsCamera = (OrthographicCamera) (ecsViewport.getCamera());
        this.ecsManager = ecsManager;
        initListeners();
    }

    private void initListeners() {
        initECSInputListener();
        initECSCameraListener();
    }


    private void initECSInputListener() {
        // scene / World listener
        InputListener inputListener = new InputListener() {

            Vector2 delta = new Vector2();

            Entity prevSelected, dragged;
            boolean isDragging;

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (button != LEFT_MOUSE_BUTTON) return false;
                isDragging = false;

                Vector2 tmp = Pools.obtain(Vector2.class);
                // get mouse position
                tmp.set(Gdx.input.getX(), Gdx.input.getY());
                // screen to world coord
                ecsViewport.unproject(tmp);
                // check if is hitting an entity
                Entity hit = ecsManager.hitWorld(tmp.x, tmp.y);
                if (hit != null) {
                    Entity currentSelected = ecsManager.getSelectedEntity();
                    // 2 clicks to drag
                    if (currentSelected != null && hit == currentSelected) {
                        dragged = hit;
                        Transform transform = ecsManager.getTransformMapper().get(currentSelected);
                        delta.set(transform.x - tmp.x, transform.y - tmp.y);
                    }
                }
                Pools.free(tmp);


                prevSelected = ecsManager.getSelectedEntity();
                ecsManager.setSelectedEntity(hit);

                // has hit
                if (hit != null) return true;
                // has no hit
                setInspectorVisible(false);
                return false;

            }


            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                if (dragged == null) return;
                isDragging = true;
                Entity lastSelected = ecsManager.getSelectedEntity();
                Transform transform = ecsManager.getTransformMapper().get(lastSelected);
                Vector2 tmp = Pools.obtain(Vector2.class);
                // get mouse position
                tmp.set(Gdx.input.getX(), Gdx.input.getY());
                // screen to world coord
                ecsStage.screenToStageCoordinates(tmp);
                // calc drag position of transform
                tmp.add(delta);
                transform.setPosition(tmp.x, tmp.y);

                // update transform inspector information
                Inspector inspector = getInspector();
                inspector.updateWidgetValue(Transform.class, "x", transform.x);
                inspector.updateWidgetValue(Transform.class, "y", transform.y);


                // make inspector translucent while being dragged
                setInspectorAlpha(0.5f);
                inspector.updateInspectorPositionRelativeTo(lastSelected, ecsStage);

                Pools.free(tmp);
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                // selected entity is here always != null
                Entity selected = ecsManager.getSelectedEntity();
                boolean hasSelectedNotChanged = prevSelected == selected;
                boolean hasNotDragged = !isDragging;

                //toggle inspector visibility
                if(hasSelectedNotChanged){
                    if(hasNotDragged){
                        //toggleInspectorVisibility();
                        boolean visible = getInspector().isVisible();
                        showInspector(!visible, selected, prevSelected);
                    }

                } else showInspector(true, selected, prevSelected);

                // undo dragging 0.5 alpha
                if(isDragging)setInspectorAlpha(1);

                dragged = null;
                isDragging = false;
            }


        };
        addListener(inputListener);
    }

    private void initECSCameraListener() {
        // camera input listener
        InputListener cameraListener = new InputListener() {
            private float lastMouseX, lastMouseY;

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (button == RIGHT_MOUSE_BUTTON) {
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

                //update inspector
                Inspector inspector = getInspector();
                inspector.updateInspectorPositionRelativeTo(ecsManager.getSelectedEntity(), ecsStage);

                lastMouseX = mouseX;
                lastMouseY = mouseY;
            }

            @Override
            public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY) {
                if (amountY == 0) return false;

                float factor;
                if (amountY < 0) factor = (float) Math.pow(3 / 4f, Math.abs(amountY)); // zoom in
                else factor = (float) Math.pow(4 / 3f, Math.abs(amountY)); // zoom out

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
                float minZoom = 1 / 400f; //1 / 50f; // zoom in
                final float zoom = MathUtils.clamp(ecsCamera.zoom * factor, minZoom, maxZoom);
                ecsCamera.zoom = zoom;
                ecsViewport.unproject(worldTouch.set(screenTouch));
                ecsCamera.position.set(worldTouch, 0);
                ecsCamera.position.sub(screenDelta.x * zoom, -screenDelta.y * zoom, 0);
                // camera notifies listeners. update canvases in here
                ecsCamera.update();

                //updateCanvasCameras(); // update canvases in listener
                Inspector inspector = getInspector();
                inspector.updateInspectorPositionRelativeTo(ecsManager.getSelectedEntity(), ecsStage);


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


    // -- ui elements --
    private Inspector getInspector() {
        if (hasInspector != null) return hasInspector;
        hasInspector = uiStage.getRoot().findActor(UINames.INSPECTOR);
        return hasInspector;
    }

    /**
     * makes inspector visible, sets alpha to 1, repositions it and
     * refreshes it if needed
     *
     * @param show      if false hides the inspector
     * @param entity gets ignored if show is false
     */
    private void showInspector(boolean show, Entity entity, Entity previous) {
        Inspector inspector = getInspector();
        setInspectorVisible(show);
        setInspectorAlpha(1);
        if (show == false) return;

        // fill inspector;
        //inspector.fill(inspected);
        inspector.update(entity, previous);
        inspector.updateInspectorPositionRelativeTo(entity, ecsStage);

    }

    private void setInspectorVisible(boolean visible){
        getInspector().setVisible(visible);
    }

    private void setInspectorAlpha(float alpha) {
        Inspector inspector = getInspector();
        inspector.getColor().a = alpha;
    }

}
