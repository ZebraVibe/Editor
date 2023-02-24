package com.sk.editor.ui.inspector;

import com.badlogic.ashley.core.Entity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use this to mark private / protected fields of Components which should be
 * editable/shown in the editor<br>
 * <p>
 * {@link #displayedName()} optional. use this to change the displayed name in the editor, works for (1,2,3). By default
 * returns an empty string
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.FIELD})//, ElementType.TYPE, ElementType.METHOD})
public @interface SerializeField {

    String displayedName() default "";
}
