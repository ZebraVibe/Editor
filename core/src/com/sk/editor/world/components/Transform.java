package com.sk.editor.world.components;

import com.artemis.annotations.PooledWeaver;
import com.badlogic.gdx.math.Rectangle;
import com.sk.editor.ui.inspector.InvokeMethod;
import com.sk.editor.ui.inspector.SerializeField;

@PooledWeaver
public class Transform extends BaseComponent{

	/**world coordinates of bottom left corner*/
	public float x, y;
	public float width, height;
	private final Rectangle bounds = new Rectangle();

	/**recalculates the bounds on each call*/
	public Rectangle getBounds() {
		return bounds.set(x, y, width, height);
	}

	/**
	 * sets the world coordinates
	 * @param x
	 * @param y
	 */
	public void setPosition(float x, float y){
		this.x = x;
		this.y = y;
	}

	public void setSize(float width, float height){
		this.width = width;
		this.height = height;
	}

	@Override
	protected void reset() {
		x = 0;
		y = 0;
		width = 0;
		height = 0;
	}
}
