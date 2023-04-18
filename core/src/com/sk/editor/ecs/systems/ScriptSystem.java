package com.sk.editor.ecs.systems;

import com.artemis.*;
import com.artemis.annotations.All;
import com.artemis.systems.IteratingSystem;
import com.artemis.utils.Bag;
import com.artemis.utils.IntBag;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import com.sk.editor.ecs.components.Script;
import com.sk.editor.ecs.components.Transform;
import com.sk.editor.scripting.ScriptManager;


/**
 * Probably does not work since Script components extend Script and are
 * not direct Script objects themselves
 */
@All()
public class ScriptSystem extends BaseEntitySystem {

    ComponentMapper<Transform> transformMapper;
    Array<Class<?>> scriptSubTypes = new Array<>();
    Bag<Component> tmpBag = new Bag<>();
    ScriptManager scriptManager;

    public ScriptSystem(ScriptManager scriptManager){
        this.scriptManager = scriptManager;
    }




    @Override
    protected void inserted(int entityId) {
        /*
        Script script = scriptMapper.get(entityId);
        if(script == null)return;
        //world.inject(script);// artemis should injects systems
        script.create();*/
    }


    @Override
    protected void initialize() {
        try {
            //scriptManager.getSubTypesOf(Script.class, scriptSubTypes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    protected void processSystem() {


        // process entities;
        IntBag actives = getEntityIds();
        int[] ids = actives.getData();
        for (int i = 0, s = actives.size(); s > i; i++) {
            //process(ids[i]);
        }
    }

    protected void process(int entityId) {
        Entity entity = world.getEntity(entityId);
        entity.getComponents(tmpBag);
        for(Component c : tmpBag){
            if(scriptSubTypes.contains(c.getClass(), true)){
                Script script = (Script) c;
                script.update();
            }
        }

        tmpBag.clear();
    }


}
