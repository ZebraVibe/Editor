package com.sk.editor.ecs;

import com.artemis.*;
import com.artemis.injection.CachedInjector;
import com.artemis.injection.Injector;
import com.artemis.utils.IntBag;
import com.badlogic.gdx.utils.Null;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.sk.editor.Editor;
import com.sk.editor.world.components.Transform;
import com.sk.editor.world.systems.DebugSystem;
import com.sk.editor.world.systems.RenderSystem;

public class ECSManager {

    private Editor editor;
    private Viewport ecsViewport, uiViewport;
    private World world;
    private EntitySubscription transformSubscription;
    private IntBag tmpIntBag = new IntBag();
    private Entity selectedEntity;

    public ECSManager(Editor editor, Viewport ecsViewport, Viewport uiViewport){
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
        Injector injector = createInjector();
        config.setInjector(injector);
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


    /**
     * handles dependency injection and annotations that are used for reading injections
     * @return
     */
    private Injector createInjector() {
        // handling @Wire annotated fields are not by default present in world config so the custom field
        // resolver should take care of it if @Wire is wanted
        // FieldHandler fieldHandler = new FieldHandler(new InjectionCache());
        // fieldHandler.addFieldResolver(new CustomFieldsResolver());

        Injector injector = new CachedInjector();
        return injector;
    }

    // -- public --


    /**
     *
     * @return the entity selected (i.e. to inspect)
     */
    public Entity getSelectedEntity() {
        return selectedEntity;
    }

    /**
     * Only use when you know hat you are doing
     * @param selectedEntity
     */
    public void setSelectedEntity(Entity selectedEntity) {
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
        for(int i = actives.size() -1; i >= 0; i--){
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

    /**
     * creates an entity with a {@link Transform} component
     * @return the entity id
     */
    public int createEntity(){
        int id = world.create();
        Transform transform = world.edit(id).create(Transform.class);
        return id;
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
