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
import java.util.stream.Collectors;

import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.validation.validator.ValidationEntry;
import org.flowable.cmmn.validation.validator.ValidatorSet;

/**
 * @author Calin Cerchez
 */
public class CaseValidatorImpl implements CaseValidator {

    private List<ValidatorSet> validatorSets = new ArrayList<>();

    @Override
    public List<ValidationEntry> validate(CmmnModel model) {
        return validatorSets.stream()
                .flatMap(validatorSet -> validatorSet.getValidators().stream())
                .flatMap(validator -> validator.validate(model).stream())
                .collect(Collectors.toList());
    }

    public void addValidatorSet(ValidatorSet validatorSet) {
        this.validatorSets.add(validatorSet);
    }
}
