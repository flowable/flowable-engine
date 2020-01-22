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
package org.flowable.eventregistry.impl.cmd;

import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.flowable.eventregistry.impl.db.EventDbSchemaManager;
import org.flowable.eventregistry.impl.util.CommandContextUtil;

/**
 * @author Joram Barrez
 */
public class SchemaOperationsEventRegistryEngineBuild implements Command<Void> {

    @Override
    public Void execute(CommandContext commandContext) {
        EventRegistryEngineConfiguration configuration = CommandContextUtil.getEventRegistryConfiguration(commandContext);
        ((EventDbSchemaManager) configuration.getSchemaManager()).initSchema(configuration);
        return null;
    }

}
