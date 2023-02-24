package com.sk.editor.world.systems;

import com.artemis.BaseEntitySystem;
import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.artemis.annotations.All;
import com.artemis.utils.IntBag;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.sk.editor.config.Config;
import com.sk.editor.ecs.ECSManager;
import com.sk.editor.world.components.Transform;

@All(Transform.class)
public class DebugSystem extends BaseEntitySystem {

    public final Color
            DEBUG_COLOR = Config.DARKEST_GRAY.cpy(),
            SELECTED_COLOR = Config.GREEN.cpy(),
            tmpColor = new Color();

    private ComponentMapper<Transform> transformMapper;

    private ShapeRenderer renderer;
    private Viewport viewport;
    private ECSManager ecsManager;


    public boolean debug = true;

    public DebugSystem(ShapeRenderer renderer, Viewport viewport, ECSManager ecsManager) {
        this.renderer = renderer;
        this.viewport = viewport;
        this.ecsManager = ecsManager;
    }

    @Override
    protected void processSystem() {
        IntBag actives = getEntityIds();
        int[] ids = actives.getData();

        renderer.setProjectionMatrix(viewport.getCamera().combined);
        renderer.setAutoShapeType(true);
        renderer.begin();

        for (int i = 0; i < ids.length; i++) {
            process(ids[i]);
        }
        renderer.end();
    }

    private void process(int entityId) {
        renderer.flush();
        tmpColor.set(renderer.getColor());

        Transform transform = transformMapper.getSafe(entityId, null);
        if (transform == null) return;

        Entity selected = ecsManager.getSelectedEntity();
        // set debug color
        if (selected != null && entityId == selected.getId()){
            renderer.setColor(SELECTED_COLOR);
        } else renderer.setColor(DEBUG_COLOR);

            renderer.rect(
                    transform.x ,
                    transform.y,
                    0,
                    0,
                    transform.width,
                    transform.height,
                    1f,
                    1f,
                    0
            );

        renderer.flush();
        renderer.setColor(tmpColor);

    }

}
