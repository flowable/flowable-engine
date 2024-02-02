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
package org.flowable.cmmn.test.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class CmmnTaskDueDateTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment
    public void testDueDateTypes() throws ParseException {

        Date date = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss XXX").parse("30-01-2024 10:28:00 +01:00");
        Instant instant = date.toInstant();
        LocalDate localDate = LocalDate.of(2024, 1, 30);
        LocalDateTime localDateTime = LocalDateTime.of(2024, 1, 30, 10, 28);

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testDateTypes")
                .variable("date", date)
                .variable("instant", instant)
                .variable("localDate", localDate)
                .variable("localDateTime", localDateTime)
                .variable("stringDate", "2024-01-30T10:28:00+01:00")
                .start();

        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("dateTask").singleResult().getDueDate()).isEqualTo(date);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("instantTask").singleResult().getDueDate()).isEqualTo(instant);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("stringDateTask").singleResult().getDueDate()).isEqualTo(date);

        Date startOfDay = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("localDateTask").singleResult().getDueDate()).isEqualTo(startOfDay);

        Date localDateTimeToDate = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("localDateTimeTask").singleResult().getDueDate()).isEqualTo(localDateTimeToDate);

    }

}
