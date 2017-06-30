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
package org.flowable.dmn.engine.impl.interceptor;

import java.util.ArrayList;

import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.dmn.engine.impl.db.DbSqlSession;
import org.flowable.dmn.engine.impl.persistence.entity.DecisionTableEntityManager;
import org.flowable.dmn.engine.impl.persistence.entity.DmnDeploymentEntityManager;
import org.flowable.dmn.engine.impl.persistence.entity.ResourceEntityManager;
import org.flowable.dmn.engine.impl.persistence.entity.TableDataManager;
import org.flowable.engine.common.impl.interceptor.AbstractCommandContext;
import org.flowable.engine.common.impl.interceptor.BaseCommandContextCloseListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class CommandContext extends AbstractCommandContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandContext.class);

    protected DmnEngineConfiguration dmnEngineConfiguration;

    public CommandContext(Command<?> command, DmnEngineConfiguration dmnEngineConfiguration) {
        super(command);
        this.dmnEngineConfiguration = dmnEngineConfiguration;
        sessionFactories = dmnEngineConfiguration.getSessionFactories();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addCloseListener(CommandContextCloseListener commandContextCloseListener) {
        if (closeListeners == null) {
            closeListeners = new ArrayList<BaseCommandContextCloseListener<AbstractCommandContext>>(1);
        }
        closeListeners.add((BaseCommandContextCloseListener) commandContextCloseListener);
    }

    public DbSqlSession getDbSqlSession() {
        return getSession(DbSqlSession.class);
    }

    public DmnDeploymentEntityManager getDeploymentEntityManager() {
        return dmnEngineConfiguration.getDeploymentEntityManager();
    }

    public DecisionTableEntityManager getDecisionTableEntityManager() {
        return dmnEngineConfiguration.getDecisionTableEntityManager();
    }

    public ResourceEntityManager getResourceEntityManager() {
        return dmnEngineConfiguration.getResourceEntityManager();
    }

    public TableDataManager getTableDataManager() {
        return dmnEngineConfiguration.getTableDataManager();
    }

    // getters and setters
    // //////////////////////////////////////////////////////

    public DmnEngineConfiguration getDmnEngineConfiguration() {
        return dmnEngineConfiguration;
    }
}
