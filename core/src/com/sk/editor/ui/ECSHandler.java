package com.sk.editor.ui;

import com.artemis.*;
import com.artemis.utils.IntBag;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Null;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.sk.editor.Editor;
import com.sk.editor.world.components.Transform;
import com.sk.editor.world.systems.DebugSystem;
import com.sk.editor.world.systems.RenderSystem;

public class ECSHandler{

    private Editor editor;
    private Viewport ecsViewport, uiViewport;
    private World world;
    private EntitySubscription transformSubscription;
    private IntBag tmpIntBag = new IntBag();
    private Entity selectedEntity;

    public ECSHandler(Editor editor, Viewport ecsViewport, Viewport uiViewport){
        this.editor = editor;
        this.ecsViewport = ecsViewport;
        this.uiViewport = uiViewport;

        initWorld();
        initTransformSubscription();
    }

    private void initWorld() {
        // create ecs world
        WorldConfiguration config = new WorldConfigurationBuilder()
                .with(
                        new RenderSystem(editor.getBatch()),
                        new DebugSystem(editor.getShapeRenderer(), ecsViewport, this))
                .build();
        this.world = new World(config);
    }

    private void initTransformSubscription() {
        transformSubscription = world.getAspectSubscriptionManager().get(Aspect.all(Transform.class));
        initTransformSubscriptionListener();
    }

    private void initTransformSubscriptionListener(){
        transformSubscription.addSubscriptionListener(new EntitySubscription.SubscriptionListener() {

            @Override
            public void inserted(IntBag entities) {

            }

            @Override
            public void removed(IntBag entities) {
                // check if selected entity is removed
                if(selectedEntity != null && entities.contains(selectedEntity.getId()) == false){
                    selectedEntity = null;
                }

            }
        });
    }

    // -- public --


    /**
     *
     * @return the entity selected (i.e. to inspect)
     */
    public Entity getSelectedEntity() {
        return selectedEntity;
    }

    protected void setSelectedEntity(Entity selectedEntity) {
        this.selectedEntity = selectedEntity;
    }


    /**
     *
     * @param worldX
     * @param worldY
     * @return Maybe null
     */
    public @Null Entity hitWorld(float worldX, float worldY){
        ComponentMapper<Transform> transformMapper = getTransformMapper();
        IntBag actives = transformSubscription.getActiveEntityIds().toIntBag(tmpIntBag);
        int[] ids = actives.getData();
        for(int i = 0; i < actives.size(); i++){
            int id = ids[i];
            Transform transform = transformMapper.getSafe(id, null);
            if(transform == null)continue;
            // check if is hitting
            if(transform.getBounds().contains(worldX, worldY))return world.getEntity(id);
        }
        return null;
    }

    public ComponentMapper<Transform> getTransformMapper(){
        return world.getMapper(Transform.class);
    }

    public World getWorld(){
        return world;
    }

    public EntitySubscription getTransformSubscription(){
        return transformSubscription;
    }

    /**
     * disposes the world
     */
    public void dispose(){
        world.dispose();
    }

}
