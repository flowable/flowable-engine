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

package org.flowable.engine.impl.bpmn.behavior;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.ValuedDataObject;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.ProcessInstanceHelper;

/**
 * Implementation of the BPMN 2.0 subprocess (formally known as 'embedded' subprocess): a subprocess defined within another process definition.
 * 
 * @author Joram Barrez
 */
public class SubProcessActivityBehavior extends AbstractBpmnActivityBehavior {

    private static final long serialVersionUID = 1L;
  
    protected boolean isOnlyNoneStartEventAllowed;
  
    public SubProcessActivityBehavior() {
        this.isOnlyNoneStartEventAllowed = true;
    }

    @Override
    public void execute(DelegateExecution execution) {
        SubProcess subProcess = getSubProcessFromExecution(execution);

        FlowElement startElement = getStartElement(subProcess);

        if (startElement == null) {
            throw new FlowableException("No initial activity found for subprocess " + subProcess.getId());
        }

        ExecutionEntity executionEntity = (ExecutionEntity) execution;
        executionEntity.setScope(true);

        // initialize the template-defined data objects as variables
        Map<String, Object> dataObjectVars = processDataObjects(subProcess.getDataObjects());
        if (dataObjectVars != null) {
            executionEntity.setVariablesLocal(dataObjectVars);
        }
        
        CommandContext commandContext = Context.getCommandContext();
        ProcessInstanceHelper processInstanceHelper = CommandContextUtil.getProcessEngineConfiguration(commandContext).getProcessInstanceHelper();
        processInstanceHelper.processAvailableEventSubProcesses(executionEntity, subProcess, commandContext);

        ExecutionEntity startSubProcessExecution = CommandContextUtil.getExecutionEntityManager(commandContext)
                .createChildExecution(executionEntity);
        startSubProcessExecution.setCurrentFlowElement(startElement);
        CommandContextUtil.getAgenda().planContinueProcessOperation(startSubProcessExecution);
    }
  
    protected FlowElement getStartElement(SubProcess subProcess) {
        if (CollectionUtil.isNotEmpty(subProcess.getFlowElements())) {
            for (FlowElement subElement : subProcess.getFlowElements()) {
                if (subElement instanceof StartEvent) {
                    StartEvent startEvent = (StartEvent) subElement;
                    if (isOnlyNoneStartEventAllowed) {
                        if (CollectionUtil.isEmpty(startEvent.getEventDefinitions())) {
                            return startEvent;
                        }
                        
                    } else {
                        return startEvent;
                    }
                }
            }
        }
        return null;
    }

    protected SubProcess getSubProcessFromExecution(DelegateExecution execution) {
        FlowElement flowElement = execution.getCurrentFlowElement();
        SubProcess subProcess = null;
        if (flowElement instanceof SubProcess) {
            subProcess = (SubProcess) flowElement;
        } else {
            throw new FlowableException("Programmatic error: sub process behaviour can only be applied" + " to a SubProcess instance, but got an instance of " + flowElement);
        }
        return subProcess;
    }

    protected Map<String, Object> processDataObjects(Collection<ValuedDataObject> dataObjects) {
        Map<String, Object> variablesMap = new HashMap<>();
        // convert data objects to process variables
        if (dataObjects != null) {
            for (ValuedDataObject dataObject : dataObjects) {
                variablesMap.put(dataObject.getName(), dataObject.getValue());
            }
        }
        return variablesMap;
    }
}
