package com.sk.editor.ecs.systems;

import com.artemis.BaseEntitySystem;
import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.artemis.annotations.All;
import com.artemis.utils.Bag;
import com.artemis.utils.IntBag;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.sk.editor.EditorManager;
import com.sk.editor.ecs.components.Canvas;
import com.sk.editor.ecs.components.Transform;
import com.sk.editor.ui.NotifyingOrthographicCamera;

@All({Transform.class, Canvas.class})
public class CanvasSystem extends BaseEntitySystem implements NotifyingOrthographicCamera.CameraListener {


    private static final Transform.TransformComparator comparator = new Transform.TransformComparator();


    ComponentMapper<Canvas> canvasMapper;
    ComponentMapper<Transform> transformMapper;
    Bag<Entity> tmpBag = new Bag<>();


    private EditorManager editorManager;
    private Viewport ecsViewport;

    public CanvasSystem(EditorManager editorManager, Viewport ecsViewport) {
        this.editorManager = editorManager;
        this.ecsViewport = ecsViewport;
    }


    @Override
    protected void processSystem() {
        getRootCanvases(tmpBag);

        // process canvases
        for (Entity e : tmpBag) {
            processRootCanvas(e.getId(), transformMapper.get(e), canvasMapper.get(e));
        }
        tmpBag.clear();
    }

    /**
     * Applies the viewport of the root canvas, then processes it and its children.Override this to set up gl and the batch using the viewport
     *
     * @param entityId  the canvas-entity id
     * @param transform the transform belonging to the canvas-entityId
     * @param canvas    the canvas belonging to the canvas-entityId
     */
    protected void processRootCanvas(int entityId, Transform transform, Canvas canvas) {
        canvas.apply();
        process(entityId);
    }

    /**
     * handles sorting and processing children
     *
     * @param entityId
     */

    protected void process(int entityId) {
        Transform transform = transformMapper.get(entityId);

        // children
        if (transform.hasChildren()) {

            // sort if needed : entities are also sorted for other systems
            if (transform.childrenChanged()) transform.sortChildren();

            // process children
            for (Entity child : transform.getChildren()) {
                process(child.getId());
            }
        }
    }


    @Override
    protected void inserted(int entityId) {
        Canvas canvas = canvasMapper.get(entityId);
        updateCanvas(canvas);
    }


    private void updateCanvas(Canvas canvas) {
        canvas.updateViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), editorManager.getRenderMode(), ecsViewport);
    }

    @Override
    public void updated(OrthographicCamera camera) {
        IntBag actives = getEntityIds();
        int[] ids = actives.getData();

        // update canvases
        for (int i = 0, s = actives.size(); s > i; i++) {
            Canvas canvas = canvasMapper.get(ids[i]);
            updateCanvas(canvas);
        }
    }

    /**
     * Fills the given bag sorted with the currently existing root canvases.
     *
     * @param emptyBag an empty bag
     * @return the given bag filled with the current root canvases
     */
    public Bag<Entity> getRootCanvases(Bag<Entity> emptyBag) {
        IntBag actives = getEntityIds();
        int[] ids = actives.getData();

        // sort canvases (typically just a hand full)
        for (int i = 0, s = actives.size(); s > i; i++) {
            int id = ids[i];
            Transform transform = transformMapper.get(id);
            if(transform.isRoot())emptyBag.add(getWorld().getEntity(id));
        }
        emptyBag.sort(comparator);
        return emptyBag;
    }

}