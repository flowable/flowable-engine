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
package org.flowable.common.engine.impl.persistence.entity.data.impl;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.cfg.IdGenerator;
import org.flowable.common.engine.impl.db.AbstractDataManager;
import org.flowable.common.engine.impl.persistence.entity.ChangeLogEntity;
import org.flowable.common.engine.impl.persistence.entity.ChangeLogEntityImpl;
import org.flowable.common.engine.impl.persistence.entity.data.ChangeLogDataManager;

public class MybatisChangeLogDataManager extends AbstractDataManager<ChangeLogEntity> implements ChangeLogDataManager {
    
    @Override
    public Class<? extends ChangeLogEntity> getManagedEntityClass() {
        return ChangeLogEntityImpl.class;
    }

    @Override
    public ChangeLogEntity create() {
        throw new FlowableException("Create is not supported for changelog entity");
    }

    @Override
    protected IdGenerator getIdGenerator() {
        return null;
    }
}
