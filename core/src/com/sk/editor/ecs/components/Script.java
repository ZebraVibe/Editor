package com.sk.editor.ecs.components;

import com.artemis.Component;
import com.artemis.Entity;
import com.artemis.PooledComponent;

public class Script extends PooledComponent {

    private boolean init;

    public void create(){}

    public void update(){}

    protected void updateInternally(){
        if(!init){
            create();
            init = true;
        }
        update();
    }

    @Override
    protected void reset() {
        init = false;
    }
}
