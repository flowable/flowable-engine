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

package org.flowable.content.engine.impl.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.TestCase;

/**
 * @author Tom Baeyens
 */
public abstract class AbstractContentTestCase extends TestCase {

    protected static final String EMPTY_LINE = "\n";

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractContentTestCase.class);

    protected boolean isEmptyLinesEnabled = true;

    /**
     * Asserts if the provided text is part of some text.
     */
    public void assertTextPresent(String expected, String actual) {
        if ((actual == null) || (!actual.contains(expected))) {
            throw new AssertionError("expected presence of [" + expected + "], but was [" + actual + "]");
        }
    }

    /**
     * Asserts if the provided text is part of some text, ignoring any uppercase characters
     */
    public void assertTextPresentIgnoreCase(String expected, String actual) {
        assertTextPresent(expected.toLowerCase(), actual.toLowerCase());
    }

    @Override
    protected void runTest() throws Throwable {
        if (LOGGER.isDebugEnabled()) {
            if (isEmptyLinesEnabled) {
                LOGGER.debug(EMPTY_LINE);
            }
            LOGGER.debug("#### START {}.{} ###########################################################", this.getClass().getSimpleName(), getName());
        }

        try {

            super.runTest();

        } catch (AssertionError e) {
            LOGGER.error(EMPTY_LINE);
            LOGGER.error("ASSERTION FAILED: {}", e, e);
            throw e;

        } catch (Throwable e) {
            LOGGER.error(EMPTY_LINE);
            LOGGER.error("EXCEPTION: {}", e, e);
            throw e;

        } finally {
            LOGGER.debug("#### END {}.{} #############################################################", this.getClass().getSimpleName(), getName());
        }
    }

}
