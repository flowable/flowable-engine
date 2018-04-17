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
package org.activiti.mule;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.db.DbSchemaManager;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandConfig;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.impl.ProcessEngineImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.junit.Assert;
import org.mule.tck.junit4.FunctionalTestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractMuleTest extends FunctionalTestCase {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractMuleTest.class);

    protected static final String EMPTY_LINE = "                                                                                           ";
    private static final List<String> TABLENAMES_EXCLUDED_FROM_DB_CLEAN_CHECK = Arrays.asList("ACT_GE_PROPERTY", "ACT_ID_PROPERTY");

    /**
     * Each test is assumed to clean up all DB content it entered. After a test method executed, this method scans all tables to see if the DB is completely clean. It throws AssertionFailed in case
     * the DB is not clean. If the DB is not clean, it is cleaned by performing a create a drop.
     */
    protected void assertAndEnsureCleanDb(ProcessEngine processEngine) throws Exception {
        LOGGER.debug("verifying that db is clean after test");
        Map<String, Long> tableCounts = processEngine.getManagementService().getTableCount();
        StringBuilder outputMessage = new StringBuilder();
        for (String tableName : tableCounts.keySet()) {
            String tableNameWithoutPrefix = tableName.replace(processEngine.getProcessEngineConfiguration().getDatabaseTablePrefix(), "");
            if (!TABLENAMES_EXCLUDED_FROM_DB_CLEAN_CHECK.contains(tableNameWithoutPrefix)) {
                Long count = tableCounts.get(tableName);
                if (count != 0L) {
                    outputMessage.append("  " + tableName + ": " + count + " record(s) ");
                }
            }
        }
        if (outputMessage.length() > 0) {
            outputMessage.insert(0, "DB NOT CLEAN: \n");
            LOGGER.error(EMPTY_LINE);
            LOGGER.error(outputMessage.toString());

            LOGGER.info("dropping and recreating db");

            CommandExecutor commandExecutor = ((ProcessEngineImpl) processEngine).getProcessEngineConfiguration().getCommandExecutor();
            CommandConfig config = new CommandConfig().transactionNotSupported();
            commandExecutor.execute(config, new Command<Object>() {
                public Object execute(CommandContext commandContext) {
                    DbSchemaManager dbSchemaManager = CommandContextUtil.getProcessEngineConfiguration(commandContext).getDbSchemaManager();
                    dbSchemaManager.dbSchemaDrop();
                    dbSchemaManager.dbSchemaCreate();
                    return null;
                }
            });

            Assert.fail(outputMessage.toString());

        } else {
            LOGGER.info("database was clean");
        }
    }

}
