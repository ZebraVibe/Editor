package com.sk.editor.ecs.systems;

import com.artemis.Entity;
import com.artemis.annotations.All;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.SnapshotArray;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.sk.editor.EditorManager;
import com.sk.editor.ecs.components.Transform;
import com.sk.editor.ui.UIStage;
import com.sk.editor.utils.RenderMode;

@All(Transform.class)
public class WorldPropertiesOld extends CanvasSystem {

    // -- attributes --
    private final SnapshotArray<Entity> rootEntities = new SnapshotArray<>();

    final Viewport ecsViewport, viewport;
    private EditorManager editorManager;


    /**
     * whether the camera should be centered on resize
     */
    public boolean centerCamera;

    /**
     * the viewports units per pixel
     */
    public float uppX = 1, uppY = 1;

    // -- constructor --
    public WorldPropertiesOld(Viewport ecsViewport, EditorManager editorManager, UIStage uiStage){
        super(editorManager, ecsViewport);
        this.ecsViewport = ecsViewport;
        this.editorManager = editorManager;
        this.viewport = createViewport();
        init();
    }

    // -- init --
    private void init(){

    }



    // -- root --

    /**
     *
     * @return sorted array of the root entities
     */
    public SnapshotArray<Entity> getRootEntities(){
        return rootEntities;
    }

    /**
     * if the given entity already exists in root entities the old value will be replaced
     * @param entity
     */
    public void addRootEntity(Entity entity){
        insertRootEntity(rootEntities.size, entity);
    }

    /**
     * if the given entity already exists in root entities the old value will be replaced
     * @param index
     * @param entity
     */
    public void insertRootEntity(int index, Entity entity){
        index = Math.min(index, rootEntities.size);
        if(isRoot(entity) == false){ // not previously root
            rootEntities.insert(index, entity);
            return;
        }
        Entity before = index == 0 ? null : rootEntities.get(index -1);
        rootEntities.removeValue(entity, true);
        rootEntities.insert(before == null ? 0 : rootEntities.indexOf(before, true) + 1, entity);
    }

    public boolean removeRootEntity(Entity entity){
        return rootEntities.removeValue(entity, true);
    }

    public boolean isRoot(Entity entity){
        return rootEntities.contains(entity, true);
    }




    // -- viewport --
    private Viewport createViewport(){
        Viewport viewport = new Viewport() {

            @Override
            public void update(int screenWidth, int screenHeight, boolean centerCamera) {
                RenderMode mode = editorManager.getRenderMode();

                if(mode.isEdit()) {
                    OrthographicCamera ecsCamera = (OrthographicCamera) ecsViewport.getCamera();
                    OrthographicCamera worldCamera = (OrthographicCamera) getCamera();
                    worldCamera.position.set(ecsCamera.position);
                    worldCamera.position.scl(uppX, uppY, 1);
                    worldCamera.zoom = ecsCamera.zoom;
                    worldCamera.update();

                    // screen viewport update logic
                    setScreenBounds(0, 0, screenWidth, screenHeight);
                    setWorldSize(screenWidth * uppX, screenHeight * uppY);
                    apply(false);

                } else { // also game mode
                    super.update(screenWidth, screenHeight, centerCamera);
                }
            }


        };
        viewport.setCamera(new OrthographicCamera());
        return viewport;
    }


    /**
     * sets the viewports' units per pixel
     * @param uppX
     * @param uppY
     */
    public void setViewportUPP(float uppX, float uppY){
        this.uppX = uppX;
        this.uppY = uppY;
    }
    public void updateViewport(){
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), centerCamera);
    }

    public OrthographicCamera getCamera(){return (OrthographicCamera) viewport.getCamera();}

    //public Viewport getViewport(){return viewport;}

    /**
     * @param worldCoord
     * @return passed in vector transformed to screen coord
     */
    public Vector2 project(Vector2 worldCoord){
        viewport.project(worldCoord);
        worldCoord.y = viewport.getScreenHeight() - worldCoord.y;
        return worldCoord;
    }
    /**
     * @param screenCoord
     * @return passed in vector transformed to world coord
     */
    public Vector2 unproject(Vector2 screenCoord){
        return viewport.unproject(screenCoord);
    }
    /**
     * @return the combined projection and view matrix of the camera
     */
    public Matrix4 combined(){
        return viewport.getCamera().combined;
    }

    public int getScreenWidth(){
        return viewport.getScreenWidth();
    }

    public int getScreenHeight(){
        return viewport.getScreenHeight();
    }


    // -- private methods --
    @Override
    protected void processSystem() {}



}
