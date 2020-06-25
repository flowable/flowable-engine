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

public class StandaloneCmmnJsonConverterContext implements CmmnJsonConverterContext {

    @Override
    public String getFormModelKeyForFormModelId(String formModelId) {
        return null;
    }
    @Override
    public Map<String, String> getFormModelInfoForFormModelKey(String formModelKey) {
        return null;
    }
    @Override
    public String getCaseModelKeyForCaseModelId(String caseModelId) {
        return null;
    }
    @Override
    public Map<String, String> getCaseModelInfoForCaseModelKey(String caseModelKey) {
        return null;
    }
    @Override
    public String getProcessModelKeyForProcessModelId(String processModelId) {
        return null;
    }
    @Override
    public Map<String, String> getProcessModelInfoForProcessModelKey(String processModelKey) {
        return null;
    }
    @Override
    public String getDecisionTableModelKeyForDecisionTableModelId(String decisionTableModelId) {
        return null;
    }
    @Override
    public Map<String, String> getDecisionTableModelInfoForDecisionTableModelKey(String decisionTableModelKey) {
        return null;
    }
    @Override
    public String getDecisionServiceModelKeyForDecisionServiceModelId(String decisionServiceModelId) {
        return null;
    }
    @Override
    public Map<String, String> getDecisionServiceModelInfoForDecisionServiceModelKey(String decisionServiceModelKey) {
        return null;
    }
    @Override
    public void registerUnresolvedCaseModelReferenceForCaseModel(String unresolvedCaseModelKey, CmmnModel cmmnModel) {

    }
    @Override
    public void registerUnresolvedProcessModelReferenceForCaseModel(String unresolvedProcessModelKey, CmmnModel cmmnModel) {

    }
}
