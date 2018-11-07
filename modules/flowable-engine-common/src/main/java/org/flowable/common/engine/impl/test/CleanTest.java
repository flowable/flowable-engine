package org.flowable.common.engine.impl.test;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that instructs the internal flowable extensions to clean all deployments
 * of a test.
 * When used on a test class it will remove all deployments after each method. When used on a method it will remove the deployments only for that method.
 * This annotation is handled different depending on the {@link org.junit.jupiter.api.TestInstance.Lifecycle TestInstance.Lifecycle} and it's location.
 * <ul>
 *      <li>
 *          Class annotated and {@link org.junit.jupiter.api.TestInstance.Lifecycle#PER_CLASS TestInstance.Lifecycle#PER_CLASS}
 *          - the clean would happen after all tests have run
 *      </li>
 *      <li>
 *          Class annotated and {@link org.junit.jupiter.api.TestInstance.Lifecycle#PER_METHOD TestInstance.Lifecycle#PER_METHOD} or no lifecycle
 *          - the clean would happen after each test has run
 *      </li>
 *      <li>
 *          Method annotated - the clean would happen after the test has run
 *      </li>
 * </ul>
 *
 * @author Filip Hrisafov
 */
@Target({
    ElementType.TYPE,
    ElementType.METHOD
})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface CleanTest {

}
