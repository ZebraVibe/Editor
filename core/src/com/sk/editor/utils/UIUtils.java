package com.sk.editor.utils;


import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FileTextureData;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Logger;
import com.badlogic.gdx.utils.Null;
import com.badlogic.gdx.utils.reflect.*;
import com.sk.editor.ui.inspector.InvokeMethod;
import com.sk.editor.ui.inspector.SerializeField;

public class UIUtils {

    private static final Logger log = new Logger(UIUtils.class.getName(), Logger.DEBUG);

    private static final Array<Object> tmpObjects = new Array<Object>();

    private UIUtils() {
    }


    /***
     *
     * @param name
     * @param maxLength negatvie to indicate no max length
     * @return
     */
    public static String getFormattedName(String name, int maxLength) {
        if (name == null || maxLength < 0 || name.length() <= maxLength) return name;
        else {
            String tmp = "";
            tmp = name.substring(0, maxLength - 3);
            tmp += "...";
            return tmp;
        }
    }

    /**
     * @param region
     * @param maxLength negative to indicate no max length.
     * @return an empty string if region is null
     */
    public static String getNameOfRegion(TextureRegion region, int maxLength) {
        if (region == null) return "";

        if (region instanceof AtlasRegion) {
            AtlasRegion atlasRegion = (AtlasRegion) region;
            return atlasRegion.name;
        }


        TextureData data = region.getTexture().getTextureData();
        if (data instanceof FileTextureData) {
            FileTextureData fileData = (FileTextureData) data;
            String name = fileData.getFileHandle().nameWithoutExtension();
            name = UIUtils.getFormattedName(name, maxLength);
            return name;
        }
        log.error("unknown region name when tried to create name from texture");
        return "unknown";
    }

    public static String getNameOfRegion(TextureRegion region) {
        return getNameOfRegion(region, -1);
    }

    public static boolean hasDeclaredAnnotation(Field field, Class<? extends java.lang.annotation.Annotation> annotationType) {
        return getDeclaredAnnotation(field, annotationType) != null;
    }


    public static Field getDeclaredFieldOf(Class<?> cls, String name) throws ReflectionException {
        //cls.getDeclaredFields() // to read about it
        return ClassReflection.getDeclaredField(cls, name);
    }

    /**
     * @param cls
     * @return all fields that are public or marked with @{@link SerializeField}
     */
    public static Field[] getFieldsOf(Class<?> cls) {
        Field[] fields = ClassReflection.getDeclaredFields(cls); // all fields
        tmpObjects.clear();
        for (Field field : fields) {
            // isAccess is for sth else and canAccess is not included in badlogic field class
            if (field.isFinal() ||
                    (!field.isPublic() && !hasDeclaredAnnotation(field, SerializeField.class))) continue;
            tmpObjects.add(field);

        }
        Field[] fs = tmpObjects.toArray(Field.class);//tmpFields.toArray(Field.class);
        tmpObjects.clear();
        return fs;
    }


    /**
     * handles the accessebility of private fields
     */
    public static Object getFieldValue(Field field, Object obj) {
        Object value = null;
        try {
            field.setAccessible(true); // disable checks
            value = field.get(obj);
            field.setAccessible(false); // enable checks
        } catch (ReflectionException e) {
            log.error("couldnt get value of field " + field);
            e.printStackTrace();
        }
        return value;
    }


    /**
     * handles the accessebility of private fields
     */
    public static void setFieldValue(Field field, Object obj, Object newValue) {
        if (field.isFinal())
            return;
        try {
            field.setAccessible(true);
            field.set(obj, newValue);
            invokeMethodOfInvokeMethodAnnotation(obj, field);
            field.setAccessible(false);
        } catch (ReflectionException e) {
            log.error("couldnt set value of field " + field);
            e.printStackTrace();
        }
    }

    /**
     * @param field
     * @return the {@link SerializeField#displayedName()} if present else {@link Field#getName()}
     */
    public static String getNameOfSerializedFieldIfPresent(Field field) {
        SerializeField serial = getDeclaredAnnotation(field, SerializeField.class);
        if (hasDisplayedName(serial)) return serial.displayedName();
        return field.getName();
    }

    /**
     * @param serial
     * @return whether the field has been given a @{@link SerializeField#displayedName()};
     */
    public static boolean hasDisplayedName(SerializeField serial){
        return serial != null ? !serial.displayedName().isEmpty() : false;
    }

    public static @Null <T extends java.lang.annotation.Annotation> T getDeclaredAnnotation(Field field, java.lang.Class<T> annotationType){
        Annotation anno = field.getDeclaredAnnotation(annotationType);
        return anno != null ? anno.getAnnotation(annotationType): null;
    }


    /**
     *
     * @param obj the object from which to invoke the method
     * @param field the field carrying the annotation
     */
    public static void invokeMethodOfInvokeMethodAnnotation(Object obj, Field field) {
        Annotation anno = field.getDeclaredAnnotation(InvokeMethod.class);
        if (anno == null)
            return;
        InvokeMethod call = anno.getAnnotation(InvokeMethod.class);
        String methodName = call.name();
        try {
            Method methodField = ClassReflection.getDeclaredMethod(obj.getClass(), methodName);
            methodField.setAccessible(true);
            methodField.invoke(obj);
            methodField.setAccessible(false);
        } catch (Exception e) {
            log.error("Could not find/call the method " + methodName + " provided in annotation "
                    + InvokeMethod.class.getSimpleName() + ". Error: " + e.toString());
        }

    }


}
