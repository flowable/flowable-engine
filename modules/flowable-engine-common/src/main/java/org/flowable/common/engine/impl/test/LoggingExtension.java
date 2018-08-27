/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.common.engine.impl.test;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Filip Hrisafov
 */
public class LoggingExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingExtension.class);
    private static final String EMPTY_LINE = "\n";

    @Override
    public void beforeTestExecution(ExtensionContext context) throws Exception {
        LOGGER.debug(EMPTY_LINE);
        LOGGER.debug("#### START {}.{} ###########################################################", context.getRequiredTestClass().getSimpleName(),
            context.getRequiredTestMethod().getName());
    }

    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception {
        context.getExecutionException().ifPresent(LoggingExtension::logExecutionException);

        LOGGER.debug("#### END {}.{} ###########################################################", context.getRequiredTestClass().getSimpleName(),
            context.getRequiredTestMethod().getName());
        LOGGER.debug(EMPTY_LINE);
    }

    protected static void logExecutionException(Throwable ex) {
        if (ex instanceof AssertionError) {
            LOGGER.error(EMPTY_LINE);
            LOGGER.error("ASSERTION FAILED: {}", ex, ex);
        } else {
            LOGGER.error(EMPTY_LINE);
            LOGGER.error("EXCEPTION: {}", ex, ex);
        }
    }
}
