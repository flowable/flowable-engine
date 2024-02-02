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
package org.flowable.eventregistry.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.io.StringReader;
import java.sql.Connection;

import org.apache.ibatis.jdbc.ScriptRunner;
import org.flowable.common.engine.impl.db.DbSqlSession;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.eventregistry.api.ChannelDefinition;
import org.flowable.eventregistry.impl.cmd.UpdateChannelDefinitionTypeAndImplementationForAllChannelDefinitionsCmd;
import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
class UpdateChannelDefinitionTypeAndImplementationForAllChannelDefinitionsCmdTest extends AbstractFlowableEventTest {

    @Test
    @EventDeploymentAnnotation(resources = {
            "org/flowable/eventregistry/test/repository/one.channel",
            "org/flowable/eventregistry/test/repository/one-outbound.channel",
            "org/flowable/eventregistry/test/repository/two.channel"
    })
    void updatingChannelDefinitionTypeAndImplementationShouldWork() {
        assertThat(repositoryService.createChannelDefinitionQuery().list())
                .extracting(ChannelDefinition::getKey, ChannelDefinition::getType, ChannelDefinition::getImplementation)
                .containsExactlyInAnyOrder(
                        tuple("one", "inbound", "jms"),
                        tuple("one-outbound", "outbound", "rabbit"),
                        tuple("two", "inbound", "jms")
                );
        CommandExecutor commandExecutor = eventEngineConfiguration.getCommandExecutor();

        commandExecutor.execute(commandContext -> {
            Connection connection = commandContext.getSession(DbSqlSession.class)
                    .getSqlSession()
                    .getConnection();

            ScriptRunner runner = new ScriptRunner(connection);
            runner.runScript(new StringReader("UPDATE FLW_CHANNEL_DEFINITION SET TYPE_ = null, IMPLEMENTATION_ = null;"));

            return null;
        });

        assertThat(repositoryService.createChannelDefinitionQuery().list())
                .extracting(ChannelDefinition::getKey, ChannelDefinition::getType, ChannelDefinition::getImplementation)
                .containsExactlyInAnyOrder(
                        tuple("one", null, null),
                        tuple("one-outbound", null, null),
                        tuple("two", null, null)
                );

        commandExecutor.execute(new UpdateChannelDefinitionTypeAndImplementationForAllChannelDefinitionsCmd());

        assertThat(repositoryService.createChannelDefinitionQuery().list())
                .extracting(ChannelDefinition::getKey, ChannelDefinition::getType, ChannelDefinition::getImplementation)
                .containsExactlyInAnyOrder(
                        tuple("one", "inbound", "jms"),
                        tuple("one-outbound", "outbound", "rabbit"),
                        tuple("two", "inbound", "jms")
                );
    }
}
