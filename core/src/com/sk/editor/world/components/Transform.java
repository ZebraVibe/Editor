package com.sk.editor.world.components;

import com.artemis.annotations.PooledWeaver;
import com.badlogic.gdx.math.Rectangle;

@PooledWeaver
public class Transform extends BaseComponent{

	public float
			x, y, // world position
			width, height;
	private final Rectangle bounds = new Rectangle();


	/**recalculates the bounds on each call*/
	public Rectangle getBounds() {
		return bounds.set(x, y, width, height);
	}

	/**
	 * sets the world position
	 * @param x
	 * @param y
	 */
	public void setPosition(float x, float y){
		this.x = x;
		this.y = y;
	}
}
