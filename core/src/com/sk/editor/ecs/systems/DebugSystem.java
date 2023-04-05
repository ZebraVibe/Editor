package com.sk.editor.ecs.systems;

import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.artemis.annotations.All;
import com.artemis.annotations.Wire;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Null;
import com.badlogic.gdx.utils.Pools;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.sk.editor.EditorManager;
import com.sk.editor.config.Config;
import com.sk.editor.ecs.components.Canvas;
import com.sk.editor.ecs.components.Image;
import com.sk.editor.ecs.components.Transform;

@Wire(injectInherited = true)
@All({Transform.class, Canvas.class})
public class DebugSystem extends CanvasSystem {

    public final Color
            DEBUG_COLOR = Config.DARKEST_GRAY.cpy(),
            SELECTED_COLOR = Config.GREEN.cpy(),
            tmpColor = new Color();

    ComponentMapper<Transform> transformMapper;
    ComponentMapper<Canvas> canvasMapper;
    ComponentMapper<Image> imageMapper;

    private ShapeRenderer renderer;

    private @Null Entity selectedEntity;

    public boolean debug = true;

    public DebugSystem(ShapeRenderer renderer, EditorManager editorManager, Viewport ecsViewport) {
        super(editorManager, ecsViewport);
        this.renderer = renderer;
    }

    @Override
    protected void processSystem() {
        // setup renderer
        //Gdx.gl.glEnable(GL20.GL_BLEND);
        renderer.setAutoShapeType(true);
        renderer.setColor(Color.WHITE);
        renderer.begin();

        // process
        super.processSystem();

        renderer.end();
    }

    @Override
    protected void processRootCanvas(int entityId, Transform transform, Canvas canvas) {
        if(transform.isRoot()){
            renderer.flush();
            renderer.setProjectionMatrix(canvas.combined());
        }

        // processes canvases and applies the viewport if possible
        super.processRootCanvas(entityId, transform, canvas);
    }

    @Override
    protected void process(int entityId) {
        renderer.flush();
        tmpColor.set(renderer.getColor());

        Transform transform = transformMapper.getSafe(entityId, null);
        if (transform == null) return;

        // set debug color
        if (selectedEntity != null && entityId == selectedEntity.getId()){
            renderer.setColor(SELECTED_COLOR);
        } else renderer.setColor(DEBUG_COLOR);

        Vector2 worldCoord = Pools.obtain(Vector2.class);
        worldCoord.set(transform.x, transform.y);
        transform.parentToWorldCoord(worldCoord); // TODO: improve

        // debug self
        renderer.rect(
                worldCoord.x ,
                worldCoord.y,
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
        Pools.free(worldCoord);

        // sort and process children
        super.process(entityId);
    }

    // -- public --

    /**
     * @param entity nullable
     */
    public void setSelectedEntity(@Null Entity entity){
        selectedEntity = entity;
    }

}
