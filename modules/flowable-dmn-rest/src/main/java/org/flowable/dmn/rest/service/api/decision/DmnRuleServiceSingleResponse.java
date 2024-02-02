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
package org.flowable.dmn.rest.service.api.decision;

import java.util.ArrayList;
import java.util.List;

import org.flowable.common.rest.variable.EngineRestVariable;

/**
 * @author Yvo Swillens
 */
public class DmnRuleServiceSingleResponse {

    protected List<EngineRestVariable> resultVariables = new ArrayList<>();
    protected String url;

    public List<EngineRestVariable> getResultVariables() {
        return resultVariables;
    }

    public void setResultVariables(List<EngineRestVariable> resultVariables) {
        this.resultVariables = resultVariables;
    }

    public void addResultVariable(EngineRestVariable variable) {
        resultVariables.add(variable);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
