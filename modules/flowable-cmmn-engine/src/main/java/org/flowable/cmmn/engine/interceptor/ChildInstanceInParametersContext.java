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

import java.util.Map;

import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.model.ChildTask;

/**
 * The variables a process task or case task starts its child instance with, see
 * {@link CmmnChildInstanceParametersInterceptor#afterInParameters(ChildInstanceInParametersContext)}.
 */
public class ChildInstanceInParametersContext {

    protected DelegatePlanItemInstance planItemInstance;
    protected ChildTask childTask;
    protected Map<String, Object> variables;
    protected Map<String, Object> transientVariables;

    public ChildInstanceInParametersContext(DelegatePlanItemInstance planItemInstance, ChildTask childTask, Map<String, Object> variables,
            Map<String, Object> transientVariables) {

        this.planItemInstance = planItemInstance;
        this.childTask = childTask;
        this.variables = variables;
        this.transientVariables = transientVariables;
    }

    /** The plan item instance of the process task or case task, in the parent case instance. */
    public DelegatePlanItemInstance getPlanItemInstance() {
        return planItemInstance;
    }

    /** The process task or case task. */
    public ChildTask getChildTask() {
        return childTask;
    }

    /** The variables the child instance starts with. Changes to this map are passed to the child instance. */
    public Map<String, Object> getVariables() {
        return variables;
    }

    /** The transient variables the child instance starts with. Changes to this map are passed to the child instance. */
    public Map<String, Object> getTransientVariables() {
        return transientVariables;
    }
}
