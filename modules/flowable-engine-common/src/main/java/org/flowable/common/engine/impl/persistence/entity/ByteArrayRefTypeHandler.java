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
package org.flowable.common.engine.impl.persistence.entity;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeReference;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;

/**
 * MyBatis TypeHandler for {@link ByteArrayRef}.
 * 
 * @author Marcus Klimstra (CGI)
 */
public class ByteArrayRefTypeHandler extends TypeReference<ByteArrayRef> implements TypeHandler<ByteArrayRef> {

    @Override
    public void setParameter(PreparedStatement ps, int i, ByteArrayRef parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, getValueToSet(parameter));
    }

    private String getValueToSet(ByteArrayRef parameter) {
        if (parameter == null) {
            // Note that this should not happen: ByteArrayRefs should always be initialized.
            return null;
        }
        return parameter.getId();
    }

    @Override
    public ByteArrayRef getResult(ResultSet rs, String columnName) throws SQLException {
        String id = rs.getString(columnName);
        return createVariableByteArrayRef(id);
    }

    @Override
    public ByteArrayRef getResult(ResultSet rs, int columnIndex) throws SQLException {
        String id = rs.getString(columnIndex);
        return createVariableByteArrayRef(id);
    }

    @Override
    public ByteArrayRef getResult(CallableStatement cs, int columnIndex) throws SQLException {
        String id = cs.getString(columnIndex);
        return createVariableByteArrayRef(id);
    }

    protected ByteArrayRef createVariableByteArrayRef(String id) {
        if (id == null) {
            return null;
        }
        return new ByteArrayRef(id, getCommandExecutor());
    }


    protected CommandExecutor getCommandExecutor() {
        CommandContext commandContext = Context.getCommandContext();
        if (commandContext != null) {
            // There is always a command context and engine here
            // However, just to be extra safe do a null check
            return commandContext.getCommandExecutor();
        }
        return null;
    }
}
