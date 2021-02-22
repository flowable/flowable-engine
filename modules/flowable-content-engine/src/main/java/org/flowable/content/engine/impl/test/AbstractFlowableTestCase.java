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

package org.flowable.content.engine.impl.test;

import java.util.ArrayList;
import java.util.List;

import org.flowable.common.engine.impl.db.SchemaManager;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.test.EnsureCleanDbUtils;
import org.flowable.content.api.ContentManagementService;
import org.flowable.content.api.ContentService;
import org.flowable.content.engine.ContentEngine;
import org.flowable.content.engine.ContentEngineConfiguration;
import org.flowable.content.engine.impl.util.CommandContextUtil;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public abstract class AbstractFlowableTestCase extends AbstractContentTestCase {

    private static final List<String> TABLENAMES_EXCLUDED_FROM_DB_CLEAN_CHECK = new ArrayList<>();

    static {
        TABLENAMES_EXCLUDED_FROM_DB_CLEAN_CHECK.add("ACT_CO_DATABASECHANGELOG");
        TABLENAMES_EXCLUDED_FROM_DB_CLEAN_CHECK.add("ACT_CO_DATABASECHANGELOGLOCK");
        TABLENAMES_EXCLUDED_FROM_DB_CLEAN_CHECK.add("ACT_GE_PROPERTY");
    }

    protected ContentEngine contentEngine;

    protected Throwable exception;

    protected ContentEngineConfiguration contentEngineConfiguration;
    protected ContentManagementService managementService;
    protected ContentService contentService;

    protected abstract void initializeContentEngine();

    // Default: do nothing
    protected void closeDownContentEngine() {
    }

    protected void nullifyServices() {
        contentEngineConfiguration = null;
        managementService = null;
        contentService = null;
    }

    @Override
    public void runBare() throws Throwable {
        initializeContentEngine();
        if (contentService == null) {
            initializeServices();
        }

        try {

            super.runBare();

        } catch (AssertionError e) {
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

            assertAndEnsureCleanDb();
            contentEngineConfiguration.getClock().reset();

            // Can't do this in the teardown, as the teardown will be called as part of the super.runBare
            closeDownContentEngine();
        }
    }

    /**
     * Each test is assumed to clean up all DB content it entered. After a test method executed, this method scans all tables to see if the DB is completely clean. It throws AssertionFailed in case
     * the DB is not clean. If the DB is not clean, it is cleaned by performing a create a drop.
     */
    protected void assertAndEnsureCleanDb() throws Throwable {
        EnsureCleanDbUtils.assertAndEnsureCleanDb(
                getName(),
                LOGGER,
                contentEngineConfiguration,
                TABLENAMES_EXCLUDED_FROM_DB_CLEAN_CHECK,
                exception == null,
                new Command<Void>() {
                    @Override
                    public Void execute(CommandContext commandContext) {
                        SchemaManager schemaManager = CommandContextUtil.getContentEngineConfiguration(commandContext).getSchemaManager();
                        schemaManager.schemaDrop();
                        schemaManager.schemaCreate();
                        return null;
                    }
                }
        );
    }

    protected void initializeServices() {
        contentEngineConfiguration = contentEngine.getContentEngineConfiguration();
        managementService = contentEngine.getContentManagementService();
        contentService = contentEngine.getContentService();
    }

}
