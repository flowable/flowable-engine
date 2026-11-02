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

import java.nio.charset.Charset;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.db.DbUpgradeStep;
import org.flowable.common.engine.impl.db.SchemaManagerDatabaseConfiguration;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.flowable.eventregistry.impl.util.CommandContextUtil;
import org.flowable.eventregistry.model.ChannelModel;

/**
 * @author Filip Hrisafov
 */
public class UpdateChannelDefinitionTypeAndImplementationForAllChannelDefinitionsCmd implements Command<Void>, DbUpgradeStep {

    @Override
    public void execute() throws Exception {
        execute(Context.getCommandContext());
    }

    @Override
    public Void execute(CommandContext commandContext) {
        SchemaManagerDatabaseConfiguration databaseConfiguration = getDatabaseConfiguration(commandContext);
        Connection connection = databaseConfiguration.getConnection();
        EventRegistryEngineConfiguration configuration = CommandContextUtil.getEventRegistryConfiguration(commandContext);
        String encoding = configuration.getXmlEncoding();
        Charset encodingCharset = encoding != null ? Charset.forName(encoding) : Charset.defaultCharset();

        String channelDefinitionTableName = "FLW_CHANNEL_DEFINITION";
        String eventResourceTableName = "FLW_EVENT_RESOURCE";

        if (!databaseConfiguration.isTablePrefixIsSchema()) {
            channelDefinitionTableName = databaseConfiguration.getDatabaseTablePrefix() + channelDefinitionTableName;
            eventResourceTableName = databaseConfiguration.getDatabaseTablePrefix() + eventResourceTableName;
        }

        try (PreparedStatement queryStatement = connection.prepareStatement("""
                select DEF.ID_ as ID_, RES.RESOURCE_BYTES_ as RESOURCE_BYTES_
                from %s DEF
                inner join %s RES on DEF.DEPLOYMENT_ID_ = RES.DEPLOYMENT_ID_ and DEF.RESOURCE_NAME_ = RES.NAME_
                """.formatted(channelDefinitionTableName, eventResourceTableName));
             PreparedStatement updateStatement = connection.prepareStatement(
                     "update %s set TYPE_ = ?, IMPLEMENTATION_ = ? where ID_ = ?".formatted(channelDefinitionTableName)
             )
        ) {
            ResultSet resultSet = queryStatement.executeQuery();
            while (resultSet.next()) {
                String definitionId = resultSet.getString("ID_");
                byte[] resourceBytes = getResourceBytes(resultSet, databaseConfiguration);
                if (resourceBytes == null) {
                    continue;
                }

                ChannelModel model = configuration.getChannelJsonConverter().convertToChannelModel(new String(resourceBytes, encodingCharset));
                String type = model.getChannelType();
                if (type == null) {
                    updateStatement.setNull(1, java.sql.Types.VARCHAR);
                } else {
                    updateStatement.setString(1, type);
                }
                String implementation = model.getType();
                if (implementation == null) {
                    updateStatement.setNull(2, java.sql.Types.VARCHAR);
                } else {
                    updateStatement.setString(2, implementation);
                }
                updateStatement.setString(3, definitionId);
                updateStatement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    protected byte[] getResourceBytes(ResultSet resultSet, SchemaManagerDatabaseConfiguration databaseConfiguration) throws SQLException {
        if ("postgres".equals(databaseConfiguration.getDatabaseType())) {
            return resultSet.getBytes("RESOURCE_BYTES_");
        }

        Blob blob = resultSet.getBlob("RESOURCE_BYTES_");
        if (blob != null) {
            return blob.getBytes(1, (int) blob.length());
        }
        return null;
    }

    protected SchemaManagerDatabaseConfiguration getDatabaseConfiguration(CommandContext commandContext) {
        return commandContext.getSession(SchemaManagerDatabaseConfiguration.class);
    }
}
