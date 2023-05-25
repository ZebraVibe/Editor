package com.sk.editor.ui.hierarchy;

import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import com.sk.editor.assets.RegionNames;
import com.sk.editor.ui.UINode;
import com.sk.editor.ecs.components.Transform;

public class EntityNode extends UINode<EntityNode, Entity>{

    Transform transform;
    String lastTag;


    public EntityNode(Entity entity, Skin skin){
        super("",skin);

        ComponentMapper<Transform> transformMapper = entity.getWorld().getMapper(Transform.class);
        transform = transformMapper.get(entity);
        setText(transform != null ? transform.getName() : "node");
        setValue(entity);
        setIcon(skin.getDrawable(RegionNames.ENTITY_ICON));

        lastTag = transform.getName();

    }

    @Override
    protected Label newLabel(String text, Skin skin) {
        return new Label(text, skin){
            @Override
            public void act(float delta) {
                if(hasEntityTagChanged()){
                    // only set label text
                    EntityNode.this.setText(transform.getName(), false);
                }
                super.act(delta);
            }
        };
    }

    private boolean hasEntityTagChanged() {
        String currentTag = transform.getName();
        if(lastTag.equals(currentTag) == false){
            lastTag = currentTag;
            return true;
        }
        return false;
    }


    /**
     * @param text
     * @param changeEntityName if true sets it for the entity as well
     * @throws ReflectionException
     */
    public void setText(String text, boolean changeEntityName) {
        if(changeEntityName){
            transform.setName(text);
        }
        setText(text);
    }
}
