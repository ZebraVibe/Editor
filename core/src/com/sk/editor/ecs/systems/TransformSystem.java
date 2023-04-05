package com.sk.editor.ecs.systems;

import com.artemis.BaseEntitySystem;
import com.artemis.ComponentMapper;
import com.artemis.annotations.All;
import com.badlogic.gdx.utils.Logger;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Field;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import com.sk.editor.ecs.components.Transform;
import com.sk.editor.ui.logger.EditorLogger;

@All(Transform.class)
public class TransformSystem extends BaseEntitySystem {

    private static final EditorLogger log = new EditorLogger(TransformSystem.class.toString(), Logger.DEBUG);

    ComponentMapper<Transform> transformMapper;

    @Override
    protected void processSystem() {}

    @Override
    protected void inserted(int entityId) {
        try {
            Field field = ClassReflection.getDeclaredField(Transform.class, "entity");
            Transform transform = transformMapper.get(entityId);
            field.setAccessible(true);
            field.set(transform, getWorld().getEntity(entityId));
            field.setAccessible(false);
        } catch (ReflectionException e) {
            log.error("Could not initialize entity field!", e);
        }
    }
}
