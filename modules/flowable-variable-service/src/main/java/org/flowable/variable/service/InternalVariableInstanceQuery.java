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
package org.flowable.variable.service;

import java.util.Collection;
import java.util.List;

import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

/**
 * @author Filip Hrisafov
 */
public interface InternalVariableInstanceQuery {

    /**
     * Query variables with the given id.
     */
    InternalVariableInstanceQuery id(String id);

    /**
     * Query variables with the given task id
     */
    InternalVariableInstanceQuery taskId(String taskId);

    /**
     * Query variables with the given task ids
     */
    InternalVariableInstanceQuery taskIds(Collection<String> taskIds);

    /**
     * Query variables with the given process instance id
     */
    InternalVariableInstanceQuery processInstanceId(String processInstanceId);

    /**
     * Query variables with the given execution id
     */
    InternalVariableInstanceQuery executionId(String executionId);

    /**
     * Query variables with the given execution ids
     */
    InternalVariableInstanceQuery executionIds(Collection<String> executionIds);

    /**
     * Query variables without a task id.
     * Cannot be used together with {@link #taskId(String)} or {@link #taskIds(Collection)}.
     */
    InternalVariableInstanceQuery withoutTaskId();

    /**
     * Query variables with the given scope ids.
     */
    InternalVariableInstanceQuery scopeIds(Collection<String> scopeIds);

    /**
     * Query variables with the given scope id.
     */
    InternalVariableInstanceQuery scopeId(String scopeId);

    /**
     * Query variables with the given sub scope id.
     * Cannot be used together with {@link #withoutSubScopeId()}
     */
    InternalVariableInstanceQuery subScopeId(String subScopeId);

    /**
     * Query variables with the given sub scope ids.
     * Cannot be used together with {@link #withoutSubScopeId()}
     */
    InternalVariableInstanceQuery subScopeIds(Collection<String> subScopeIds);

    /**
     * Query variables without a sub scope id.
     * Cannot be used together with {@link #subScopeId(String)}
     */
    InternalVariableInstanceQuery withoutSubScopeId();

    /**
     * Query variables with the given scope type
     */
    InternalVariableInstanceQuery scopeType(String scopeType);

    /**
     * Query variables with the given scope types
     */
    InternalVariableInstanceQuery scopeTypes(Collection<String> scopeTypes);

    /**
     * Query variables with the given name
     */
    InternalVariableInstanceQuery name(String name);

    /**
     * Query variables with the given name
     */
    InternalVariableInstanceQuery names(Collection<String> names);

    List<VariableInstanceEntity> list();

    VariableInstanceEntity singleResult();
}
