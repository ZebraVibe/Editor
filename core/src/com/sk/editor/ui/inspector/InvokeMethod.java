package com.sk.editor.ui.inspector;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 
 * Apply this on public attributes which can be edited in the editor.
 * Provide a method name without brackets, to indicate if a method should
 * be invoked when the attribute is changed in the editor. The method must be declared 
 * in the same class without parameters. Method can be public or private<br>
 * <p>
 * Example:
 * <pre>
 * @InvokeMethod(name="sort")
 * public int layer;
 * 
 * public void sort(){
 * 	//...
 * }
 * </pre>
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = ElementType.FIELD)
public @interface InvokeMethod {
	String name();
}
