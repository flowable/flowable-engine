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
package org.flowable.engine.impl.delegate;

import org.flowable.engine.delegate.DelegateExecution;

/**
 * Similar to the {@link TriggerableActivityBehavior} but with a context that allows the implementing class
 * to decide if the execution should be left after the trigger or not
 * @author Christopher Welsch
 */
public interface TriggerableJavaDelegate {

    void trigger(TriggerableJavaDelegate.Context context);

    interface Context {

        void doNotLeave();

        Object getSignalData();

        DelegateExecution getExecution();

        String getSignalName();
    }
}
