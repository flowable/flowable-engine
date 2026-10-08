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
package org.flowable.cmmn.engine.interceptor;

import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.model.ChildTask;
import org.flowable.common.engine.api.variable.VariableContainer;

/**
 * The completed child instance of a process task or case task, see
 * {@link CmmnChildInstanceParametersInterceptor#afterOutParameters(ChildInstanceOutParametersContext)}.
 */
public class ChildInstanceOutParametersContext {

    protected DelegatePlanItemInstance planItemInstance;
    protected ChildTask childTask;
    protected VariableContainer childInstance;

    public ChildInstanceOutParametersContext(DelegatePlanItemInstance planItemInstance, ChildTask childTask, VariableContainer childInstance) {
        this.planItemInstance = planItemInstance;
        this.childTask = childTask;
        this.childInstance = childInstance;
    }

    /** The plan item instance of the process task or case task, in the parent case instance: where the outcome is written. */
    public DelegatePlanItemInstance getPlanItemInstance() {
        return planItemInstance;
    }

    /** The process task or case task. */
    public ChildTask getChildTask() {
        return childTask;
    }

    /** The completed child process or case instance, to read its variables from. */
    public VariableContainer getChildInstance() {
        return childInstance;
    }
}
