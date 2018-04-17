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
package org.flowable.job.service.impl.persistence.entity.data.impl;

import java.util.List;

import org.flowable.common.engine.impl.db.AbstractDataManager;
import org.flowable.job.service.impl.persistence.entity.JobByteArrayEntity;
import org.flowable.job.service.impl.persistence.entity.JobByteArrayEntityImpl;
import org.flowable.job.service.impl.persistence.entity.data.JobByteArrayDataManager;

/**
 * @author Joram Barrez
 */
public class MybatisJobByteArrayDataManager extends AbstractDataManager<JobByteArrayEntity> implements JobByteArrayDataManager {

    @Override
    public JobByteArrayEntity create() {
        return new JobByteArrayEntityImpl();
    }

    @Override
    public Class<? extends JobByteArrayEntity> getManagedEntityClass() {
        return JobByteArrayEntityImpl.class;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<JobByteArrayEntity> findAll() {
        return getDbSqlSession().selectList("selectJobByteArrays");
    }

    @Override
    public void deleteByteArrayNoRevisionCheck(String byteArrayEntityId) {
        getDbSqlSession().delete("deleteJobByteArrayNoRevisionCheck", byteArrayEntityId, JobByteArrayEntityImpl.class);
    }

}
