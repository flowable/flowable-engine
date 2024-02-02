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
package org.flowable.eventregistry.impl.db;

import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.eventregistry.impl.cmd.UpdateChannelDefinitionTypeAndImplementationForAllChannelDefinitionsCmd;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

/**
 * @author Filip Hrisafov
 */
public class SetChannelDefinitionTypeAndImplementationCustomChange implements CustomTaskChange {

    @Override
    public void execute(Database database) throws CustomChangeException {

        CommandContext commandContext = Context.getCommandContext();
        if (commandContext != null) {
            new UpdateChannelDefinitionTypeAndImplementationForAllChannelDefinitionsCmd().execute(commandContext);
        }
    }

    @Override
    public String getConfirmationMessage() {
        return "Set Channel Definition type and implementation columns";
    }

    @Override
    public void setUp() throws SetupException {

    }

    @Override
    public void setFileOpener(ResourceAccessor resourceAccessor) {

    }

    @Override
    public ValidationErrors validate(Database database) {
        return new ValidationErrors();
    }
}
