/*
 * Licensed under the Apache License, Version 2.0 (the "License");
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
 *
 */

package org.flowable.cmmn.validation.validator.impl;

import java.util.List;

import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.FlowableListener;
import org.flowable.cmmn.model.HumanTask;
import org.flowable.cmmn.validation.CaseValidationContext;
import org.flowable.cmmn.validation.validator.Problems;

/**
 * @author Calin Cerchez
 */
public class HumanTaskValidator extends CaseLevelValidator {

    @Override
    protected void executeValidation(CmmnModel model, Case caze, CaseValidationContext validationContext) {
        List<HumanTask> humanTasks = caze.findPlanItemDefinitionsOfType(HumanTask.class);
        for (HumanTask humanTask : humanTasks) {
            if (humanTask.getTaskListeners() != null) {
                for (FlowableListener listener : humanTask.getTaskListeners()) {
                    if (listener.getImplementationType() == null) {
                        validationContext.addError(Problems.HUMAN_TASK_LISTENER_IMPLEMENTATION_MISSING, caze, humanTask, listener,
                                "Element 'class', 'expression' or 'delegateExpression' is mandatory on executionListener");
                    }
                }
            }
        }
    }
}
