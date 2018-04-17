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
package org.flowable.idm.engine.impl;

import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.idm.engine.impl.db.IdmDbSchemaManager;
import org.flowable.idm.engine.impl.util.CommandContextUtil;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public final class SchemaOperationsIdmEngineBuild implements Command<Object> {

    @Override
    public Object execute(CommandContext commandContext) {
        IdmDbSchemaManager idmDbSchemaManager = (IdmDbSchemaManager) CommandContextUtil.getIdmEngineConfiguration(commandContext).getDbSchemaManager();
        idmDbSchemaManager.performSchemaOperationsIdmEngineBuild();
        return null;
    }
}