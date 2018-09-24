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

package org.flowable.form.engine.impl.cmd;

import java.io.Serializable;

import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.form.engine.impl.persistence.entity.FormDefinitionEntity;
import org.flowable.form.engine.impl.util.CommandContextUtil;

/**
 * @author Tijs Rademakers
 */
public class GetFormDefinitionCmd implements Command<FormDefinitionEntity>, Serializable {

    private static final long serialVersionUID = 1L;
    protected String formDefinitionId;

    public GetFormDefinitionCmd(String formDefinitionId) {
        this.formDefinitionId = formDefinitionId;
    }

    @Override
    public FormDefinitionEntity execute(CommandContext commandContext) {
        return CommandContextUtil.getFormEngineConfiguration().getDeploymentManager()
                .findDeployedFormDefinitionById(formDefinitionId);
    }
}
