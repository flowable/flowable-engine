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
package org.flowable.job.service.impl.asyncexecutor;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.flowable.job.service.impl.persistence.entity.JobInfoEntity;

/**
 * @author Tijs Rademakers
 */
public class AcquiredJobEntities {

    protected Map<String, JobInfoEntity> acquiredJobs = new HashMap<>();

    public void addJob(JobInfoEntity job) {
        acquiredJobs.put(job.getId(), job);
    }

    public Collection<JobInfoEntity> getJobs() {
        return acquiredJobs.values();
    }

    public boolean contains(String jobId) {
        return acquiredJobs.containsKey(jobId);
    }

    public int size() {
        return acquiredJobs.size();
    }
}
