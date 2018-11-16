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

package org.flowable.engine.impl.cmd;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.form.StartFormData;
import org.flowable.engine.impl.form.DefaultFormHandler;
import org.flowable.engine.impl.form.FormHandlerHelper;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.Flowable5Util;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.engine.repository.ProcessDefinition;

import java.io.InputStream;

/**
 * Command for  retrieving a deployed start form for a process definition with a given id.
 * 
 * @author shareniu
 */
public class GetDeployedStartFormCmd implements Command<InputStream> {

    protected String processDefinitionId;

    public GetDeployedStartFormCmd(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    @Override
    public InputStream execute(CommandContext commandContext) {
        if (processDefinitionId == null) {
            throw new FlowableIllegalArgumentException("processDefinitionId is null");
        }
        StartFormData startFormData = new GetStartFormCmd(processDefinitionId).execute(commandContext);
        String formKey = startFormData.getFormKey();
        if (formKey == null) {
            throw new FlowableIllegalArgumentException("The form key is not set");
        }
        return new GetDeploymentResourceCmd(startFormData.getDeploymentId(), formKey).execute(commandContext);
    }


}
