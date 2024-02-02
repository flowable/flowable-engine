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

package org.flowable.dmn.engine.impl.persistence;

import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.dmn.engine.impl.persistence.entity.DecisionEntityManager;
import org.flowable.dmn.engine.impl.persistence.entity.DmnDeploymentEntityManager;
import org.flowable.dmn.engine.impl.persistence.entity.DmnResourceEntityManager;
import org.flowable.dmn.engine.impl.persistence.entity.HistoricDecisionExecutionEntityManager;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public abstract class AbstractManager {

    protected DmnEngineConfiguration dmnEngineConfiguration;

    public AbstractManager(DmnEngineConfiguration dmnEngineConfiguration) {
        this.dmnEngineConfiguration = dmnEngineConfiguration;
    }

    // Command scoped

    protected CommandContext getCommandContext() {
        return Context.getCommandContext();
    }

    protected <T> T getSession(Class<T> sessionClass) {
        return getCommandContext().getSession(sessionClass);
    }

    // Engine scoped

    protected DmnEngineConfiguration getDmnEngineConfiguration() {
        return dmnEngineConfiguration;
    }

    protected DmnDeploymentEntityManager getDeploymentEntityManager() {
        return getDmnEngineConfiguration().getDeploymentEntityManager();
    }

    protected DecisionEntityManager getDecisionTableEntityManager() {
        return getDmnEngineConfiguration().getDecisionEntityManager();
    }
    
    protected HistoricDecisionExecutionEntityManager getHistoricDecisionExecutionEntityManager() {
        return getDmnEngineConfiguration().getHistoricDecisionExecutionEntityManager();
    }

    protected DmnResourceEntityManager getResourceEntityManager() {
        return getDmnEngineConfiguration().getResourceEntityManager();
    }

}
