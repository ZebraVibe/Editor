package com.sk.editor.ecs.world.components;

import com.artemis.PooledComponent;
import com.badlogic.gdx.utils.Array;

public class Family extends PooledComponent {

    Family parent;
    Array<Family> children = new Array<>();

    public Array<Family> getChildren() {
        return children;
    }

    /**
     * removes this instance from its parent Family component if a parent is present
     * but does not remove the entity from the world
     */
    public void remove(){
        // remove itself
        parent.children.removeValue(this, true);
        parent = null;

        // remove children
    }

    /**
     * @param child removes this child but does not remove the entity from the world
     */
    public boolean remove(Family child){
        if(child == this || child == null)return false;
        children.removeValue(child, true);
        child.parent = null;
        return true;
    }

    /**
     * removes all child but does not remove their entities from the world
     */
    public void removeAll(){
        for(int i = 0; i < children.size; i++){
            remove(children.get(i));
        }
    }

    public void add(Family child){
        if(child == this || child == null)return;
        child.parent = this;
        children.add(child);
    }

    public void addAll(Family ...children){
        if(children == null)return;
        for(int i = 0; i < children.length; i++){
            Family child = children[i];
            if(child == null)continue;
            add(child);
        }
    }


    @Override
    protected void reset() {
        children.clear();
        parent = null;
    }
}
