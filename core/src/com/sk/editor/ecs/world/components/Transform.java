package com.sk.editor.ecs.world.components;

import com.artemis.PooledComponent;
import com.artemis.World;
import com.artemis.annotations.PooledWeaver;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Null;
import com.badlogic.gdx.utils.SnapshotArray;
import com.sk.editor.ui.inspector.SerializeField;
import com.sk.editor.ecs.world.utils.Align;
import com.artemis.Entity;
import com.sk.editor.utils.Nonnull;

@PooledWeaver
public class Transform extends PooledComponent {


	private @Null Transform parent;
	private final SnapshotArray<Transform> children = new SnapshotArray<Transform>();

	private @Nonnull World world;
	private @Nonnull Entity entity;
	private final Rectangle bounds = new Rectangle();
	private final Vector2 tmp = new Vector2();


	/** nonnull*/
	@SerializeField
	private String tag = "entity";

	/**Coordinates in parent coordinate system relative to bottom left corner*/
	public float x, y;
	public float width, height;




	// -- tag --
	/**
	 * @param tag if null sets the tag to an empty string;
	 */
	public void setTag(String tag) {
		if(tag == null)tag = "";
		this.tag = tag;
	}
	public String getTag() {
		return tag;
	}


	// -- bounds --
	/**Calculates the bounds on each call.*/
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
		return pos.set(x,y);
	}
	public Vector2 getPosition(Vector2 pos, int alignment) {
		return pos.set(getX(alignment), getY(alignment));
	}
	/** bottom left corner (0,0) is relative to this' instances' bottom left corner*/
	public Vector2 getLocalPosition(Vector2 pos, int alignment) {
		return pos.set(getX(alignment) - x, getY(alignment) - y);
	}


	// -- x --
	public void setX(float x, int alignment) {
		if(Align.isCenterVertical(alignment)) {
			this.x = x - width / 2f;
		}else if(Align.isRight(alignment)) {
			this.x = x + width;
		}
	}
	public float getX(int alignment) {
		float x = this.x;
		if(Align.isCenterVertical(alignment)) {
			x += width / 2f;
		}else if(Align.isRight(alignment)) {
			x += width;
		}
		return x;
	}
	public float getX() {
		return x;
	}


	// -- y --
	public void setY(float y, int alignment) {
		if(Align.isCenterHorizontal(alignment)) {
			this.y = y - height / 2f;
		}else if(Align.isTop(alignment)) {
			this.y = y + height;
		}
	}
	public float getY() {
		return y;
	}
	public float getY(int alignment) {
		float y = this.y;
		if(Align.isCenterHorizontal(alignment)) {
			y += height / 2f;
		}else if(Align.isTop(alignment)) {
			y += height;
		}
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
		if(hasChildren()) {
			for(int i = children.size -1; i>=0; i--) {
				Transform child = children.get(i);
				child.parentToLocalCoord(tmp.set(x,y));
				hit = child.hit(tmp.x, tmp.y);
				if(hit != null)return hit;
			}
		}
		// check itself last
		localToParentCoord(tmp.set(x,y));
		if(getBounds().contains(tmp))return this;
		return null;
	}


	// -- parent --
	public @Null Transform getParent() {
		return parent;
	}
	public boolean hasParent() {
		return parent != null;
	}
	public boolean hasParent(@Null Transform parent) {
		return this.parent == parent;
	}
	public boolean hasParent(@Null Entity entity) {
		if(this.parent != null)return this.parent.entity == entity;
		return entity != null ? false : true;
	}
	/**repositions the transform so that the world position stays the same*/
	public void setParent(@Null Transform parent, boolean reposition) {
		if(!reposition) {
			setParent(parent);
			return;
		}
		localToWorldCoord(tmp.setZero());

		setParent(parent);

		worldToParentCoord(tmp);
		setPosition(tmp.x, tmp.y);
	}
	/**adds the child to its children*/
	public void setParent(@Null Transform parent) {
		if(this.parent == parent)return;
		if(hasParent()) {
			this.parent.children.removeValue(this, true);
		}
		this.parent = parent;
		if(hasParent()) {
			this.parent.children.add(this);
		}
	}


	// -- children --
	public SnapshotArray<Transform> getChildren() {
		return children;
	}
	public boolean hasChildren() {
		return children.size != 0;
	}
	/**
	 *
	 * @param name should be unique among children
	 * @returns the first child/ descendant instance carrying that name. Might be null.
	 */
	public @Null Transform findChild(String name) {
		// check immediate children first
		for(Transform child : children) {
			if(child.tag.equals(name))return child;
		}
		// check tree second
		for(Transform child : children) {
			Transform t = child.findChild(name);
			if(t != null)return t;
		}
		return null;
	}
	public void addChild(Transform ...children) {
		if(children != null)for(Transform child : children)addChild(child);
	}
	public void addChild(Transform child) {
		if(child == null || child == this)return;
		if(child.hasParent()) {
			child.parent.children.removeValue(child, true);
		}
		children.add(child);
		child.parent = this;
	}
	public void addChild(Entity...children) {
		if(children != null)for(Entity child : children)addChild(child);
	}
	public void addChild(Entity child) {
		if(child != null)addChild(child.getComponent(Transform.class));
	}
	public void removeChild(Transform child) {
		if(child == null)return;
		children.removeValue(child, true);
		child.parent = null;
	}


	// -- remove --
	public void removeFromParent() {
		if(!hasParent())return;
		parent.children.removeValue(this, true);
		parent = null;
	}
	public void removeFromWorld(){
		if(!hasParent())return;
		parent.children.removeValue(this, true);
		parent = null;
		world.deleteEntity(entity);
		world = null;
	}


	// -- coordinates --
	public Vector2 worldToScreenCoord(Vector2 worldCoord) {
		Transform transform = getRoot();
		if(transform != null) {
			CanvasComponent canvas = Mappers.CANVAS.get(transform.entity);
			CameraComponent camera = null;
			if(canvas != null && (camera = canvas.getCamera()) != null)
				return camera.project(worldCoord);
		}
		// if no canvas use scene viewport
		return scene.getViewport().project(worldCoord);
	}
	public Vector2 screenToWorldCoord(Vector2 screenCoord) {
		Transform transform = getRoot();
		if(transform != null) {
			CanvasComponent canvas = Mappers.CANVAS.get(transform.entity);
			CameraComponent camera = null;
			if(canvas != null && (camera = canvas.getCamera()) != null)
				return camera.unproject(screenCoord);
		}
		// if no canvas use scene viewport
		return scene.getViewport().unproject(screenCoord);
	}


	public Vector2 screenToParentCoord(Vector2 screenCoord) {
		tmp.set(screenToWorldCoord(screenCoord));
		Transform parent = this.parent;
		while(parent != null) {
			tmp.sub(parent.x, parent.y);
			parent = parent.parent;
		}
		return screenCoord.set(tmp);
	}
	public Vector2 parentToScreenCoord(Vector2 parentCoord) {
		return worldToScreenCoord(parentToWorldCoord(parentCoord));
	}


	public Vector2 worldToLocalCoord(Vector2 worldCoord) {
		tmp.set(worldCoord);
		Transform parent = this;
		while(parent != null) {
			tmp.sub(parent.x, parent.y);
			parent = parent.parent;
		}
		return worldCoord.set(tmp);
	}
	public Vector2 localToWorldCoord(Vector2 localCoord) {
		tmp.set(localCoord);
		Transform parent = this;
		while(parent != null) {
			tmp.add(parent.x, parent.y);
			parent = parent.parent;
		}
		return localCoord.set(tmp);
	}


	public Vector2 localToScreenCoord(Vector2 localCoord) {
		return worldToScreenCoord(localToWorldCoord(localCoord));
	}


	public Vector2 parentToWorldCoord(Vector2 parentCoord) {
		tmp.set(parentCoord);
		Transform parent = this.parent;
		while(parent != null) {
			tmp.add(parent.x, parent.y);
			parent = parent.parent;
		}
		return parentCoord.set(tmp);
	}
	public Vector2 worldToParentCoord(Vector2 worldCoord) {
		tmp.set(worldCoord);
		Transform parent = this.parent;
		while(parent != null) {
			tmp.sub(parent.x, parent.y);
			parent = parent.parent;
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
		return parent == null && world != null && world.getMapper(Transform.class).has(entity);
	}
	/**
	 *
	 * @return the root of  this sub tree that has scene entity as its parent.
	 * Null if parent is null
	 */
	public @Null Transform getRoot() {
		Transform current = this;
		while(current != null) {
			if(current.isRoot())return current;
			current = current.parent;
		}
		return null;
	}



	// -- reset --
	@Override
	protected void reset() {
		children.clear();
		parent = null;
		world = null;

		x = 0;
		y = 0;
		width = 0;
		height = 0;
		tag = "entity";
	}
}
