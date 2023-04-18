package com.sk.editor.ecs.components;

import com.artemis.PooledComponent;
import com.artemis.utils.Bag;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Null;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Field;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import com.sk.editor.ui.inspector.SerializeField;
import com.sk.editor.ecs.utils.Align;
import com.artemis.Entity;
import com.sk.editor.utils.Nonnull;
import com.sk.editor.utils.UIUtils;

import java.util.Comparator;

public class Transform extends Script {

    private transient final static TransformComparator comparator = new TransformComparator();

    private transient final Rectangle bounds = new Rectangle();
    private transient final Vector2 tmp = new Vector2();

    /**
     * The entity holding this component.
     * Upon adding the component to an entity this value
     * is changed to the entity holding the component.
     * This is set in @{@link com.sk.editor.ecs.systems.TransformSystem}
     */
    public Entity entity;
    public @Null Entity parent;
    public Bag<Entity> children = new Bag<>();
    @Nonnull
    @SerializeField
    private String name = "unnamed";
    /**
     * Coordinates in parent coordinate system relative to bottom left corner
     */
    public float x, y;
    public float width, height;


    private int index;
    /**
     * a marker to inform the system to sort the children
     */
    private boolean childrenChanged;



    // -- entity --

    private Entity getNullEntity(){
        return null;
    }


    // -- name --

    /**
     * @param name if null sets the name to an empty string;
     */
    public void setName(String name) {
        if (name == null) name = "";
        this.name = name;
    }

    public String getName() {
        return name;
    }


    // -- bounds --

    /**
     * Calculates the bounds on each call.
     */
    public Rectangle getBounds() {
        return bounds.set(x, y, width, height);
    }

    public void setBounds(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }


    // -- size --
    public void setSize(float width, float height) {
        this.width = width;
        this.height = height;
    }

    public Vector2 getSize(Vector2 size) {
        return size.set(width, height);
    }


    // -- position --
    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void setPosition(float x, float y, int alignment) {
        setX(x, alignment);
        setY(y, alignment);
    }

    public Vector2 getPosition(Vector2 pos) {
        return pos.set(x, y);
    }

    public Vector2 getPosition(Vector2 pos, int alignment) {
        return pos.set(getX(alignment), getY(alignment));
    }

    /**
     * bottom left corner (0,0) is relative to this' instances' bottom left corner
     */
    public Vector2 getLocalPosition(Vector2 pos, int alignment) {
        return pos.set(getX(alignment) - x, getY(alignment) - y);
    }


    // -- x --
    public void setX(float x, int alignment) {
        if (alignment == Align.top || alignment == Align.center || alignment == Align.bottom) {
            this.x = x - width / 2f;
        } else if (alignment == Align.topRight || alignment == Align.right || alignment == Align.bottomRight) {
            this.x = x - width;
        }
    }

    public float getX(int alignment) {
        float x = this.x;
        if (alignment == Align.top || alignment == Align.center || alignment == Align.bottom) {
            x += width / 2f;
        } else if (alignment == Align.topRight || alignment == Align.right || alignment == Align.bottomRight) {
            x += width;
        }
        return x;
    }

    public float getX() {
        return x;
    }


    // -- y --
    public void setY(float y, int alignment) {
        if (alignment == Align.left || alignment == Align.center || alignment == Align.right) {
            this.y = y - height / 2f;
        } else if (alignment == Align.topLeft || alignment == Align.top || alignment == Align.topRight) {
            this.y = y - height;
        }
    }

    public float getY(int alignment) {
        float y = this.y;
        if (alignment == Align.left || alignment == Align.center || alignment == Align.right) {
            y += height / 2f;
        } else if (alignment == Align.topLeft || alignment == Align.top || alignment == Align.topRight) {
            y += height;
        }
        return y;
    }

    public float getY() {
        return y;
    }

