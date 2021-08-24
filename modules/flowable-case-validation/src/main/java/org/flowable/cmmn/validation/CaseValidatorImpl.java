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

package org.flowable.cmmn.validation;

import java.util.ArrayList;
import java.util.List;

import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.validation.validator.ValidationEntry;
import org.flowable.cmmn.validation.validator.Validator;
import org.flowable.cmmn.validation.validator.ValidatorSet;

/**
 * @author Calin Cerchez
 */
public class CaseValidatorImpl implements CaseValidator {

    protected List<ValidatorSet> validatorSets = new ArrayList<>();

    @Override
    public List<ValidationEntry> validate(CmmnModel model) {
        List<ValidationEntry> allEntries = new ArrayList<>();

        for (ValidatorSet validatorSet : validatorSets) {
            CaseValidationContextImpl validationContext = new CaseValidationContextImpl(validatorSet);

            for (Validator validator : validatorSet.getValidators()) {
                validator.validate(model, validationContext);
            }

            allEntries.addAll(validationContext.getEntries());
        }

        return allEntries;
    }

    @Override
    public List<ValidatorSet> getValidatorSets() {
        return validatorSets;
    }

    public void addValidatorSet(ValidatorSet validatorSet) {
        this.validatorSets.add(validatorSet);
    }
}
