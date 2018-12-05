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
package org.flowable.cmmn.test.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.test.impl.CustomCmmnConfigurationFlowableTestCase;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskLogEntry;
import org.flowable.task.api.TaskLogEntryBuilder;
import org.flowable.task.api.TaskLogEntryQuery;
import org.hamcrest.MatcherAssert;
import org.junit.After;
import org.junit.Test;

/**
 * @author martin.grofcik
 */
public class CmmnHistoryServiceDisableTaskLogTest extends CustomCmmnConfigurationFlowableTestCase {

    protected Task task;

    @Override
    protected String getEngineName() {
        return this.getClass().getName();
    }

    @Override
    protected void configureConfiguration(CmmnEngineConfiguration cmmnEngineConfiguration) {
        cmmnEngineConfiguration.setEnableUserTaskDatabaseEventLogging(false);
    }

    @After
    public void deleteTasks() {
        if (task != null) {
            assertThat(cmmnTaskService.createTaskLogEntryQuery().taskId(task.getId()).count()).isEqualTo(0);
            cmmnHistoryService.deleteHistoricTaskInstance(task.getId());
            cmmnTaskService.deleteTask(task.getId());
        }
    }

    @Test
    public void createTaskEvent() {
        task = cmmnTaskService.createTaskBuilder().
            assignee("testAssignee").
            create();

    }

    @Test
    public void createTaskEventAsAuthenticatedUser() {
        String previousUserId = Authentication.getAuthenticatedUserId();
        Authentication.setAuthenticatedUserId("testUser");
        try {
            task = cmmnTaskService.createTaskBuilder().
                assignee("testAssignee").
                create();
        } finally {
            Authentication.setAuthenticatedUserId(previousUserId);
        }
    }

}
