package com.sk.editor.ecs.utils;

import com.artemis.World;
import com.artemis.io.ComponentLookupSerializer;
import com.artemis.io.JsonArtemisSerializer;
import com.artemis.io.SaveFileFormat;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Field;
import com.badlogic.gdx.utils.reflect.Method;
import com.badlogic.gdx.utils.reflect.ReflectionException;

public class CustomJsonArtemisSerializer extends JsonArtemisSerializer {
    public CustomJsonArtemisSerializer(World world) {
        super(world);
        // override registration
        ComponentLookupSerializer serializer = createComponentLookupSerializer();
        // for future safety sake override field
        Field lookup = null;
        try {
            lookup = ClassReflection.getDeclaredField(JsonArtemisSerializer.class, "lookup");
            lookup.setAccessible(true);
            lookup.set(this, serializer);
            lookup.setAccessible(false);
        } catch (ReflectionException e) {
            throw new RuntimeException(e);
        }
        register(SaveFileFormat.ComponentIdentifiers.class, serializer);
    }


    /**
     * to be able to de-/serialize ScriptManagers' classLoader classes
     * @return
     */
    private ComponentLookupSerializer createComponentLookupSerializer(){
        return new ComponentLookupSerializer(){
            @Override
            public SaveFileFormat.ComponentIdentifiers read(Json json, JsonValue jsonData, Class type) {
                SaveFileFormat.ComponentIdentifiers ci = new SaveFileFormat.ComponentIdentifiers();

                JsonValue component = jsonData.child;
                try {
                    while (component != null) {
                        // use context class loader (set by ScriptManager) instead of artemis Reflection class loader
                        // which includes external project classes to be able to de-/serialize ScriptManagers' classLoader classes
                        //Class c = ClassReflection.forName(component.name());
                        Class c = Class.forName(component.name(), true, Thread.currentThread().getContextClassLoader());
                        ci.typeToName.put(c, component.asString());
                        component = component.next;
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                Method build = null;
                try {
                    build = ClassReflection.getDeclaredMethod(SaveFileFormat.ComponentIdentifiers.class, "build");
                } catch (ReflectionException e) {
                    throw new RuntimeException(e);
                }

                try {
                    build.setAccessible(true);
                    build.invoke(ci);
                    build.setAccessible(false);
                } catch (ReflectionException e) {
                    throw new RuntimeException(e);
                }

                //ci.build();
                return ci;

                //return super.read(json, jsonData, type);
            }
        };
    }
}
