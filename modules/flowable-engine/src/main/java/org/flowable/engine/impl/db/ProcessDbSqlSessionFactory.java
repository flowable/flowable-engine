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

package org.flowable.engine.impl.db;

import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.db.DbSqlSession;
import org.flowable.common.engine.impl.db.DbSqlSessionFactory;
import org.flowable.common.engine.impl.persistence.cache.EntityCache;

/**
 * @author martin.grofcik
 */
public class ProcessDbSqlSessionFactory extends DbSqlSessionFactory {

    public ProcessDbSqlSessionFactory(boolean usePrefixId) {
        super(usePrefixId);
    }

    protected DbSqlSession createDbSqlSession() {
        return new ProcessDbSqlSession(this, Context.getCommandContext().getSession(EntityCache.class));
    }

}
