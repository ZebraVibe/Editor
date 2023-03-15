package com.sk.editor.ecs;

import com.artemis.*;
import com.artemis.injection.CachedInjector;
import com.artemis.injection.Injector;
import com.artemis.utils.IntBag;
import com.badlogic.gdx.utils.Null;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.sk.editor.Editor;
import com.sk.editor.ui.UIStage;
import com.sk.editor.ecs.world.components.Transform;
import com.sk.editor.ecs.world.systems.DebugSystem;
import com.sk.editor.ecs.world.systems.RenderSystem;

public class ECSManager {

    private Editor editor;
    private Viewport ecsViewport, uiViewport;
    private World world;
    private EntitySubscription transformSubscription;
    private IntBag tmpIntBag = new IntBag();
    private UIStage uiStage;
    private Entity selectedEntity;

    public ECSManager(Editor editor, UIStage uiStage, Viewport ecsViewport){
        this.editor = editor;
        this.ecsViewport = ecsViewport;
        this.uiViewport = uiStage.getViewport();
        this.uiStage = uiStage;

        initWorld();
        initSubscriptionListener();
    }

    private void initWorld() {
        // create ecs world
        WorldConfiguration config = new WorldConfigurationBuilder()
                .with(
                        new RenderSystem(editor.getBatch()),
                        new DebugSystem(editor.getShapeRenderer(), ecsViewport))
                .build();
        Injector injector = createInjector();
        config.setInjector(injector);
        this.world = new World(config);
    }

    private void initSubscriptionListener() {
        transformSubscription = world.getAspectSubscriptionManager().get(Aspect.all(Transform.class));
        transformSubscription.addSubscriptionListener(new EntitySubscription.SubscriptionListener() {

            @Override // bag with inserted entities
            public void inserted(IntBag entities) {}

            @Override // bag with removed entities
            public void removed(IntBag entities) {}
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

    public AspectSubscriptionManager geAspectSubscriptionManager(){
        return world.getAspectSubscriptionManager();
    }

    public <T extends Component> ComponentMapper<T> getMapper(Class<T> component){
        return world.getMapper(component);
    }

    public void processWorld(float delta){
        world.setDelta(delta);
        world.process();
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

    /**
     * creates an entity with a {@link Transform} component
     * @return the entity id
     */
    public int createEntity(){
        int id = world.create();
        Transform transform = world.edit(id).create(Transform.class);
        return id;
    }

    public void deleteEntity(int entityId){
        world.delete(entityId);
    }

    public void deleteEntity(Entity e){
        world.deleteEntity(e);
    }

    public EntityEdit editEntity(int entityId){
        return world.edit(entityId);
    }

    public Entity getEntity(int entityId){
        return world.getEntity(entityId);
    }

    /**
     * sets the entity to draw the focused debug bounds to
     * @param entity
     */
    public void setFocused(@Null Entity entity){
        DebugSystem debugSystem = world.getSystem(DebugSystem.class);
        debugSystem.setSelectedEntity(entity);
    }

    /**
     * disposes the world
     */
    public void dispose(){
        world.dispose();
    }

}
