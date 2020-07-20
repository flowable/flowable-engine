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
package org.flowable.cmmn.api.delegate;

import java.util.HashMap;
import java.util.Map;

/**
 * A {@link FlowablePlanItemFutureJavaDelegate} which has {@link Map} as input and output data.
 * By default this will have a copy {@link DelegatePlanItemInstance#getVariables()} as the input data
 * and will store all data from the output map as variables in the {@link DelegatePlanItemInstance}
 *
 * @author Filip Hrisafov
 */
public interface MapBasedFlowablePlanItemFutureJavaDelegate extends FlowablePlanItemFutureJavaDelegate<Map<String, Object>, Map<String, Object>> {

    @Override
    default Map<String, Object> prepareExecutionData(DelegatePlanItemInstance planItemInstance) {
        return new HashMap<>(planItemInstance.getVariables());
    }

    @Override
    default void afterExecution(DelegatePlanItemInstance planItemInstance, Map<String, Object> executionData) {
        if (executionData != null) {
            for (Map.Entry<String, Object> entry : executionData.entrySet()) {
                planItemInstance.setVariable(entry.getKey(), entry.getValue());
            }
        }
    }
}
