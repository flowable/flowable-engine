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
package org.flowable.cmmn.validation.validator.impl;

import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.Stage;
import org.flowable.cmmn.validation.CaseValidationContext;
import org.flowable.cmmn.validation.validator.Problems;

/**
 * @author Filip Hrisafov
 */
public class PlanModelValidator extends CaseLevelValidator {

    @Override
    protected void executeValidation(CmmnModel model, Case caze, CaseValidationContext validationContext) {
        Stage planModel = caze.getPlanModel();
        if (planModel != null) {
            if (planModel.getPlanItems().isEmpty()) {
                validationContext.addWarning(Problems.PLAN_MODEL_EMPTY, caze, planModel, planModel, "Case plan model is empty");
            }
        }
    }
}
