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
package org.flowable.engine.impl.variable;

/**
 * @author Filip Hrisafov
 */
public class ParallelMultiInstanceLoopVariable {

    public static final String COMPLETED_INSTANCES = "completed";
    public static final String ACTIVE_INSTANCES = "active";

    protected final String executionId;
    protected final String type;

    public ParallelMultiInstanceLoopVariable(String executionId, String type) {
        this.executionId = executionId;
        this.type = type;
    }

    public String getExecutionId() {
        return executionId;
    }

    public String getType() {
        return type;
    }

    public static ParallelMultiInstanceLoopVariable completed(String executionId) {
        return new ParallelMultiInstanceLoopVariable(executionId, COMPLETED_INSTANCES);
    }

    public static ParallelMultiInstanceLoopVariable active(String executionId) {
        return new ParallelMultiInstanceLoopVariable(executionId, ACTIVE_INSTANCES);
    }
}
