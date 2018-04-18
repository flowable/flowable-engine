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
package org.flowable.http.bpmn.impl;

import org.flowable.bpmn.model.MapExceptionEntry;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.bpmn.helper.ErrorPropagation;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.http.ErrorPropagator;

import java.util.List;

/**
 * This class propagates process errors
 */
public class ProcessErrorPropagator implements ErrorPropagator {
    @Override
    public void propagateError(VariableContainer execution, String code) {
            ErrorPropagation.propagateError("HTTP" + code, (DelegateExecution) execution);
    }

    @Override
    public boolean mapException(Exception e, VariableContainer execution, List<MapExceptionEntry> exceptionMap) {
        if (execution instanceof ExecutionEntity) {
            return ErrorPropagation.mapException(e, (ExecutionEntity) execution, exceptionMap);
        }
        throw new FlowableIllegalArgumentException("VariableContainer "+ execution +" is not execution entity");
    }
}
