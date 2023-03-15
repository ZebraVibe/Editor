package com.sk.editor.ui.overview;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.artemis.EntitySubscription;
import com.artemis.utils.IntBag;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.Logger;
import com.badlogic.gdx.utils.Null;
import com.sk.editor.ecs.ECSManager;
import com.sk.editor.ecs.world.systems.DebugSystem;
import com.sk.editor.ui.InputManager;
import com.sk.editor.ui.UINode;
import com.sk.editor.ui.UITree;
import com.sk.editor.ui.UIWindow;
import com.sk.editor.ui.logger.EditorLogger;
import com.sk.editor.ecs.world.components.Family;
import com.sk.editor.ecs.world.components.Transform;

public class Hierarchy extends UIWindow {

    private static final EditorLogger log = new EditorLogger(Hierarchy.class.toString(), Logger.DEBUG);

    private UITree<EntityNode, Entity> tree;
    private ECSManager ecsManager;
    private InputManager inputManager;
    private ComponentMapper<Family> childrenMapper;


    public Hierarchy(Skin skin, ECSManager ecsManager, InputManager inputManager) {
        super("Hierarchy", skin);
        this.ecsManager = ecsManager;
        this.inputManager = inputManager;
        init();
    }

    // -- init --

    private void init() {
        Skin skin = getSkin();

        // mapper
        childrenMapper = ecsManager.getMapper(Family.class);

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
        scrollTable.add(tree).expand().fill();
        add(scroll).expand().fill();

        // add transform subscription
        EntitySubscription sub =  ecsManager.geAspectSubscriptionManager().get(Aspect.all(Transform.class));
        ComponentMapper<Transform> transformMapper = ecsManager.getTransformMapper();
        sub.addSubscriptionListener(new EntitySubscription.SubscriptionListener() {
            @Override
            public void inserted(IntBag entities) { // bag wit inserted entities
                for(int i = 0; i < entities.size(); i++){
                    Entity e = ecsManager.getEntity(entities.get(i));
                    addNode(e, null); // add as root node
                }
            }

            @Override
            public void removed(IntBag entities) { // bag with removed entities
                for(int i = 0; i < entities.size(); i++){
                    Entity e = ecsManager.getEntity(entities.get(i));
                    removeNode(e); // add as root node
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
        };
    }

    // -- public --

    public UITree<EntityNode, Entity> getTree(){
        return tree;
    }

    public EntityNode newNode(Entity entity){
        EntityNode node = new EntityNode(entity, getSkin());
        return node;
    }

    /**
     *
     * @param entity
     * @param parent if null added as root node
     */
    public void addNode(Entity entity, @Null EntityNode parent){
        EntityNode node = newNode(entity);
        if(parent == null)tree.add(node);
        else parent.add(node);
    }

    private void removeNode(Entity entity){
        if(true)throw new GdxRuntimeException("method unfinished");
        EntityNode node = tree.findNode(entity);
        //TODO: decide if here : remove entity and possible children from world
        //Family family = childrenMapper.get(node.getValue());
        if(node != null){
            node.remove();
        }
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
