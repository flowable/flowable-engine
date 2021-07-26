/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.flowable.cmmn.validation.validator;

import org.flowable.cmmn.validation.validator.impl.DecisionTaskValidator;
import org.flowable.cmmn.validation.validator.impl.HumanTaskValidator;
import org.flowable.cmmn.validation.validator.impl.PlanModelValidator;

/**
 * @author Calin Cerchez
 */
public class ValidatorSetFactory {

    public ValidatorSet createFlowableExecutableCaseValidatorSet() {
        ValidatorSet validatorSet = new ValidatorSet(ValidatorSetNames.FLOWABLE_CASE);
        validatorSet.addValidator(new DecisionTaskValidator());
        validatorSet.addValidator(new HumanTaskValidator());
        validatorSet.addValidator(new PlanModelValidator());
        return validatorSet;
    }
}
