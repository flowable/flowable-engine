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

package org.flowable.form.engine.impl.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.db.DbSchemaManager;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandConfig;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.form.api.FormManagementService;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.api.FormService;
import org.flowable.form.engine.FormEngine;
import org.flowable.form.engine.FormEngineConfiguration;
import org.flowable.form.engine.impl.util.CommandContextUtil;
import org.flowable.form.engine.test.FormTestHelper;
import org.junit.Assert;

import junit.framework.AssertionFailedError;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public abstract class AbstractFlowableTestCase extends AbstractTestCase {

    private static final List<String> TABLENAMES_EXCLUDED_FROM_DB_CLEAN_CHECK = new ArrayList<>();

    static {
        TABLENAMES_EXCLUDED_FROM_DB_CLEAN_CHECK.add("ACT_FO_DATABASECHANGELOG");
        TABLENAMES_EXCLUDED_FROM_DB_CLEAN_CHECK.add("ACT_FO_DATABASECHANGELOGLOCK");
    }

    protected FormEngine formEngine;

    protected String deploymentIdFromDeploymentAnnotation;
    protected List<String> deploymentIdsForAutoCleanup = new ArrayList<>();
    protected Throwable exception;

    protected FormEngineConfiguration formEngineConfiguration;
    protected FormManagementService managementService;
    protected FormRepositoryService repositoryService;
    protected FormService formService;

    protected abstract void initializeFormEngine();

    // Default: do nothing
    protected void closeDownFormEngine() {
    }

    protected void nullifyServices() {
        formEngineConfiguration = null;
        managementService = null;
        repositoryService = null;
        formService = null;
    }

    @Override
    public void runBare() throws Throwable {
        initializeFormEngine();
        if (repositoryService == null) {
            initializeServices();
        }

        try {

            deploymentIdFromDeploymentAnnotation = FormTestHelper.annotationDeploymentSetUp(formEngine, getClass(), getName());

            super.runBare();

        } catch (AssertionFailedError e) {
            LOGGER.error(EMPTY_LINE);
            LOGGER.error("ASSERTION FAILED: {}", e, e);
            exception = e;
            throw e;

        } catch (Throwable e) {
            LOGGER.error(EMPTY_LINE);
            LOGGER.error("EXCEPTION: {}", e, e);
            exception = e;
            throw e;

        } finally {

            if (deploymentIdFromDeploymentAnnotation != null) {
                FormTestHelper.annotationDeploymentTearDown(formEngine, deploymentIdFromDeploymentAnnotation, getClass(), getName());
                deploymentIdFromDeploymentAnnotation = null;
            }

            for (String autoDeletedDeploymentId : deploymentIdsForAutoCleanup) {
                repositoryService.deleteDeployment(autoDeletedDeploymentId);
            }
            deploymentIdsForAutoCleanup.clear();

            assertAndEnsureCleanDb();
            formEngineConfiguration.getClock().reset();

            // Can't do this in the teardown, as the teardown will be called as part of the super.runBare
            closeDownFormEngine();
        }
    }

    /**
     * Each test is assumed to clean up all DB content it entered. After a test method executed, this method scans all tables to see if the DB is completely clean. It throws AssertionFailed in case
     * the DB is not clean. If the DB is not clean, it is cleaned by performing a create a drop.
     */
    protected void assertAndEnsureCleanDb() throws Throwable {
        LOGGER.debug("verifying that db is clean after test");
        Map<String, Long> tableCounts = managementService.getTableCount();
        StringBuilder outputMessage = new StringBuilder();
        for (String tableName : tableCounts.keySet()) {
            String tableNameWithoutPrefix = tableName.replace(formEngineConfiguration.getDatabaseTablePrefix(), "");
            if (!TABLENAMES_EXCLUDED_FROM_DB_CLEAN_CHECK.contains(tableNameWithoutPrefix)) {
                Long count = tableCounts.get(tableName);
                if (count != 0L) {
                    outputMessage.append("  ").append(tableName).append(": ").append(count).append(" record(s) ");
                }
            }
        }

        if (outputMessage.length() > 0) {
            outputMessage.insert(0, "DB NOT CLEAN: \n");
            LOGGER.error(EMPTY_LINE);
            LOGGER.error(outputMessage.toString());

            LOGGER.info("dropping and recreating db");

            CommandExecutor commandExecutor = formEngine.getFormEngineConfiguration().getCommandExecutor();
            CommandConfig config = new CommandConfig().transactionNotSupported();
            commandExecutor.execute(config, new Command<Object>() {
                @Override
                public Object execute(CommandContext commandContext) {
                    DbSchemaManager dbSchemaManager = CommandContextUtil.getFormEngineConfiguration(commandContext).getDbSchemaManager();
                    dbSchemaManager.dbSchemaDrop();
                    dbSchemaManager.dbSchemaCreate();
                    return null;
                }
            });

            if (exception != null) {
                throw exception;
            } else {
                Assert.fail(outputMessage.toString());
            }
        } else {
            LOGGER.info("database was clean");
        }
    }

    protected void initializeServices() {
        formEngineConfiguration = formEngine.getFormEngineConfiguration();
        managementService = formEngine.getFormManagementService();
        repositoryService = formEngine.getFormRepositoryService();
        formService = formEngine.getFormService();
    }

}
