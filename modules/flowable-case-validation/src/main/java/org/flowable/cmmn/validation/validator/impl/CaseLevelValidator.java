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

import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.validation.CaseValidationContext;
import org.flowable.cmmn.validation.validator.Validator;

/**
 * @author Calin Cerchez
 */
public abstract class CaseLevelValidator implements Validator {

    @Override
    public void validate(CmmnModel model, CaseValidationContext validationContext) {
        for (Case caze : model.getCases()) {
            executeValidation(model, caze, validationContext);
        }
    }

    protected abstract void executeValidation(CmmnModel model, Case caze, CaseValidationContext validationContext);
}
