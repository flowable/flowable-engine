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
package org.flowable.mongodb.persistence.mapper;

import org.bson.Document;
import org.flowable.job.service.impl.persistence.entity.JobEntityImpl;
import org.flowable.mongodb.persistence.EntityToDocumentMapper;

public class JobEntityMapper extends AbstractJobEntityMapper implements EntityToDocumentMapper<JobEntityImpl> {

    @Override
    public JobEntityImpl fromDocument(Document document) {
        JobEntityImpl jobEntity = new JobEntityImpl();
        copyJobInfoFromDocument(document, jobEntity);
        jobEntity.setLockExpirationTime(document.getDate("lockExpirationTime"));
        jobEntity.setLockOwner(document.getString("lockOwner"));
        
        return jobEntity;
    }
    
    @Override
    public Document toDocument(JobEntityImpl jobEntity) {
        Document jobDocument = copyJobInfoToDocument(jobEntity);
        jobDocument.append("lockExpirationTime", jobEntity.getLockExpirationTime());
        jobDocument.append("lockOwner", jobEntity.getLockOwner());
        
        return jobDocument;
    }

}
