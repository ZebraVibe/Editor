package com.sk.editor.ecs;

import com.artemis.*;
import com.artemis.io.JsonArtemisSerializer;
import com.artemis.io.SaveFileFormat;
import com.artemis.link.EntityLinkManager;
import com.artemis.link.LinkAdapter;
import com.artemis.link.LinkListener;
import com.artemis.managers.WorldSerializationManager;
import com.artemis.utils.Bag;
import com.artemis.utils.IntBag;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.sk.editor.Editor;
import com.sk.editor.config.Config;
import com.sk.editor.ecs.components.Canvas;
import com.sk.editor.ecs.components.Transform;
import com.sk.editor.ecs.systems.*;
import com.sk.editor.ui.NotifyingOrthographicCamera;
import com.sk.editor.ui.UIStage;
import com.sk.editor.ui.logger.EditorLogger;
import com.sk.editor.utils.ArrayPool;

import java.io.*;

public class ECSManager implements NotifyingOrthographicCamera.CameraListener {

    private static final EditorLogger log = new EditorLogger(ECSManager.class.toString(), Logger.DEBUG);
    private Editor editor;
    private Viewport ecsViewport, uiViewport;
    private World world;
    private WorldSerializationManager worldSerializationManager;
    private EntityLinkManager entityLinkManager;
    private Archetype transformArchetype, canvasArchetype;
    private EntitySubscription transformSubscription;
    private IntBag tmpIntBag = new IntBag();
    private ArrayPool arrayPool = new ArrayPool();
    private UIStage uiStage;


    public ECSManager(Editor editor, UIStage uiStage, Viewport ecsViewport){
        this.editor = editor;
        this.ecsViewport = ecsViewport;
        this.uiViewport = uiStage.getViewport();
        this.uiStage = uiStage;

        initWorld();
        initArchetypes();
        initSubscription();

        // load
        loadWorld();
    }

    private void initWorld() {
        // create config
        WorldConfiguration config = new WorldConfigurationBuilder()
                .with(
                        this.worldSerializationManager = new WorldSerializationManager(),
                        this.entityLinkManager = new EntityLinkManager(),
                        new TransformSystem(),
                        new ScriptSystem(),
                        new RenderSystem(editor.getBatch(), editor.getEditorManager(), ecsViewport),
                        new DebugSystem(editor.getShapeRenderer(),  editor.getEditorManager(), ecsViewport))
                .build();

        // create injector
        //config.setInjector(new CachedInjector());

        // create world
        this.world = new World(config);

        //setup
        setupWorldSerializationManager();
        setupEntityLinkManager();
    }

    private void setupWorldSerializationManager() {
        worldSerializationManager.setSerializer(new JsonArtemisSerializer(world));

    }

    private void setupEntityLinkManager(){
        // -- register link listeners --
        // parent field
        entityLinkManager.register(Transform.class, "parent", new LinkListener(){
            ComponentMapper<Transform> transformMapper;

            @Override // field value updated after being null
            public void onLinkEstablished(int sourceId, int targetId) {
                Transform transform = transformMapper.get(sourceId);
                Transform parentTransform;

                // if parent has no Transform: create
                if(!transformMapper.has(targetId))parentTransform = transformMapper.create(targetId);
                else parentTransform = transformMapper.get(targetId);

                //check if parent has entity as child already : else add
                if(parentTransform.hasChild(world.getEntity(sourceId)) == false)
                    parentTransform.addChild(transform);

            }

            @Override // deletion of component or source entity
            public void onLinkKilled(int sourceId, int targetId) {
                transformMapper.get(sourceId).removeFromParent(); // changed to root inserted at its index
            }

            @Override // reference entity deleted(field auto reset to null)
            public void onTargetDead(int sourceId, int deadTargetId) {
                transformMapper.get(sourceId).removeFromWorld();
            }

            @Override
            public void onTargetChanged(int sourceId, int targetId, int oldTargetId) {
                Transform transform = transformMapper.get(sourceId);
                Transform oldParent = transformMapper.get(oldTargetId);
                Transform newParent;

                // if new parent has no Transform: create
                if(!transformMapper.has(targetId))newParent = transformMapper.create(targetId);
                else newParent = transformMapper.get(targetId);

                //remove child from old parent
                oldParent.removeChildFromParent(transform);

                //check if new parent has entity as child already : else add
                if(newParent.hasChild(world.getEntity(sourceId)) == false)
                    newParent.addChild(transform);

            }
        });
        // children field
        entityLinkManager.register(Transform.class, "children", new LinkAdapter(){
            ComponentMapper<Transform> transformMapper;

            @Override
            public void onLinkEstablished(int sourceId, int targetId) {
                Transform transform = transformMapper.get(sourceId);
                Bag<Entity> children = transformMapper.get(sourceId).children;
                children.forEach(e -> {
                    Transform childTransform;
                    // create transform for new child
                    if(!transformMapper.has(e)) childTransform = transformMapper.create(e);
                    else childTransform = transformMapper.get(e);

                    // set parent of new child if missing
                    if(childTransform.hasParent(transform) == false)
                        childTransform.setParent(transform);
                });
            }

            @Override // deletion of source entity or its component
            public void onLinkKilled(int sourceId, int targetId) {
                transformMapper.get(sourceId).removeFromWorld(); // remove every child
            }

            @Override // reference entity deleted (field auto reset to null or (?))
            public void onTargetDead(int sourceId, int deadTargetId) {
                transformMapper.get(deadTargetId).removeFromWorld(); // removing child (?)
            }

        });

    }

    private void initArchetypes(){
        transformArchetype = new ArchetypeBuilder().add(Transform.class).build(world);

        canvasArchetype = new ArchetypeBuilder().add(Transform.class, Canvas.class).build(world);
    }

