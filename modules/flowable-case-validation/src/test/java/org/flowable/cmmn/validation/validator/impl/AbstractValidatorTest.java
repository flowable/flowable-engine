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

import java.util.List;

import org.flowable.cmmn.converter.CmmnXmlConverter;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.validation.CaseValidatorFactory;
import org.flowable.cmmn.validation.CaseValidator;
import org.flowable.cmmn.validation.validator.ValidationEntry;
import org.junit.jupiter.api.BeforeEach;

/**
 * @author Filip Hrisafov
 */
public abstract class AbstractValidatorTest {

    protected CaseValidator caseValidator;

    @BeforeEach
    void createCaseValidator() {
        caseValidator = new CaseValidatorFactory().createDefaultCaseValidator();
    }

    protected CmmnModel readXMLFile(String resource) {
        return new CmmnXmlConverter().convertToCmmnModel(() -> getClass().getClassLoader().getResourceAsStream(resource), true, false);
    }

    protected List<ValidationEntry> validate(String resource) {
        CmmnModel model = readXMLFile(resource);
        return caseValidator.validate(model);
    }

}
