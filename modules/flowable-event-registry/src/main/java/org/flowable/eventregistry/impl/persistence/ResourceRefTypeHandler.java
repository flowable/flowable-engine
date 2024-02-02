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
package org.flowable.eventregistry.impl.persistence;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeReference;
import org.flowable.eventregistry.impl.persistence.entity.ResourceRef;

/**
 * MyBatis TypeHandler for {@link ResourceRef}.
 * 
 * @author Marcus Klimstra (CGI)
 */
public class ResourceRefTypeHandler extends TypeReference<ResourceRef> implements TypeHandler<ResourceRef> {

    @Override
    public void setParameter(PreparedStatement ps, int i, ResourceRef parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, getValueToSet(parameter));
    }

    private String getValueToSet(ResourceRef parameter) {
        if (parameter == null) {
            // Note that this should not happen: ByteArrayRefs should always be initialized.
            return null;
        }
        return parameter.getId();
    }

    @Override
    public ResourceRef getResult(ResultSet rs, String columnName) throws SQLException {
        String id = rs.getString(columnName);
        return new ResourceRef(id);
    }

    @Override
    public ResourceRef getResult(ResultSet rs, int columnIndex) throws SQLException {
        String id = rs.getString(columnIndex);
        return new ResourceRef(id);
    }

    @Override
    public ResourceRef getResult(CallableStatement cs, int columnIndex) throws SQLException {
        String id = cs.getString(columnIndex);
        return new ResourceRef(id);
    }

}
