package com.sk.editor.ui.hierarchy;

import com.artemis.*;
import com.artemis.utils.Bag;
import com.artemis.utils.IntBag;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Logger;
import com.badlogic.gdx.utils.Null;
import com.sk.editor.ecs.ECSManager;
import com.sk.editor.ecs.systems.DebugSystem;
import com.sk.editor.ui.InputManager;
import com.sk.editor.ui.UINode;
import com.sk.editor.ui.UITree;
import com.sk.editor.ui.UIWindow;
import com.sk.editor.ui.logger.EditorLogger;
import com.sk.editor.ecs.components.Transform;

public class Hierarchy extends UIWindow {

    private static final EditorLogger log = new EditorLogger(Hierarchy.class.toString(), Logger.DEBUG);

    private UITree<EntityNode, Entity> tree;
    private ECSManager ecsManager;
    private InputManager inputManager;


    public Hierarchy(Skin skin, ECSManager ecsManager, InputManager inputManager) {
        super("Hierarchy", skin);
        this.ecsManager = ecsManager;
        this.inputManager = inputManager;
        init();
    }

    // -- init --

    private void init() {
        Skin skin = getSkin();
        // tree
        tree = createTree();

        // scroll pane
        Table scrollTable = new Table();
        ScrollPane scroll = new ScrollPane(scrollTable, skin);
        scroll.addListener(new InputListener(){
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                event.getStage().setScrollFocus(scroll);
            }
        });
        scroll.setFlickScroll(false);
        scrollTable.add(tree).expand().fill();
        add(scroll).expand().fill();

