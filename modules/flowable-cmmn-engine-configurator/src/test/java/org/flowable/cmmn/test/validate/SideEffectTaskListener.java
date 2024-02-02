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
package org.flowable.cmmn.test.validate;

import java.util.concurrent.atomic.AtomicInteger;

import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;

/**
 * @author martin.grofcik
 */
public class SideEffectTaskListener implements TaskListener {

    protected static AtomicInteger sideEffect = new AtomicInteger(0);

    public static void reset() {
        sideEffect.set(0);
    }

    public static int getSideEffect() {
        return sideEffect.get();
    }

    @Override
    public void notify(DelegateTask delegateTask) {
        sideEffect.incrementAndGet();
    }
}
