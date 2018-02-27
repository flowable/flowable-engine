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
package org.flowable.engine.test.jobexecutor;

import java.util.ArrayList;
import java.util.List;

import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RetryFailingDelegate implements JavaDelegate {
    private static final Logger logger = LoggerFactory.getLogger(RetryFailingDelegate.class);

    public static final String EXCEPTION_MESSAGE = "Expected exception.";

    private static int shallThrow;
    private static List<Long> times;

    public static void initialize(int num) {
        shallThrow = num;
        times = new ArrayList<>();
    }
    
    public static int getNumCalls() {
        return times.size();
    }
    
    public static long getTimeDiff() {
        assert times.size() >= 2;
        return times.get(1) - times.get(0);
    }

    @Override
    public void execute(DelegateExecution execution) {
        times.add(System.currentTimeMillis());

        if (shallThrow > 0) {
            logger.info("Throwing exception {} more times", shallThrow);
            shallThrow--;
            throw new FlowableException(EXCEPTION_MESSAGE);
        } else {
            logger.info("Not throwing exception", shallThrow);
        }
    }
}
