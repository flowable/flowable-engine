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

import org.flowable.cmmn.converter.CmmnXmlConverter;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.validation.CaseValidatorFactory;
import org.flowable.cmmn.validation.validator.Problems;
import org.flowable.cmmn.validation.validator.ValidationEntry;
import org.flowable.cmmn.validation.validator.ValidatorSetNames;
import org.junit.Test;

/**
 * @author Calin Cerchez
 */
public class HumanTaskValidatorTest {

    private CmmnModel readXMLFile(String resource) {
        return new CmmnXmlConverter().convertToCmmnModel(() -> HumanTaskValidatorTest.class.getResourceAsStream(resource), true, false);
    }

    @Test
    public void testValidateNoErrors() {
        CmmnModel cmmnModel = readXMLFile("humanTaskNoErrors.cmmn");
        List<ValidationEntry> validationEntries = new CaseValidatorFactory().createDefaultCaseValidator().validate(cmmnModel);
        assertThat(validationEntries).isEmpty();
    }

    @Test
    public void testValidateMissingListenerImplementationType() {
        CmmnModel cmmnModel = readXMLFile("humanTaskMissingListenerImplementationType.cmmn");
        List<ValidationEntry> validationEntries = new CaseValidatorFactory().createDefaultCaseValidator().validate(cmmnModel);
        assertThat(validationEntries)
                .extracting(ValidationEntry::getProblem, ValidationEntry::getDefaultDescription)
                .containsExactlyInAnyOrder(
                        tuple(Problems.HUMAN_TASK_LISTENER_IMPLEMENTATION_MISSING,
                                "Element 'class', 'expression' or 'delegateExpression' is mandatory on executionListener")
                );

        ValidationEntry entry = validationEntries.get(0);
        assertThat(entry.getLevel()).isEqualTo(ValidationEntry.Level.Error);
        assertThat(entry.getValidatorSetName()).isEqualTo(ValidatorSetNames.FLOWABLE_CASE);
        assertThat(entry.getCaseDefinitionId()).isEqualTo("humanTaskVariableNameCase");
        assertThat(entry.getCaseDefinitionName()).isEqualTo("Human Task Variable Case");
        assertThat(entry.getItemId()).isEqualTo("task1");
        assertThat(entry.getItemName()).isEqualTo("Task 1");

        assertThat(entry.getXmlLineNumber()).isEqualTo(23);
        assertThat(entry.getXmlColumnNumber()).isEqualTo(54);
    }
}
