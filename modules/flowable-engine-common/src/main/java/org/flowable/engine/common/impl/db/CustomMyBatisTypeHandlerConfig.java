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
package org.flowable.engine.common.impl.db;

import org.apache.ibatis.type.JdbcType;

/**
 * @author Joram Barrez
 */
public class CustomMyBatisTypeHandlerConfig {
    
    protected Class<?> javaTypeClass;
    protected JdbcType jdbcType;
    protected Class<?> typeHandlerClass;
    
    public CustomMyBatisTypeHandlerConfig(Class<?> javaTypeClass, JdbcType jdbcType, Class<?> typeHandlerClass) {
        this.javaTypeClass = javaTypeClass;
        this.jdbcType = jdbcType;
        this.typeHandlerClass = typeHandlerClass;
    }

    public Class<?> getJavaTypeClass() {
        return javaTypeClass;
    }

    public void setJavaTypeClass(Class<?> javaTypeClass) {
        this.javaTypeClass = javaTypeClass;
    }

    public JdbcType getJdbcType() {
        return jdbcType;
    }

    public void setJdbcType(JdbcType jdbcType) {
        this.jdbcType = jdbcType;
    }

    public Class<?> getTypeHandlerClass() {
        return typeHandlerClass;
    }

    public void setTypeHandlerClass(Class<?> typeHandlerClass) {
        this.typeHandlerClass = typeHandlerClass;
    }
    
}
