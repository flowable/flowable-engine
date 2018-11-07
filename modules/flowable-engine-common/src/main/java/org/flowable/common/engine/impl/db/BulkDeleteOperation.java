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
package org.flowable.common.engine.impl.db;

import org.apache.ibatis.session.SqlSession;
import org.flowable.common.engine.impl.persistence.entity.Entity;

/**
 * Use this to execute a dedicated delete statement. It is important to note there won't be any optimistic locking checks done for these kind of delete operations!
 * 
 * @author Joram Barrez
 */
public class BulkDeleteOperation {

    protected String statement;
    protected Object parameter;

    public BulkDeleteOperation(String statement, Object parameter) {
        this.statement = statement;
        this.parameter = parameter;
    }

    public void execute(SqlSession sqlSession, Class<? extends Entity> clazz) {
        sqlSession.delete(statement, parameter);
    }
    
    public String getStatement() {
        return statement;
    }

    public void setStatement(String statement) {
        this.statement = statement;
    }

    public Object getParameter() {
        return parameter;
    }

    public void setParameter(Object parameter) {
        this.parameter = parameter;
    }

    @Override
    public String toString() {
        return "bulk delete: " + statement + "(" + parameter + ")";
    }
}