       addSubscription();
    }

    private void addSubscription(){
        // add transform subscription
        EntitySubscription sub =  ecsManager.geAspectSubscriptionManager().get(Aspect.all(Transform.class));
        ComponentMapper<Transform> transformMapper = ecsManager.getTransformMapper();

        // transform subscription
        sub.addSubscriptionListener(new EntitySubscription.SubscriptionListener() {

            IntBag tmpEntities = new IntBag();

            @Override
            public void inserted(IntBag entities) { // bag with inserted entities
                tmpEntities.addAll(entities);

                int[] ids = entities.getData();
                // remove children from the bag whose parents are also in the bag
                for(int i = 0, s = entities.size(); i < s; i++ ){
                    int id = ids[i];
                    Transform transform = ecsManager.getEntity(id).getComponent(Transform.class);
                    Transform parent = transform.getParent();

                    // remove itself from the bag if the parent is inside too
                    if(parent != null && entities.contains(parent.entity.getId())){
                        tmpEntities.removeValue(transform.entity.getId());
                    }
                }

                // add everything remaining to tree
                ids = tmpEntities.getData();
                for(int i = 0, s = tmpEntities.size(); i < s; i++ ){
                    int id = ids[i];
                    Transform transform = ecsManager.getEntity(id).getComponent(Transform.class);
                    Transform parent = transform.getParent();
                    insertNode(transform.entity, parent == null ? null : findNode(parent.entity), transform.getIndex());
                }

            }


            @Override
            public void removed(IntBag entities) { // bag with removed entities
                for(int i = 0; i < entities.size(); i++){
                    Entity e = ecsManager.getEntity(entities.get(i));
                    removeNode(e, true);
                }
            }
        });
    }


    private UITree<EntityNode, Entity> createTree(){
        return new UITree<EntityNode, Entity>(getSkin()){
            @Override
            public void nodeSelected(EntityNode selectedNode) {
                // select entity
                inputManager.setFocusedEntity(selectedNode.getValue());
            }

            @Override
            public boolean insertNode(EntityNode node, EntityNode parent, int index) {
                // setup index
                Entity entity = node.getValue();
                index = Math.min(index, (parent == null ? tree.getRootNodes().size : parent.getChildren().size));

                // insert
                if(super.insertNode(node, parent, index)){

                    Transform transform = entity.getComponent(Transform.class);
                    Transform oldParent = transform.getParent();
                    Transform newParent = parent == null ? null : parent.transform;

                    // set index (also informs to sort)
                    transform.setIndex(index);


                    if(newParent == null)transform.setParent((Entity) null, true);
                    else { // has new parent
                        // set parent
                        newParent.insertChild(index, transform, true); // to insert at correct index with same world position
                    }
                    return true;
                }
                return false;
            }
        };
    }

    // -- public --

    //public UITree<EntityNode, Entity> getTree(){return tree;}

    /**
     * @param entity
     * @return a new instance of a node with the given entity as value.
     * (Is not added to the tree).
     */
    private EntityNode newNode(Entity entity){
        EntityNode node = new EntityNode(entity, getSkin());
        return node;
    }

    /**
     * Inserts the given entity and its children if wanted to the tree
     * @param entity
     * @param parent the parent to insert the entity to. If null inserts to root entities
     * @param index must be >= 0
     * @param withChildren if true inserts the entities' @{@link Transform} children as well
     * @return null if the insertion failed
     */
    public EntityNode insertNode(Entity entity, @Null EntityNode parent, int index, boolean withChildren){
        EntityNode node = tree.findNode(entity);
        if(node == null)node = newNode(entity);

        // self
        tree.insertNode(node, parent, index);

        // children
        if(withChildren){
            Transform transform = entity.getComponent(Transform.class);
            for(Entity child : transform.getChildren()){
                Transform childTransform = child.getComponent(Transform.class);
                insertNode(child, node, childTransform.getIndex(), true);
            }
        }

        return node;
    }

    /**
     * calls {@link #insertNode(Entity, EntityNode, int, boolean)} with true
     * @param entity
     * @param parent
     * @param index
     * @return
     */
    public EntityNode insertNode(Entity entity, @Null EntityNode parent, int index){
        return insertNode(entity, parent, index, true);
    }

    /**
     * Adds the given entity and its children if wanted to the tree
     * @param entity
     * @param parent if null added appends the root nodes
     * @param withChildren if true adds the entities' @{@link Transform} children as well
     * @return the node of the entity
     */
    public EntityNode addNode(Entity entity, @Null EntityNode parent, boolean withChildren){
        insertNode(entity, parent, Integer.MAX_VALUE, withChildren);
        return tree.findNode(entity);
    }

    /**
     * calls {@link #addNode(Entity, EntityNode, boolean)} with true
     * @param entity
     * @param parent
     * @return
     */
    public EntityNode addNode(Entity entity, @Null EntityNode parent){
        return addNode(entity, parent, true);
    }

    public @Null EntityNode findNode(Entity entity){
        return tree.findNode(entity);
    }

    /**
     *
     * @param entity
     * @param removeFromWorld if true removes the entity entirely from the world which makes the entity unusable
     *                        and put back into the pool.
     * @return the node the entity belongs to. Maybe null.
     */
    private @Null EntityNode removeNode(Entity entity, boolean removeFromWorld){
        EntityNode node = tree.findNode(entity);
        // remove node from parent
        if(node != null)node.remove();

        // remove transform from world or parent
        Transform transform = entity.getComponent(Transform.class);

        //no need anymore to update canvas systems
        //Canvas canvas = ecsManager.getMapper(Canvas.class).getSafe(entity, null);
        //if(canvas != null){} // remove from canvas system

        // remove from parent or world
        if(removeFromWorld){
            transform.removeFromWorld();
        } else {
            transform.removeFromParent();
        }
        return node;
    }

    public Array<EntityNode> getRootNodes(){
        return tree.getRootNodes();
    }

    /**
     * see @{@link UITree#chooseNode(UINode)}.
     * Also informs the @{@link DebugSystem}
     * @param entity
     */
    public void chooseNode(@Null Entity entity){
        tree.chooseNode(entity == null ? null : tree.findNode(entity));
        ecsManager.setFocused(entity);
    }


    public @Null EntityNode getSelectedNode(){
        return tree.getSelectedNode();
    }

    public @Null Entity getSelectedEntity(){
        return tree.getSelectedValue();
    }


}
