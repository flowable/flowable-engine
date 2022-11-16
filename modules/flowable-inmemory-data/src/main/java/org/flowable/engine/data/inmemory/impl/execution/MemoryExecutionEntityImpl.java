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
package org.flowable.engine.data.inmemory.impl.execution;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl;

/**
 * Thread safe version of ExecutionEntityImpl for use with
 * {@link MemoryExecutionDataManager}
 * 
 * @author ikaakkola (Qvantel Finland Oy)
 */
public class MemoryExecutionEntityImpl extends ExecutionEntityImpl {

    private static final long serialVersionUID = 1L;

    /**
     * Static factory method: to be used when a new execution is created for the
     * very first time/ Calling this will make sure no extra db fetches are
     * needed later on, as all collections will be populated with empty
     * collections. If they would be null, it would trigger a database fetch for
     * those relationship entities.
     *
     * @return entity
     */
    public static MemoryExecutionEntityImpl createWithEmptyRelationshipCollections() {
        MemoryExecutionEntityImpl execution = new MemoryExecutionEntityImpl();
        execution.executions = new ArrayList<>(1);
        execution.eventSubscriptions = new ArrayList<>(1);
        execution.identityLinks = new ArrayList<>(1);
        // Use a concurrent map for variable instances as the Memory
        // datamanagers can cause some concurrent modification issues in
        // execution entities due to faster operations and shared entity
        // instances (a select from SQL always returns a new instance,
        // but the in-memory datamanagers do not create new instances)
        execution.variableInstances = new ConcurrentHashMap<>(1);
        return execution;
    }

    @Override
    public String toString() {
        return "MemoryExecution[ id=" + getId() + ", processInstanceId=" + getProcessInstanceId() + ", parentId=" + getParentId() + ", rootProcessInstanceId="
                        + getRootProcessInstanceId() + ", superExecutionId=" + getSuperExecutionId() + ", currentActivityId=" + getCurrentActivityId() + "]";
    }
}
