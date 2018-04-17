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
package org.flowable.app.engine.impl.cmd;

import java.io.Serializable;

import org.flowable.app.api.repository.AppDefinition;
import org.flowable.app.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * An abstract superclass for {@link Command} implementations that want to verify the provided app is always active (ie. not suspended).
 * 
 * @author Tijs Rademakers
 */
public abstract class NeedsAppDefinitionCmd<T> implements Command<T>, Serializable {

    private static final long serialVersionUID = 1L;

    protected String appDefinitionId;

    public NeedsAppDefinitionCmd(String appDefinitionId) {
        this.appDefinitionId = appDefinitionId;
    }

    @Override
    public T execute(CommandContext commandContext) {

        if (appDefinitionId == null) {
            throw new FlowableIllegalArgumentException("appDefinitionId is null");
        }

        AppDefinition appDefinition = CommandContextUtil.getAppRepositoryService().getAppDefinition(appDefinitionId);

        if (appDefinition == null) {
            throw new FlowableObjectNotFoundException("Cannot find app definition with id " + appDefinitionId, AppDefinition.class);
        }

        return execute(commandContext, appDefinition);
    }

    /**
     * Subclasses must implement in this method their normal command logic.
     */
    protected abstract T execute(CommandContext commandContext, AppDefinition appDefinition);

}
