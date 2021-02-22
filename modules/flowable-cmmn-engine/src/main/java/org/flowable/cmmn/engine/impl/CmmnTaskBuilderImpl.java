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
package org.flowable.cmmn.engine.impl;

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.cmd.CreateCmmnTaskCmd;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskBuilder;
import org.flowable.task.service.impl.BaseTaskBuilderImpl;

/**
 * {@link TaskBuilder} implementation
 */
public class CmmnTaskBuilderImpl extends BaseTaskBuilderImpl {
    
    protected CmmnEngineConfiguration cmmnEngineConfiguration;

    CmmnTaskBuilderImpl(CommandExecutor commandExecutor, CmmnEngineConfiguration cmmnEngineConfiguration) {
        super(commandExecutor);
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
    }

    @Override
    public Task create() {
        return commandExecutor.execute(new CreateCmmnTaskCmd(this));
    }

}
