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

package org.flowable.idm.engine.test;

import org.flowable.common.engine.impl.test.LoggingExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tom Baeyens
 */
@ExtendWith(LoggingExtension.class)
public abstract class AbstractTestCase {

    protected static final String EMPTY_LINE = "\n";

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractTestCase.class);

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
}
