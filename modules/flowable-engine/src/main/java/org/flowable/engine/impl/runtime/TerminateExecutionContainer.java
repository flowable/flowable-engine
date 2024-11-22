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
package org.flowable.engine.impl.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.flowable.engine.impl.persistence.entity.ExecutionEntity;

/**
 * @author Matthias Stöckli
 */
public class TerminateExecutionContainer {

    protected List<String> executionIds = new ArrayList<>();
    protected List<ExecutionEntity> executions = new ArrayList<>();

    public TerminateExecutionContainer() {
    }

    public TerminateExecutionContainer(String singleExecutionId) {
        this.executionIds = Collections.singletonList(singleExecutionId);
    }

    public TerminateExecutionContainer(List<String> executionIds) {
        this.executionIds = executionIds;
    }

    public List<String> getExecutionIds() {
        return Optional.ofNullable(executionIds).orElse(Collections.emptyList());
    }


    public List<ExecutionEntity> getExecutions() {
        return executions;
    }
}
