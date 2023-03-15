package com.sk.editor.ecs.world.systems;

import com.artemis.BaseEntitySystem;
import com.artemis.ComponentMapper;
import com.artemis.annotations.All;
import com.artemis.utils.IntBag;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.sk.editor.ecs.world.components.Image;
import com.sk.editor.ecs.world.components.Transform;

@All({Transform.class, Image.class})
public class RenderSystem extends BaseEntitySystem {
	
	private ComponentMapper<Transform> transformMapper;

	private ComponentMapper<Image> imageMapper;

	private SpriteBatch batch;
	
	public RenderSystem(SpriteBatch batch) {
		this.batch = batch;
	}

	@Override
	protected void processSystem() {
		IntBag actives = getEntityIds();
		int[] ids = actives.getData();

		//setup batch
		boolean isDrawing = batch.isDrawing();
		if(!isDrawing){
			ScreenUtils.clear(0,0,0,1);
			batch.begin();
		} else batch.flush();
		batch.setColor(1,1,1,1);

		// draw entities
		for (int i = 0, s = actives.size(); s > i; i++) {
			process(ids[i]);
		}

		//reset batch
		if(!isDrawing)batch.end();
		else batch.flush();
	}

	protected void process(int entityId) {
		Transform transform = transformMapper.getSafe(entityId, null);
		Image image = imageMapper.getSafe(entityId, null);
		if(transform == null)return;

		batch.flush();

		// draw self
		if(image != null || image.region != null){
			batch.draw(image.region,
					transform.x, // global world position
					transform.y,
					0,
					0,
					transform.width,
					transform.height,
					1f,
					1f,
					0);
			batch.flush();
		}
		
	}
	
	@Override
	public void inserted(IntBag entities) {
		//super.inserted(entities);
		sort();
	}
	
	@Override
	public void removed(IntBag entities) {
		//super.removed(entities);
		sort();
	}
	
	private void sort() {
		
	}

}
