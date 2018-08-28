package org.flowable.form.engine.test;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation that can be used on test methods, lifecycle method to inject the id of the deployment deployed via {@link FormDeploymentAnnotation}.
 *
 * <b>NB:</b> This only works for the tests with JUnit Jupiter.
 *
 * @author Filip Hrisafov
 */
@Target({ ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FormDeploymentId {

}
