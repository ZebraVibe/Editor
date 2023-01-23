package com.sk.editor.ui;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.SnapshotArray;

/**
 * add listeners that get informed whenever the camera is updated
 */
public class NotifyingOrthographicCamera extends OrthographicCamera {

    private final SnapshotArray<CameraListener> cameraListeners = new SnapshotArray<>();


    @Override
    public void update() {
        super.update();
        notifyListeners();
    }


    // -- public methods --

    public void addCameraListener(CameraListener listener){
        if(listener == null)return;
        if(!cameraListeners.contains(listener, true)){
            cameraListeners.add(listener);
        }
    }

    public boolean removeCameraListener(CameraListener listener){
        return cameraListeners.removeValue(listener, true);
    }

    public static interface CameraListener{
        /** called whenever the camera is updated */
        void updated(OrthographicCamera camera);
    }

    // -- private --

    /**
     * notifies the camera listeners that the camera has been updated
     */
    private void notifyListeners(){
        Object[] items = cameraListeners.begin();
        for(int i = 0, n = cameraListeners.size; i < n; i++){
            CameraListener listener = (CameraListener)items[i];
            listener.updated(this);
        }
        cameraListeners.end();
    }

}
