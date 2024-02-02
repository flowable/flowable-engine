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
package org.flowable.cmmn.engine.impl.parser;

import org.flowable.cmmn.validation.CaseValidator;
import org.flowable.common.engine.api.repository.EngineResource;

/**
 * @author Filip Hrisafov
 */
public interface CmmnParseContext {

    /**
     * The {@link EngineResource} that contains the information that should be parsed
     */
    EngineResource resource();

    /**
     * Whether extra checks needs to be done when parsing the CMMN XML
     */
    boolean enableSafeXml();

    /**
     * The encoding that should be used for the XML parsing.
     */
    String xmlEncoding();

    /**
     * Whether to perform XML Schema validation
     */
    boolean validateXml();

    /**
     * Whether to perform Flowable Case Model validation
     */
    boolean validateCmmnModel();

    /**
     * The validation that should be used for the Case Model validation
     */
    CaseValidator caseValidator();
}