    // -- width --
    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }


    // -- height --
    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }


    // -- hit --

    /***
     * checks children first from last added to first then itself
     * @param x in local coord
     * @param y in local coord
     * @return Maybe null
     */
    public @Null Transform hit(float x, float y) {
        // check children hit first
        Transform hit = null;
        if (hasChildren()) {
            for (int i = children.size() - 1; i >= 0; i--) {
                Entity child = children.get(i);
                Transform childTransform = child.getComponent(Transform.class);
                childTransform.parentToLocalCoord(tmp.set(x, y));
                hit = childTransform.hit(tmp.x, tmp.y);
                if (hit != null) return hit;
            }
        }
        // check itself last
        localToParentCoord(tmp.set(x, y));
        if (getBounds().contains(tmp)) return this;
        return null;
    }


    // -- parent --
    public @Null Transform getParent() {
        return parent == null ? null : parent.getComponent(Transform.class);
    }

    public @Null Entity getParentEntity() {
        return parent;
    }

    public boolean hasParent() {
        return parent != null;
    }

    public boolean hasParent(@Null Transform parent) {
        return hasParent(parent == null ? null : parent.entity);
    }

    public boolean hasParent(@Null Entity parent) {
        return this.parent == parent;
    }

    /**
     * repositions the transform so that the world position stays the same
     */
    public void setParent(@Null Entity newParent, boolean keepWorldPosition) {
        setParent(newParent == null ? null : newParent.getComponent(Transform.class), keepWorldPosition);
    }

    public void setParent(@Null Transform newParent, boolean keepWorldPosition) {
        if (!keepWorldPosition) {
            setParent(newParent);
            return;
        }
        localToWorldCoord(tmp.setZero());

        setParent(newParent);

        worldToParentCoord(tmp);
        setPosition(tmp.x, tmp.y);
    }

    /**
     * adds the child to its children
     */
    public void setParent(@Null Entity newParent) {
        setParent(newParent == null ? null : newParent.getComponent(Transform.class));
    }

    /**
     * adds the child to its children
     */
    public void setParent(@Null Transform newParent) {
        if (newParent != null && this.parent == newParent.entity) return;

        // has currently parent
        if (this.parent != null) {
            Transform oldParent = this.parent == null ? null : this.parent.getComponent(Transform.class);
            oldParent.children.remove(this.entity);
            oldParent.childrenChanged = true;
        }
        // set new parent
        this.parent = newParent == null ? null : newParent.entity;
        //parentDirty = true;

        // has new parent
        if (newParent != null) {
            newParent.children.add(this.entity);
            newParent.childrenChanged = true;
        }
    }


    // -- children --

    /**
     * sorts the children immediately and sets {@link #childrenChanged} to false;
     */
    public void sortChildren() {
        children.sort(comparator);
        childrenChanged = false;
    }


    public Bag<Entity> getChildren() {
        return children;
    }

    public boolean hasChildren() {
        return children.size() != 0;
    }

    public boolean hasChild(Entity child) {
        return children.contains(child);
    }

    /**
     * @param name should be unique among children
     * @returns the first child/ descendant instance carrying that name. Might be null.
     */
    public @Null Transform findChild(String name) {
        // check immediate children first
        for (Entity e : children) {
            Transform child = e.getComponent(Transform.class);
            if (child.name.equals(name)) return child;
        }
        // check tree second
        for (Entity e : children) {
            Transform child = e.getComponent(Transform.class);
            Transform t = child.findChild(name);
            if (t != null) return t;
        }
        return null;
    }

    public @Null Entity findChildEntity(String name) {
        Transform t = findChild(name);
        return t == null ? null : t.entity;
    }


    /**
     * If child is pre-existing as a child of this instances the old value will be replaced.
     * repositions the childs' transform so that the world position stays the same
     *
     * @param index
     * @param child
     */
    public void insertChild(int index, Transform child) {
        insertChild(index, child == null ? null : child.entity);
    }

    /**
     * If child is pre-existing as a child of this instances the old value will be replaced.
     * repositions the childs' transform so that the world position stays the same
     *
     * @param index
     * @param child
     */
    public void insertChild(int index, Entity child) {
        if (child == null || child == this.entity) return;
        index = Math.min(index, children.size());

        Transform childTransform = child.getComponent(Transform.class);
        // set child index
        childTransform.index = index;

        if (!childTransform.hasParent()) { // no pre-existing parent
            //children.insert(index, child);
            children.add(child);
            //sortChildren();

        } else if (childTransform.hasParent(this)) { // pre-existing parent is this instance
            Entity before = index == 0 ? null : children.get(index - 1);
            children.remove(child);
            //children.add(before == null ? 0 : children.indexOf(before, true) + 1, child);
            children.add(child);
            //sortChildren();
            return;
        } else { // has a parent but not this instance
            //child.parent.children.removeValue(child, true);
            childTransform.parent.getComponent(Transform.class).children.remove(child);
            //children.insert(index, child)
            children.add(entity);
            //sortChildren();
        }

        childTransform.parent = this.entity;
        childrenChanged = true;
    }

    public void insertChild(int index, Entity child, boolean keepWorldPosition) {
        insertChild(index, child == null ? null : child.getComponent(Transform.class), keepWorldPosition);
    }

    /**
     * if child is pre-existing as a child of this instances the old value will be replaced
     *
     * @param index
     * @param child
     */
    public void insertChild(int index, Transform child, boolean keepWorldPosition) {
        if (!keepWorldPosition) {
            insertChild(index, child);
            return;
        }
        child.localToWorldCoord(tmp.setZero());

        insertChild(index, child);

        child.worldToParentCoord(tmp);
        child.setPosition(tmp.x, tmp.y);
    }


    public void addChild(Transform child) {
        insertChild(children.size(), child);
    }

    public void addChild(Transform... children) {
        if (children != null) for (Transform child : children) addChild(child);
    }

    public void addChild(Entity... children) {
        if (children != null) for (Entity child : children) addChild(child);
    }

    public void addChild(Entity child) {
        if (child != null) addChild(child.getComponent(Transform.class));
    }

    public void removeChildFromParent(Entity child) {
        removeChildFromParent(child == null ? null : child.getComponent(Transform.class));
    }
    public void removeChildFromParent(Transform child) {
        if (child == null) return;
        children.remove(child.entity);
        child.parent = null;
        childrenChanged = true;
    }

    public void removeChildFromWorld(Entity child) {
        removeChildFromWorld(child == null ? null : child.getComponent(Transform.class));
    }

    public void removeChildFromWorld(Transform child) {
        if (child == null) return;
        child.removeFromWorld();
        childrenChanged = true;
    }


    public void setChildrenChanged(boolean childrenChanged) {
        this.childrenChanged = childrenChanged;
    }

    public boolean childrenChanged() {
        return childrenChanged;
    }

    // -- remove --
    public void removeFromParent() {
        setParent((Entity) null);
    }

    public void removeFromWorld() {
        setParent((Transform) null);
        entity.getWorld().deleteEntity(entity); // remove from systems
    }


    // -- coordinates --
    public Vector2 worldToScreenCoord(Vector2 worldCoord) {
        Canvas canvas = getRootEntity().getComponent(Canvas.class);
        if (canvas != null) return canvas.project(worldCoord);
        return worldCoord;
    }

    public Vector2 screenToWorldCoord(Vector2 screenCoord) {
        Canvas canvas = getRootEntity().getComponent(Canvas.class);
        if (canvas != null) return canvas.unproject(screenCoord);
        return screenCoord;
    }


    public Vector2 screenToParentCoord(Vector2 screenCoord) {
        tmp.set(screenToWorldCoord(screenCoord));
        Entity parent = this.parent;
        while (parent != null) {
            Transform parentTransform = parent.getComponent(Transform.class);
            tmp.sub(parentTransform.x, parentTransform.y);
            parent = parentTransform.parent;
        }
        return screenCoord.set(tmp);
    }

    public Vector2 parentToScreenCoord(Vector2 parentCoord) {
        return worldToScreenCoord(parentToWorldCoord(parentCoord));
    }


    public Vector2 worldToLocalCoord(Vector2 worldCoord) {
        tmp.set(worldCoord);
        Entity parent = this.entity;
        while (parent != null) {
            Transform parentTransform = parent.getComponent(Transform.class);
            tmp.sub(parentTransform.x, parentTransform.y);
            parent = parentTransform.parent;
        }
        return worldCoord.set(tmp);
    }

    public Vector2 localToWorldCoord(Vector2 localCoord) {
        tmp.set(localCoord);
        Entity parent = this.entity;
        while (parent != null) {
            Transform parentTransform = parent.getComponent(Transform.class);
            tmp.add(parentTransform.x, parentTransform.y);
            parent = parentTransform.parent;
        }
        return localCoord.set(tmp);
    }


    public Vector2 localToScreenCoord(Vector2 localCoord) {
        return worldToScreenCoord(localToWorldCoord(localCoord));
    }


    public Vector2 parentToWorldCoord(Vector2 parentCoord) {
        tmp.set(parentCoord);
        Entity parent = this.parent;
        while (parent != null) {
            Transform parentTransform = parent.getComponent(Transform.class);
            tmp.add(parentTransform.x, parentTransform.y);
            parent = parentTransform.parent;
        }
        return parentCoord.set(tmp);
    }

    public Vector2 worldToParentCoord(Vector2 worldCoord) {
        tmp.set(worldCoord);
        Entity parent = this.parent;
        while (parent != null) {
            Transform parentTransform = parent.getComponent(Transform.class);
            tmp.sub(parentTransform.x, parentTransform.y);
            parent = parentTransform.parent;
        }
        return worldCoord.set(tmp);
    }


    public Vector2 localToParentCoord(Vector2 localCoord) {
        tmp.set(localCoord);
        tmp.add(x, y);
        return localCoord.set(tmp);
    }

    public Vector2 parentToLocalCoord(Vector2 parentCoord) {
        tmp.set(parentCoord);
        tmp.sub(x, y);
        return parentCoord.set(tmp);
    }


    // -- root -
    public boolean isRoot() {
        return getRootEntity() == entity;
    }
    public Entity getRootEntity() {
        Transform current = entity.getComponent(Transform.class);
        while (current.parent != null) {
            current = current.parent.getComponent(Transform.class);
        }
        return current.entity;
    }
    public Transform getRoot(){
        return getRootEntity().getComponent(Transform.class);
    }


    // -- index --

    /**
     * If a parent is present informs it to sort its children.
     * If this transform {@link #isRoot()} (which always leads to it having a Canvas) it will be automatically called
     * by the system
     * @param index
     */
    public void setIndex(int index) {
        this.index = index;
        if(parent != null)
            parent.getComponent(Transform.class).setChildrenChanged(true);
    }

    public int getIndex() {
        return index;
    }


    // -- reset --
    @Override
    protected void reset() {
        bounds.set(0, 0, 0, 0);
        tmp.setZero();

        children.clear();
        parent = null;
        entity = null;


        x = 0;
        y = 0;
        width = 0;
        height = 0;
        name = "unnamed";

        index = 0;
        childrenChanged = false;
    }

    public static class TransformComparator implements Comparator<Entity> {
        @Override
        public int compare(Entity o1, Entity o2) {
            Transform t1 = o1.getComponent(Transform.class);
            Transform t2 = o2.getComponent(Transform.class);
            return t1.index - t2.index;
        }
    }
}
