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
package org.flowable.cmmn.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.validation.validator.ValidationEntry;
import org.flowable.cmmn.validation.validator.ValidatorSet;
import org.junit.jupiter.api.Test;

/**
 * @author Tijs Rademakers
 */
class CaseValidatorImplTest {

    @Test
    void testValidateWithSharedContextDoesNotDuplicateEntries() {
        CaseValidatorImpl validator = new CaseValidatorImpl();

        ValidatorSet set1 = new ValidatorSet("set1");
        set1.addValidator((model, validationContext) -> validationContext.addError("PROBLEM_A", "Error from set 1"));

        ValidatorSet set2 = new ValidatorSet("set2");
        set2.addValidator((model, validationContext) -> validationContext.addError("PROBLEM_B", "Error from set 2"));

        validator.addValidatorSet(set1);
        validator.addValidatorSet(set2);

        CmmnModel cmmnModel = new CmmnModel();

        // Validate with a shared external context
        CaseValidationContextImpl sharedContext = new CaseValidationContextImpl(set1);
        List<ValidationEntry> entries = validator.validate(cmmnModel, sharedContext);

        // Must be exactly 2 entries (1 per set), not 3 which would happen
        // if the cumulative getEntries() list was re-added each iteration
        assertThat(entries)
                .extracting(ValidationEntry::getProblem, ValidationEntry::getValidatorSetName)
                .containsExactly(
                        tuple("PROBLEM_A", "set1"),
                        tuple("PROBLEM_B", "set2")
                );

        // Verify no-context path produces the same result count
        List<ValidationEntry> entriesWithoutContext = validator.validate(cmmnModel, null);
        assertThat(entriesWithoutContext)
                .extracting(ValidationEntry::getProblem, ValidationEntry::getValidatorSetName)
                .containsExactly(
                        tuple("PROBLEM_A", "set1"),
                        tuple("PROBLEM_B", "set2")
                );
    }
}
