package org.flowable.common.engine.impl.test;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that instructs the internal flowable extensions to assert a clean DB (scans all tables to see if the DB is completely clean).
 * It throws {@link AssertionError} in case the DB is not clean. If the DB is not clean, it would be cleaned by performing a create and drop.
 * Dropping the DB can be disabled by setting {@link EnsureCleanDb#dropDb()} to {@code false}.
 * If the {@link org.junit.jupiter.api.TestInstance.Lifecycle TestInstance.Lifecycle} is set to
 * {@link org.junit.jupiter.api.TestInstance.Lifecycle#PER_CLASS TestInstance.Lifecycle#PER_CLASS} then the assertion will happen after all tests have run.
 *
 * @author Filip Hrisafov
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface EnsureCleanDb {

    /**
     * The names of the tables that should be excluded from a check
     *
     * @return the table names that should be excluded when doing the check
     */
    String[] excludeTables() default {};

    boolean dropDb() default true;

}
