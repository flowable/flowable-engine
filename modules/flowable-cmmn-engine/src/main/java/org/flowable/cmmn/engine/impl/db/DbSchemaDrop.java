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

package org.flowable.cmmn.engine.impl.db;

import java.io.IOException;
import java.io.InputStream;

import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandConfig;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakwrs
 */
public class DbSchemaDrop {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DbSchemaDrop.class);

    public static void main(String[] args) {
        try (InputStream inputStream = FlowableCmmnTestCase.class.getClassLoader().getResourceAsStream("flowable.cmmn.cfg.xml")) {
            CmmnEngine cmmnEngine = CmmnEngineConfiguration.createCmmnEngineConfigurationFromInputStream(inputStream).buildCmmnEngine();
            CommandExecutor commandExecutor = cmmnEngine.getCmmnEngineConfiguration().getCommandExecutor();
            CommandConfig config = new CommandConfig().transactionNotSupported();
            commandExecutor.execute(config, new Command<Object>() {
                @Override
                public Object execute(CommandContext commandContext) {
                    CommandContextUtil.getCmmnEngineConfiguration(commandContext).getDbSchemaManager().dbSchemaDrop();
                    return null;
                }
            });
            
        } catch (IOException e) {
            LOGGER.error("Could not create CMMN engine", e);
        }
    }
}
