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
 * @author Christopher Welsch
 */
public class TriggerableJavaDelegateContextImpl implements TriggerableJavaDelegate.Context {

    protected DelegateExecution execution;
    protected String signalName;
    protected Object signalData;

    protected boolean shouldLeave = true;

    public TriggerableJavaDelegateContextImpl(DelegateExecution execution, String signalName, Object signalData) {
        this.execution = execution;
        this.signalName = signalName;
        this.signalData = signalData;
    }
    @Override
    public DelegateExecution getExecution() {
        return execution;
    }

    public void setExecution(DelegateExecution execution) {
        this.execution = execution;
    }

    @Override
    public String getSignalName() {
        return signalName;
    }

    public void setSignalName(String signalName) {
        this.signalName = signalName;
    }
    @Override
    public Object getSignalData() {
        return signalData;
    }

    public void setSignalData(Object signalData) {
        this.signalData = signalData;
    }

    @Override
    public void doNotLeave() {
        shouldLeave = false;
    }

    public boolean shouldLeave() {
        return shouldLeave;
    }
}
