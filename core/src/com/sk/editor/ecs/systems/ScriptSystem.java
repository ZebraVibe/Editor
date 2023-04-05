package com.sk.editor.ecs.systems;

import com.artemis.ComponentMapper;
import com.artemis.annotations.All;
import com.artemis.systems.IteratingSystem;
import com.sk.editor.ecs.components.Script;

/**
 * Probably does not work since Script components extend Script and are
 * not direct Script objects themselves
 */
@All(Script.class)
public class ScriptSystem extends IteratingSystem {

    ComponentMapper<Script> scriptMapper;


    @Override
    protected void inserted(int entityId) {
        Script script = scriptMapper.get(entityId);
        //world.inject(script);// artemis should inject components and systems
        script.create();
    }



    @Override
    protected void process(int entityId) {
        Script script = scriptMapper.get(entityId);
        script.update();
    }


}
