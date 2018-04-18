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
package org.flowable.common.engine.impl.util;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Robert Hafner
 */
public class TestClockImpl extends DefaultClockImpl {
    protected static final Logger LOGGER = LoggerFactory.getLogger(TestClockImpl.class);
    protected static volatile Date PREVIOUS_TIME;

    @Override
    public Date getCurrentTime() {
        Date currentTime = super.getCurrentTime();

        if(CURRENT_TIME == null) {
            if(currentTime.equals(PREVIOUS_TIME)) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    LOGGER.warn("Failed to sleep for 1ms", e);
                }
                currentTime = super.getCurrentTime();
            }
        }

        PREVIOUS_TIME = currentTime;
        return currentTime;
    }
}
