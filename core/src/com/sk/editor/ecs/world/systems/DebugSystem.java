package com.sk.editor.ecs.world.systems;

import com.artemis.BaseEntitySystem;
import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.artemis.annotations.All;
import com.artemis.utils.IntBag;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Null;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.sk.editor.config.Config;
import com.sk.editor.ecs.world.components.Transform;

@All(Transform.class)
public class DebugSystem extends BaseEntitySystem {

    public final Color
            DEBUG_COLOR = Config.DARKEST_GRAY.cpy(),
            SELECTED_COLOR = Config.GREEN.cpy(),
            tmpColor = new Color();

    private ComponentMapper<Transform> transformMapper;

    private ShapeRenderer renderer;
    private Viewport viewport;

    private @Null Entity selectedEntity;

    public boolean debug = true;

    public DebugSystem(ShapeRenderer renderer, Viewport viewport) {
        this.renderer = renderer;
        this.viewport = viewport;
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

        // set debug color
        if (selectedEntity != null && entityId == selectedEntity.getId()){
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

    // -- public --

    /**
     * @param entity nullable
     */
    public void setSelectedEntity(@Null Entity entity){
        selectedEntity = entity;
    }

}
