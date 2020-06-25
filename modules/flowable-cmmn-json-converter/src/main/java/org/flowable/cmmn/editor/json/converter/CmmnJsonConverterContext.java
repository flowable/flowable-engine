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
package org.flowable.cmmn.editor.json.converter;

import java.util.Map;

import org.flowable.cmmn.model.CmmnModel;

/**
 * @author Joram Barrez
 */
public interface CmmnJsonConverterContext {

    // Note the Map<String, String> instead of a class here.
    // The reason is because the bpmn and cmmn json conversion modules don't share a common package.

    String getFormModelKeyForFormModelId(String formModelId);
    Map<String, String> getFormModelInfoForFormModelKey(String formModelKey);

    String getCaseModelKeyForCaseModelId(String caseModelId);
    Map<String, String> getCaseModelInfoForCaseModelKey(String caseModelKey);

    String getProcessModelKeyForProcessModelId(String processModelId);
    Map<String, String> getProcessModelInfoForProcessModelKey(String processModelKey);

    String getDecisionTableModelKeyForDecisionTableModelId(String decisionTableModelId);
    Map<String, String> getDecisionTableModelInfoForDecisionTableModelKey(String decisionTableModelKey);

    String getDecisionServiceModelKeyForDecisionServiceModelId(String decisionServiceModelId);
    Map<String, String> getDecisionServiceModelInfoForDecisionServiceModelKey(String decisionServiceModelKey);

    void registerUnresolvedCaseModelReferenceForCaseModel(String unresolvedCaseModelKey, CmmnModel cmmnModel);

    void registerUnresolvedProcessModelReferenceForCaseModel(String unresolvedProcessModelKey, CmmnModel cmmnModel);

}
