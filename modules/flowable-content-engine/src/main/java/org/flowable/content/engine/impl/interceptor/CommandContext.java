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
package org.flowable.content.engine.impl.interceptor;

import java.util.ArrayList;

import org.flowable.content.engine.ContentEngineConfiguration;
import org.flowable.content.engine.impl.db.DbSqlSession;
import org.flowable.content.engine.impl.persistence.entity.ContentItemEntityManager;
import org.flowable.content.engine.impl.persistence.entity.TableDataManager;
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

    protected ContentEngineConfiguration contentEngineConfiguration;

    public CommandContext(Command<?> command, ContentEngineConfiguration contentEngineConfiguration) {
        super(command);
        this.contentEngineConfiguration = contentEngineConfiguration;
        sessionFactories = contentEngineConfiguration.getSessionFactories();
    }

    public void addCloseListener(CommandContextCloseListener commandContextCloseListener) {
        if (closeListeners == null) {
            closeListeners = new ArrayList<BaseCommandContextCloseListener<AbstractCommandContext>>(1);
        }
        closeListeners.add((BaseCommandContextCloseListener) commandContextCloseListener);
    }

    public DbSqlSession getDbSqlSession() {
        return getSession(DbSqlSession.class);
    }

    public ContentItemEntityManager getContentItemEntityManager() {
        return contentEngineConfiguration.getContentItemEntityManager();
    }

    public TableDataManager getTableDataManager() {
        return contentEngineConfiguration.getTableDataManager();
    }

    // getters and setters
    // //////////////////////////////////////////////////////

    public ContentEngineConfiguration getContentEngineConfiguration() {
        return contentEngineConfiguration;
    }
}
