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

package org.flowable.content.engine.impl.cmd;

import static org.flowable.common.engine.api.tenant.ChangeTenantIdResult.Key.ContentItemInstances;

import org.flowable.content.engine.ContentEngineConfiguration;
import org.flowable.content.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.tenant.ChangeTenantIdResult;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChangeTenantIdContentCompleteCmd implements Command<ChangeTenantIdResult> {

        private static final Logger LOGGER = LoggerFactory.getLogger(ChangeTenantIdContentCompleteCmd.class);

        private final String sourceTenantId;
        private final String targetTenantId;

        public ChangeTenantIdContentCompleteCmd(String sourceTenantId, String targetTenantId) {
                this.sourceTenantId = sourceTenantId;
                this.targetTenantId = targetTenantId;
        }

        @Override
        public ChangeTenantIdResult execute(CommandContext commandContext) {
                LOGGER.debug("Executing Content instance migration from '{}' to '{}'.", sourceTenantId, targetTenantId);
                ContentEngineConfiguration contentEngineConfiguration = CommandContextUtil.getContentEngineConfiguration(commandContext);
                long changeTenantIdContentItemInstances = contentEngineConfiguration.getContentItemEntityManager()
                                                .changeTenantIdContentItemInstances(sourceTenantId, targetTenantId);
                return ChangeTenantIdResult.builder()
                                .addResult(ContentItemInstances,changeTenantIdContentItemInstances)
                                .build();
        }

}