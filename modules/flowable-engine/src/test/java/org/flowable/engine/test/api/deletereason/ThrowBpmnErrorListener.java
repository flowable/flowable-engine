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
package org.flowable.engine.test.api.deletereason;

import org.flowable.engine.delegate.BpmnError;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.delegate.JavaDelegate;

/** Raises a BpmnError from a start execution listener. */
public class ThrowBpmnErrorListener implements ExecutionListener {

    @Override
    public void notify(DelegateExecution execution) {
        throw new BpmnError("someError", "raised from a start execution listener");
    }

}

/** Never reached; the listener throws before the behaviour runs. */
class NoopDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        // no-op
    }

}
