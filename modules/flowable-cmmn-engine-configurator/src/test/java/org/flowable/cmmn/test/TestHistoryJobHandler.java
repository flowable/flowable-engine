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
package org.flowable.cmmn.test;

import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.service.HistoryJobHandler;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;

/**
 * @author Filip Hrisafov
 */
public class TestHistoryJobHandler implements HistoryJobHandler {

    protected final String type;

    public TestHistoryJobHandler(String type) {
        this.type = type;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void execute(HistoryJobEntity job, String configuration, CommandContext commandContext, JobServiceConfiguration jobServiceConfiguration) {

    }
}
