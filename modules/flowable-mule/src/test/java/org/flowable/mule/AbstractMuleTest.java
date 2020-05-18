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
package org.flowable.mule;

import java.util.Arrays;
import java.util.List;

import org.flowable.common.engine.impl.db.SchemaManager;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.test.EnsureCleanDbUtils;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.impl.util.CommandContextUtil;
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
        EnsureCleanDbUtils.assertAndEnsureCleanDb(
                getClass().getSimpleName() + "." + name.getMethodName(),
                LOGGER,
                processEngine.getProcessEngineConfiguration(),
                TABLENAMES_EXCLUDED_FROM_DB_CLEAN_CHECK,
                true,
                new Command<Void>() {
                    @Override
                    public Void execute(CommandContext commandContext) {
                        SchemaManager schemaManager = CommandContextUtil.getProcessEngineConfiguration(commandContext).getSchemaManager();
                        schemaManager.schemaDrop();
                        schemaManager.schemaCreate();
                        return null;
                    }
                }
        );
    }

}
