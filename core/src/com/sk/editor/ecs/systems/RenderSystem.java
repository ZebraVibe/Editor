package com.sk.editor.ecs.systems;

import com.artemis.ComponentMapper;
import com.artemis.annotations.All;
import com.artemis.annotations.Wire;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Pools;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.sk.editor.EditorManager;
import com.sk.editor.ecs.components.Canvas;
import com.sk.editor.ecs.components.Image;
import com.sk.editor.ecs.components.Transform;

@Wire(injectInherited = true)
@All({Transform.class, Canvas.class})
public class RenderSystem extends CanvasSystem {
	
	ComponentMapper<Transform> transformMapper;
	ComponentMapper<Canvas> canvasMapper;
	ComponentMapper<Image> imageMapper;
	private SpriteBatch batch;
	
	public RenderSystem(SpriteBatch batch, EditorManager editorManager, Viewport ecsViewport) {
		super(editorManager, ecsViewport);
		this.batch = batch;
	}

	@Override
	protected void processSystem() {
		//setup batch
		boolean isDrawing = batch.isDrawing();
		if(!isDrawing){
			ScreenUtils.clear(0,0,0,1);
			batch.begin();
		} else batch.flush();
		batch.setColor(1,1,1,1);

		// process
		super.processSystem();

		//reset batch
		if(!isDrawing)batch.end();
		else batch.flush();
	}

	@Override
	protected void processRootCanvas(int entityId, Transform transform, Canvas canvas) {
		batch.flush();
		batch.setProjectionMatrix(canvas.combined());

		// processes canvases and applies the viewport
		super.processRootCanvas(entityId, transform, canvas);
	}

	@Override
	protected void process(int entityId) {
		Transform transform = transformMapper.getSafe(entityId, null);
		Image image = imageMapper.getSafe(entityId, null);
		if(transform == null)return;

		batch.flush();

		// draw self
		if(image != null && image.region != null){
			Vector2 worldCoord = Pools.obtain(Vector2.class);
			worldCoord.set(transform.x, transform.y);
			transform.parentToWorldCoord(worldCoord); // TODO: improve with transform matrix ?

			batch.draw(image.region,
					worldCoord.x,
					worldCoord.y,
					0,
					0,
					transform.width,
					transform.height,
					1f,
					1f,
					0);
			batch.flush();
			Pools.free(worldCoord);
		}

		// sort & process children
		super.process(entityId);
	}

}
