package me.scf37.fine.route.docs;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;

/**
 * Annotation to define description of class or field for route documentation
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(value={FIELD, TYPE})
public @interface Description {
    String value();
}
