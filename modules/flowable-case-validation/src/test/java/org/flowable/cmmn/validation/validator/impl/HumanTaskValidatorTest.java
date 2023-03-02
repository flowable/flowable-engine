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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.flowable.cmmn.validation.validator.Problems;
import org.flowable.cmmn.validation.validator.ValidationEntry;
import org.flowable.cmmn.validation.validator.ValidatorSetNames;
import org.junit.jupiter.api.Test;

/**
 * @author Calin Cerchez
 */
public class HumanTaskValidatorTest extends AbstractValidatorTest {

    @Test
    public void testValidateNoErrors() {
        List<ValidationEntry> validationEntries = validate("org/flowable/cmmn/validation/validator/impl/humanTaskNoErrors.cmmn");
        assertThat(validationEntries).isEmpty();
    }

    @Test
    public void testValidateMissingListenerImplementationType() {
        List<ValidationEntry> validationEntries = validate("org/flowable/cmmn/validation/validator/impl/humanTaskMissingListenerImplementationType.cmmn");
        assertThat(validationEntries)
                .extracting(ValidationEntry::getProblem, ValidationEntry::getDefaultDescription)
                .containsExactlyInAnyOrder(
                        tuple(Problems.HUMAN_TASK_LISTENER_MISSING_EVENT, "Element 'event' is mandatory on taskListener"),
                        tuple(Problems.HUMAN_TASK_LISTENER_IMPLEMENTATION_MISSING, "taskListener of type 'script' expects a <script> child element"),
                        tuple(Problems.HUMAN_TASK_LISTENER_IMPLEMENTATION_MISSING,
                                "Element 'class', 'expression' or 'delegateExpression' or type=\"script\" is mandatory on taskListener")
                );

        ValidationEntry entry1 = validationEntries.get(0);
        assertThat(entry1.getLevel()).isEqualTo(ValidationEntry.Level.Error);
        assertThat(entry1.getValidatorSetName()).isEqualTo(ValidatorSetNames.FLOWABLE_CASE);
        assertThat(entry1.getCaseDefinitionId()).isEqualTo("humanTaskVariableNameCase");
        assertThat(entry1.getCaseDefinitionName()).isEqualTo("Human Task Variable Case");
        assertThat(entry1.getItemId()).isEqualTo("task1");
        assertThat(entry1.getItemName()).isEqualTo("Task 1");

        assertThat(entry1.getXmlLineNumber()).isEqualTo(23);
        assertThat(entry1.getXmlColumnNumber()).isEqualTo(54);

        ValidationEntry entry2 = validationEntries.get(1);
        assertThat(entry2.getLevel()).isEqualTo(ValidationEntry.Level.Error);
        assertThat(entry2.getValidatorSetName()).isEqualTo(ValidatorSetNames.FLOWABLE_CASE);
        assertThat(entry2.getCaseDefinitionId()).isEqualTo("humanTaskVariableNameCase");
        assertThat(entry2.getCaseDefinitionName()).isEqualTo("Human Task Variable Case");
        assertThat(entry2.getItemId()).isEqualTo("task1");
        assertThat(entry2.getItemName()).isEqualTo("Task 1");

        assertThat(entry2.getXmlLineNumber()).isEqualTo(23);
        assertThat(entry2.getXmlColumnNumber()).isEqualTo(54);
    }
}