    private void initSubscription() {
        transformSubscription = world.getAspectSubscriptionManager().get(Aspect.all(Transform.class));
    }

    // -- save & load --

    /**
     *
     * @param subscription
     * @return the JSON string
     */
    private String saveToString(EntitySubscription subscription){
        return saveToString(subscription.getEntities());
    }

    /**
     * saves the intBag to a string
     * @param intBag
     * @return the JSON string
     */
    private String saveToString(IntBag intBag){
        try {
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            final SaveFileFormat save = new SaveFileFormat(intBag);
            worldSerializationManager.save(bos, save);
            String json = new String(bos.toByteArray());
            log.debug("Saving successful.");
            return json;
        } catch (Exception e){
            log.error("Saving Failed.");
        }
        return "";
    }
    private void saveToPrefs(IntBag entities){
        String json = saveToString(entities);
        editor.getEditorManager().getPrefKeys().WORLD_SERIALIZATION.set(json);
    }
    private void loadFromPrefs(){
        String json = editor.getEditorManager().getPrefKeys().WORLD_SERIALIZATION.get();
        try {
            ByteArrayInputStream is = new ByteArrayInputStream(json.getBytes("UTF-8"));
            worldSerializationManager.load(is, SaveFileFormat.class);
            log.debug("Loading successful.");
        } catch (Exception e){
            log.error("Loading failed.", e);
        }

    }


    @Deprecated
    private void loadFromFile(){
        FileHandle file = getWorldSaveFile();
        final InputStream is = Class.class.getResourceAsStream(file.file().getName());
        if(is == null){
            log.error("InputStream is null - maybe save file not existing yet since no save has been done before.");
            return;
        }
        worldSerializationManager.load(is, SaveFileFormat.class);
    }
    /**
     * saves the intBag to file
     * @param entities
     */
    @Deprecated
    private void saveToFile(IntBag entities){
        FileHandle file = getWorldSaveFile(); // file to save to
        try {
            FileOutputStream fos = new FileOutputStream(file.file(), false);
            worldSerializationManager.save(fos, new SaveFileFormat(entities));
            fos.flush();
        } catch (IOException e) {
            log.error("Could not save to file.", e);
        }

    }
    @Deprecated
    private FileHandle getWorldSaveFile(){
        return Gdx.files.external(Config.WORLD_SAVE_FILE);
    }




    public void saveWorld(){
        EntitySubscription subscription = world.getAspectSubscriptionManager().get(Aspect.all());
        saveToPrefs(subscription.getEntities());
    }
    public void loadWorld(){
        loadFromPrefs();
        //...
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
     * @return Maybe null
     */
    public @Null Entity hitScreen(float screenX, float screenY){
        ComponentMapper<Transform> transformMapper = getTransformMapper();
        Vector2 localCoord = Pools.obtain(Vector2.class);

        // get root entities
        Bag<Entity> rootEntities = Pools.obtain(Bag.class);
        rootEntities.clear();
        world.getSystem(RenderSystem.class).getRootCanvases(rootEntities);

        // iterate from last added to first
        for(int i = rootEntities.size() -1; i >= 0; i--){
            int id = rootEntities.get(i).getId();
            Transform transform = transformMapper.getSafe(id, null);
            if(transform == null)continue;
            // check if hitting
            transform.screenToParentCoord(localCoord.set(screenX, screenY));
            transform.parentToLocalCoord(localCoord);
            Transform hit = transform.hit(localCoord.x, localCoord.y);
            if(hit != null)return hit.entity;
            log.debug("hitScreen(): hit is null");
        }
        Pools.free(localCoord);
        rootEntities.clear();
        Pools.free(rootEntities);
        return null;
    }

    public ComponentMapper<Transform> getTransformMapper(){
        return world.getMapper(Transform.class);
    }

    /**
     * Creates an entity with a {@link Transform} component.
     * Root entities are only displayed with a @{@link Canvas} component
     * @return the entity id
     */
    public int create(){
        return createEntity().getId();
    }

    /**
     * Creates an entity with a {@link Transform} component.
     * Root entities are only displayed with a @{@link Canvas} component
     * @return the entity id
     */
    public Entity createEntity(){
        ComponentMapper<Transform> transformMapper = world.getMapper(Transform.class);
        Entity e = world.createEntity(transformArchetype);
        transformMapper.get(e).setName("Entity");
        return e;
    }
    public void delete(int entityId){
        world.delete(entityId);
    }
    public void delete(Entity e){
        world.deleteEntity(e);
    }
    public EntityEdit edit(int entityId){
        return world.edit(entityId);
    }
    public EntityEdit edit(Entity entity){
        return world.edit(entity.getId());
    }
    public Entity getEntity(int entityId){
        return world.getEntity(entityId);
    }


    // -- canvas --

    /**
     * Root entities are only displayed with a @{@link Canvas} component
     * @return
     */
    public Entity createCanvas(){
        ComponentMapper<Transform> transformMapper = world.getMapper(Transform.class);
        Entity e = world.createEntity(canvasArchetype);
        transformMapper.get(e).setName("Canvas");
        return e;
    }


    // -- focused entity --
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

    /**
     * updates all systems extending @{@link com.sk.editor.ui.NotifyingOrthographicCamera.CameraListener}
     * @param camera
     */
    @Override
    public void updated(OrthographicCamera camera) {
        for(BaseSystem system : world.getSystems()){
            if(system instanceof NotifyingOrthographicCamera.CameraListener){
                ((NotifyingOrthographicCamera.CameraListener)system).updated(camera);
            }
        }
    }
}
