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
package org.flowable.cmmn.test.delegate;

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

public class ChangeCaseVariableValueDelegate implements JavaDelegate {
    
    protected Expression newVariableValue;

    @Override
    public void execute(DelegateExecution execution) {
        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration();
        CaseInstanceEntity caseInstance = (CaseInstanceEntity) cmmnEngineConfiguration.getCmmnRuntimeService().createCaseInstanceQuery().caseInstanceId(execution.getVariable("caseInstanceId").toString()).singleResult();
        caseInstance.setVariable("myVar", newVariableValue.getValue(execution));
    }

}
