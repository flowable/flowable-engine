package org.flowable.rest.service;

import org.flowable.engine.test.FlowableRule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Filip Hrisafov
 */
public class BaseSpringRestRule extends FlowableRule implements TestRule {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseSpringRestRule.class);

    private Throwable exception;

    @Override
    protected void failed(Throwable e, Description description) {
        super.failed(e, description);
        if (e instanceof AssertionError) {
            LOGGER.error("\n");
            LOGGER.error("ASSERTION FAILED: {}", e, e);
            exception = e;
        } else if (e != null) {
            LOGGER.error("\n");
            LOGGER.error("EXCEPTION: {}", e, e);
            exception = e;
        }
    }
    public Throwable getException() {
        return exception;
    }
}
