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

package org.flowable.engine.test.history;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceBuilder;
import org.flowable.engine.test.Deployment;
import org.flowable.identitylink.api.history.HistoricIdentityLink;
import org.junit.jupiter.api.Test;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class HistoricProcessInstanceTest extends PluggableFlowableTestCase {

    @Test
    @Deployment(resources = { "org/flowable/engine/test/history/oneTaskProcess.bpmn20.xml" })
    public void testHistoricDataCreatedForProcessExecution() {

        Calendar calendar = new GregorianCalendar();
        calendar.set(Calendar.YEAR, 2010);
        calendar.set(Calendar.MONTH, 8);
        calendar.set(Calendar.DAY_OF_MONTH, 30);
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date noon = calendar.getTime();

        processEngineConfiguration.getClock().setCurrentTime(noon);
        final ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", "myBusinessKey");
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        assertEquals(1, historyService.createHistoricProcessInstanceQuery().unfinished().count());
        assertEquals(0, historyService.createHistoricProcessInstanceQuery().finished().count());
        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();

        assertNotNull(historicProcessInstance);
        assertEquals(processInstance.getId(), historicProcessInstance.getId());
        assertEquals(processInstance.getBusinessKey(), historicProcessInstance.getBusinessKey());
        assertEquals(processInstance.getProcessDefinitionId(), historicProcessInstance.getProcessDefinitionId());
        assertEquals(noon, historicProcessInstance.getStartTime());
        assertNull(historicProcessInstance.getEndTime());
        assertNull(historicProcessInstance.getDurationInMillis());

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();

        assertEquals(1, tasks.size());

        // in this test scenario we assume that 25 seconds after the process start, the
        // user completes the task (yes! he must be almost as fast as me)
        Date twentyFiveSecsAfterNoon = new Date(noon.getTime() + 25 * 1000);
        processEngineConfiguration.getClock().setCurrentTime(twentyFiveSecsAfterNoon);
        taskService.complete(tasks.get(0).getId());
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        historicProcessInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();

        assertNotNull(historicProcessInstance);
        assertEquals(processInstance.getId(), historicProcessInstance.getId());
        assertEquals(processInstance.getProcessDefinitionId(), historicProcessInstance.getProcessDefinitionId());
        assertEquals(noon, historicProcessInstance.getStartTime());
        assertEquals(twentyFiveSecsAfterNoon, historicProcessInstance.getEndTime());
        assertEquals(new Long(25 * 1000), historicProcessInstance.getDurationInMillis());

        assertEquals(0, historyService.createHistoricProcessInstanceQuery().unfinished().count());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().finished().count());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/history/oneTaskProcess.bpmn20.xml" })
    public void testDeleteProcessInstanceHistoryCreated() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertNotNull(processInstance);

        // delete process instance should not delete the history
        runtimeService.deleteProcessInstance(processInstance.getId(), "cancel");
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
        
        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(historicProcessInstance.getEndTime());
    }

    /*
     * @Test
     * @Deployment(resources = {"org/flowable/engine/test/history/oneTaskProcess.bpmn20.xml"}) public void testHistoricProcessInstanceVariables() { Map<String,Object> vars = new
     * HashMap<String,Object>(); vars.put("foo", "bar"); vars.put("baz", "boo");
     * 
     * runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
     * 
     * assertEquals(1, historyService.createHistoricProcessInstanceQuery().processVariableEquals ("foo", "bar").count()); assertEquals(1, historyService.createHistoricProcessInstanceQuery
     * ().processVariableEquals("baz", "boo").count()); assertEquals(1, historyService .createHistoricProcessInstanceQuery().processVariableEquals("foo", "bar").processVariableEquals("baz",
     * "boo").count()); }
     */

    @Test
    @Deployment(resources = { "org/flowable/engine/test/history/oneTaskProcess.bpmn20.xml" })
    public void testHistoricProcessInstanceQuery() {
        Calendar startTime = Calendar.getInstance();

        processEngineConfiguration.getClock().setCurrentTime(startTime.getTime());
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", "businessKey123");
        runtimeService.addUserIdentityLink(processInstance.getId(), "kermit", "someType");
        runtimeService.setProcessInstanceName(processInstance.getId(), "The name");
        Calendar hourAgo = Calendar.getInstance();
        hourAgo.add(Calendar.HOUR_OF_DAY, -1);
        Calendar hourFromNow = Calendar.getInstance();
        hourFromNow.add(Calendar.HOUR_OF_DAY, 1);
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        // Name and name like
        assertEquals("The name", historyService.createHistoricProcessInstanceQuery().processInstanceName("The name").singleResult().getName());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().processInstanceName("The name").count());
        assertEquals(0, historyService.createHistoricProcessInstanceQuery().processInstanceName("Other name").count());

        assertEquals(1, historyService.createHistoricProcessInstanceQuery().processInstanceNameLike("% name").count());
        assertEquals(0, historyService.createHistoricProcessInstanceQuery().processInstanceNameLike("%nope").count());

        // Query after update name
        runtimeService.setProcessInstanceName(processInstance.getId(), "New name");
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
        
        assertEquals("New name", historyService.createHistoricProcessInstanceQuery().processInstanceName("New name").singleResult().getName());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().processInstanceName("New name").count());
        assertEquals(0, historyService.createHistoricProcessInstanceQuery().processInstanceName("The name").count());

        assertEquals(1, historyService.createHistoricProcessInstanceQuery().processInstanceNameLike("New %").count());
        assertEquals(0, historyService.createHistoricProcessInstanceQuery().processInstanceNameLike("The %").count());

        // Start/end dates
        assertEquals(0, historyService.createHistoricProcessInstanceQuery().finishedBefore(hourAgo.getTime()).count());
        assertEquals(0, historyService.createHistoricProcessInstanceQuery().finishedBefore(hourFromNow.getTime()).count());
        assertEquals(0, historyService.createHistoricProcessInstanceQuery().finishedAfter(hourAgo.getTime()).count());
        assertEquals(0, historyService.createHistoricProcessInstanceQuery().finishedAfter(hourFromNow.getTime()).count());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().startedBefore(hourFromNow.getTime()).count());
        assertEquals(0, historyService.createHistoricProcessInstanceQuery().startedBefore(hourAgo.getTime()).count());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().startedAfter(hourAgo.getTime()).count());
        assertEquals(0, historyService.createHistoricProcessInstanceQuery().startedAfter(hourFromNow.getTime()).count());

        // General fields
        assertEquals(0, historyService.createHistoricProcessInstanceQuery().finished().count());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().processDefinitionId(processInstance.getProcessDefinitionId()).count());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().processDefinitionKey("oneTaskProcess").count());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().processDefinitionKeyIn(Collections.singletonList("oneTaskProcess")).count());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().processDefinitionKeyIn(Arrays.asList("undefined", "oneTaskProcess")).count());
        assertEquals(0, historyService.createHistoricProcessInstanceQuery().processDefinitionKeyIn(Arrays.asList("undefined1", "undefined2")).count());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().processInstanceBusinessKey("businessKey123").count());

        List<String> excludeIds = new ArrayList<>();
        excludeIds.add("unexistingProcessDefinition");

        assertEquals(1, historyService.createHistoricProcessInstanceQuery().processDefinitionKeyNotIn(excludeIds).count());

        excludeIds.add("oneTaskProcess");
        assertEquals(0, historyService.createHistoricProcessInstanceQuery().processDefinitionKeyNotIn(excludeIds).count());

        // After finishing process
        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId());
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
        
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().finished().count());
        assertEquals(0, historyService.createHistoricProcessInstanceQuery().finishedBefore(hourAgo.getTime()).count());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().finishedBefore(hourFromNow.getTime()).count());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().finishedAfter(hourAgo.getTime()).count());
        assertEquals(0, historyService.createHistoricProcessInstanceQuery().finishedAfter(hourFromNow.getTime()).count());

        // Check identity links
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().involvedUser("kermit").count());
        assertEquals(0, historyService.createHistoricProcessInstanceQuery().involvedUser("gonzo").count());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/history/oneTaskProcess.bpmn20.xml" })
    public void testHistoricProcessInstanceOrQuery() {
        Calendar startTime = Calendar.getInstance();

        processEngineConfiguration.getClock().setCurrentTime(startTime.getTime());
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", "businessKey123");
        runtimeService.addUserIdentityLink(processInstance.getId(), "kermit", "someType");
        runtimeService.setProcessInstanceName(processInstance.getId(), "The name");
        Calendar hourAgo = Calendar.getInstance();
        hourAgo.add(Calendar.HOUR_OF_DAY, -1);
        Calendar hourFromNow = Calendar.getInstance();
        hourFromNow.add(Calendar.HOUR_OF_DAY, 1);
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        // Name and name like
        assertEquals("The name", historyService.createHistoricProcessInstanceQuery().or().processInstanceName("The name").processDefinitionId("undefined").endOr().singleResult().getName());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().or().processInstanceName("The name").processDefinitionId("undefined").endOr().count());
        assertEquals(0, historyService.createHistoricProcessInstanceQuery().or().processInstanceName("Other name").processDefinitionId("undefined").endOr().count());

        assertEquals(1, historyService.createHistoricProcessInstanceQuery().or().processInstanceNameLike("% name").processDefinitionId("undefined").endOr().count());
        assertEquals(0, historyService.createHistoricProcessInstanceQuery().or().processInstanceNameLike("%nope").processDefinitionId("undefined").endOr().count());

        assertEquals(1, historyService.createHistoricProcessInstanceQuery()
                .or()
                .processInstanceName("The name")
                .processDefinitionId("undefined")
                .endOr()
                .or()
                .processInstanceNameLike("% name")
                .processDefinitionId("undefined")
                .endOr()
                .count());

        assertEquals(0, historyService.createHistoricProcessInstanceQuery()
                .or()
                .processInstanceName("The name")
                .processDefinitionId("undefined")
                .endOr()
                .or()
                .processInstanceNameLike("undefined")
                .processDefinitionId("undefined")
                .endOr()
                .count());

        // Query after update name
        runtimeService.setProcessInstanceName(processInstance.getId(), "New name");
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
        
        assertEquals("New name", historyService.createHistoricProcessInstanceQuery().or().processInstanceName("New name").processDefinitionId("undefined").endOr().singleResult().getName());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().or().processInstanceName("New name").processDefinitionId("undefined").endOr().count());
        assertEquals(0, historyService.createHistoricProcessInstanceQuery().or().processInstanceName("The name").processDefinitionId("undefined").endOr().count());

        assertEquals(1, historyService.createHistoricProcessInstanceQuery().or().processInstanceNameLike("New %").processDefinitionId("undefined").endOr().count());
        assertEquals(0, historyService.createHistoricProcessInstanceQuery().or().processInstanceNameLike("The %").processDefinitionId("undefined").endOr().count());

        // Start/end dates
        assertEquals(0, historyService.createHistoricProcessInstanceQuery().or().finishedBefore(hourAgo.getTime()).processDefinitionId("undefined").endOr().count());
        assertEquals(0, historyService.createHistoricProcessInstanceQuery().or().finishedBefore(hourFromNow.getTime()).processDefinitionId("undefined").endOr().count());
        assertEquals(0, historyService.createHistoricProcessInstanceQuery().or().finishedAfter(hourAgo.getTime()).processDefinitionId("undefined").endOr().count());
        assertEquals(0, historyService.createHistoricProcessInstanceQuery().or().finishedAfter(hourFromNow.getTime()).processDefinitionId("undefined").endOr().count());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().or().startedBefore(hourFromNow.getTime()).processDefinitionId("undefined").endOr().count());
        assertEquals(0, historyService.createHistoricProcessInstanceQuery().or().startedBefore(hourAgo.getTime()).processDefinitionId("undefined").endOr().count());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().or().startedAfter(hourAgo.getTime()).processDefinitionId("undefined").endOr().count());
        assertEquals(0, historyService.createHistoricProcessInstanceQuery().or().startedAfter(hourFromNow.getTime()).processDefinitionId("undefined").endOr().count());

        // General fields
        assertEquals(0, historyService.createHistoricProcessInstanceQuery().or().finished().count());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().or().processInstanceId(processInstance.getId()).processDefinitionId("undefined").endOr().count());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().or().processDefinitionId(processInstance.getProcessDefinitionId()).processDefinitionKey("undefined").count());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().or().processDefinitionId("undefined").processDefinitionKeyIn(Arrays.asList("undefined", "oneTaskProcess")).endOr().count());
        assertEquals(0, historyService.createHistoricProcessInstanceQuery().or().processDefinitionId("undefined").processDefinitionKeyIn(Arrays.asList("undefined1", "undefined2")).endOr().count());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().or().processDefinitionKey("oneTaskProcess").processDefinitionId("undefined").endOr().count());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().or().processInstanceBusinessKey("businessKey123").processDefinitionId("undefined").endOr().count());

        List<String> excludeIds = new ArrayList<>();
        excludeIds.add("unexistingProcessDefinition");

        assertEquals(1, historyService.createHistoricProcessInstanceQuery().or().processDefinitionKeyNotIn(excludeIds).processDefinitionId("undefined").endOr().count());

        excludeIds.add("oneTaskProcess");
        assertEquals(0, historyService.createHistoricProcessInstanceQuery().or().processDefinitionKeyNotIn(excludeIds).processDefinitionId("undefined").endOr().count());

        // After finishing process
        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId());
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
        
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().or().finished().processDefinitionId("undefined").endOr().count());
        assertEquals(0, historyService.createHistoricProcessInstanceQuery().or().finishedBefore(hourAgo.getTime()).processDefinitionId("undefined").endOr().count());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().or().finishedBefore(hourFromNow.getTime()).processDefinitionId("undefined").endOr().count());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().or().finishedAfter(hourAgo.getTime()).processDefinitionId("undefined").endOr().count());
        assertEquals(0, historyService.createHistoricProcessInstanceQuery().or().finishedAfter(hourFromNow.getTime()).processDefinitionId("undefined").endOr().count());

        // Check identity links
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().or().involvedUser("kermit").processDefinitionId("undefined").endOr().count());
        assertEquals(0, historyService.createHistoricProcessInstanceQuery().or().involvedUser("gonzo").processDefinitionId("undefined").endOr().count());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/history/oneTaskProcess.bpmn20.xml" })
    public void testHistoricProcessInstanceSorting() {
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        assertEquals(1, historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceId().asc().list().size());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceStartTime().asc().list().size());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceEndTime().asc().list().size());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceDuration().asc().list().size());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().orderByProcessDefinitionId().asc().list().size());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceBusinessKey().asc().list().size());

        assertEquals(1, historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceId().desc().list().size());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceStartTime().desc().list().size());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceEndTime().desc().list().size());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceDuration().desc().list().size());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().orderByProcessDefinitionId().desc().list().size());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceBusinessKey().desc().list().size());

        assertEquals(1, historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceId().asc().count());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceStartTime().asc().count());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceEndTime().asc().count());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceDuration().asc().count());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().orderByProcessDefinitionId().asc().count());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceBusinessKey().asc().count());

        assertEquals(1, historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceId().desc().count());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceStartTime().desc().count());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceEndTime().desc().count());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceDuration().desc().count());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().orderByProcessDefinitionId().desc().count());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceBusinessKey().desc().count());

        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        // First complete process instance 2
        for (org.flowable.task.api.Task task : taskService.createTaskQuery().processInstanceId(processInstance2.getId()).list()) {
            taskService.complete(task.getId());
        }

        // Then process instance 1
        for (org.flowable.task.api.Task task : taskService.createTaskQuery().processInstanceId(processInstance1.getId()).list()) {
            taskService.complete(task.getId());
        }
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        assertEquals(2, historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceId().asc().list().size());
        assertEquals(2, historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceStartTime().asc().list().size());
        assertEquals(2, historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceEndTime().asc().list().size());
        assertEquals(2, historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceDuration().asc().list().size());
        assertEquals(2, historyService.createHistoricProcessInstanceQuery().orderByProcessDefinitionId().asc().list().size());
        assertEquals(2, historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceBusinessKey().asc().list().size());

        assertEquals(2, historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceId().desc().list().size());
        assertEquals(2, historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceStartTime().desc().list().size());
        assertEquals(2, historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceEndTime().desc().list().size());
        assertEquals(2, historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceDuration().desc().list().size());
        assertEquals(2, historyService.createHistoricProcessInstanceQuery().orderByProcessDefinitionId().desc().list().size());
        assertEquals(2, historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceBusinessKey().desc().list().size());

        assertEquals(2, historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceId().asc().count());
        assertEquals(2, historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceStartTime().asc().count());
        assertEquals(2, historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceEndTime().asc().count());
        assertEquals(2, historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceDuration().asc().count());
        assertEquals(2, historyService.createHistoricProcessInstanceQuery().orderByProcessDefinitionId().asc().count());
        assertEquals(2, historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceBusinessKey().asc().count());

        assertEquals(2, historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceId().desc().count());
        assertEquals(2, historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceStartTime().desc().count());
        assertEquals(2, historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceEndTime().desc().count());
        assertEquals(2, historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceDuration().desc().count());
        assertEquals(2, historyService.createHistoricProcessInstanceQuery().orderByProcessDefinitionId().desc().count());
        assertEquals(2, historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceBusinessKey().desc().count());

        // Verify orderByProcessInstanceEndTime
        List<HistoricProcessInstance> historicProcessInstances = historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceEndTime().desc().list();
        // only check for existence and assume that the SQL processing has ordered the values correctly
        // see https://github.com/flowable/flowable-engine/issues/8
        List<String> processInstance = new ArrayList<>(2);
        processInstance.add(historicProcessInstances.get(0).getId());
        processInstance.add(historicProcessInstances.get(1).getId());
        assertTrue(processInstance.contains(processInstance1.getId()));
        assertTrue(processInstance.contains(processInstance2.getId()));

        // Verify again, with variables included (bug reported on that)
        historicProcessInstances = historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceEndTime().desc().includeProcessVariables().list();
        processInstance = new ArrayList<>(4);
        processInstance.add(historicProcessInstances.get(0).getId());
        processInstance.add(historicProcessInstances.get(1).getId());
        assertTrue(processInstance.contains(processInstance1.getId()));
        assertTrue(processInstance.contains(processInstance2.getId()));
    }

    @Test
    public void testInvalidSorting() {
        try {
            historyService.createHistoricProcessInstanceQuery().asc();
            fail();
        } catch (FlowableIllegalArgumentException e) {

        }

        try {
            historyService.createHistoricProcessInstanceQuery().desc();
            fail();
        } catch (FlowableIllegalArgumentException e) {

        }

        try {
            historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceId().list();
            fail();
        } catch (FlowableIllegalArgumentException e) {

        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/history/oneTaskProcess.bpmn20.xml" })
    // ACT-1098
    public void testDeleteReason() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            final String deleteReason = "some delete reason";
            ProcessInstance pi = runtimeService.startProcessInstanceByKey("oneTaskProcess");
            runtimeService.deleteProcessInstance(pi.getId(), deleteReason);
            
            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
            
            HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery().processInstanceId(pi.getId()).singleResult();
            assertEquals(deleteReason, hpi.getDeleteReason());
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/history/oneTaskProcess.bpmn20.xml" })
    public void testHistoricIdentityLinksOnProcessInstance() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            ProcessInstance pi = runtimeService.startProcessInstanceByKey("oneTaskProcess");
            runtimeService.addUserIdentityLink(pi.getId(), "kermit", "myType");
            
            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            // Check historic links
            List<HistoricIdentityLink> historicLinks = historyService.getHistoricIdentityLinksForProcessInstance(pi.getId());
            assertEquals(1, historicLinks.size());

            assertEquals("myType", historicLinks.get(0).getType());
            assertEquals("kermit", historicLinks.get(0).getUserId());
            assertNull(historicLinks.get(0).getGroupId());
            assertEquals(pi.getId(), historicLinks.get(0).getProcessInstanceId());

            // When process is ended, link should remain
            taskService.complete(taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult().getId());
            assertNull(runtimeService.createProcessInstanceQuery().processInstanceId(pi.getId()).singleResult());
            
            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            assertEquals(1, historyService.getHistoricIdentityLinksForProcessInstance(pi.getId()).size());

            // When process is deleted, identitylinks shouldn't exist anymore
            historyService.deleteHistoricProcessInstance(pi.getId());
            
            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
            
            assertEquals(0, historyService.getHistoricIdentityLinksForProcessInstance(pi.getId()).size());
        }
    }

    /**
     * Validation for ACT-821
     */
    @Test
    @Deployment(resources = { "org/flowable/engine/test/history/HistoricProcessInstanceTest.testDeleteHistoricProcessInstanceWithCallActivity.bpmn20.xml",
            "org/flowable/engine/test/history/HistoricProcessInstanceTest.testDeleteHistoricProcessInstanceWithCallActivity-subprocess.bpmn20.xml" })
    public void testDeleteHistoricProcessInstanceWithCallActivity() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            ProcessInstance pi = runtimeService.startProcessInstanceByKey("callSimpleSubProcess");

            runtimeService.deleteProcessInstance(pi.getId(), "testing");
            
            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            // The parent and child process should be present in history
            assertEquals(2L, historyService.createHistoricProcessInstanceQuery().count());

            // Deleting the parent process should cascade the child-process
            historyService.deleteHistoricProcessInstance(pi.getId());
            
            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
            
            assertEquals(0L, historyService.createHistoricProcessInstanceQuery().count());
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/history/oneTaskProcess.bpmn20.xml" })
    public void testHistoricProcessInstanceName() {
        String piName = "Customized Process Instance Name";
        ProcessInstanceBuilder builder = runtimeService.createProcessInstanceBuilder();
        builder.processDefinitionKey("oneTaskProcess");
        builder.name(piName);
        ProcessInstance processInstance1 = builder.start();
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance1.getProcessInstanceId()).singleResult();
        assertEquals(piName, historicProcessInstance.getName());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().processInstanceName(piName).list().size());
    }

    /**
     * Validation for https://jira.codehaus.org/browse/ACT-2182
     */
    @Test
    public void testNameAndTenantIdSetWhenFetchingVariables() {

        String tenantId = "testTenantId";
        String processInstanceName = "myProcessInstance";

        String deploymentId = repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/history/oneTaskProcess.bpmn20.xml").tenantId(tenantId).deploy().getId();

        Map<String, Object> vars = new HashMap<>();
        vars.put("name", "Kermit");
        vars.put("age", 60);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKeyAndTenantId("oneTaskProcess", vars, tenantId);
        runtimeService.setProcessInstanceName(processInstance.getId(), processInstanceName);

        // Verify name and tenant id (didn't work on mssql and db2) on process instance
        List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().includeProcessVariables().list();
        assertEquals(1, processInstances.size());
        processInstance = processInstances.get(0);

        assertEquals(processInstanceName, processInstance.getName());
        assertEquals(tenantId, processInstance.getTenantId());

        Map<String, Object> processInstanceVars = processInstance.getProcessVariables();
        assertEquals(2, processInstanceVars.size());
        assertEquals("Kermit", processInstanceVars.get("name"));
        assertEquals(60, processInstanceVars.get("age"));
        
        waitForHistoryJobExecutorToProcessAllJobs(10000, 200);

        // Verify name and tenant id (didn't work on mssql and db2) on historic process instance
        List<HistoricProcessInstance> historicProcessInstances = historyService.createHistoricProcessInstanceQuery().includeProcessVariables().list();
        assertEquals(1, historicProcessInstances.size());
        HistoricProcessInstance historicProcessInstance = historicProcessInstances.get(0);

        // Verify name and tenant id (didn't work on mssql and db2) on process instance
        assertEquals(processInstanceName, historicProcessInstance.getName());
        assertEquals(tenantId, historicProcessInstance.getTenantId());

        Map<String, Object> historicProcessInstanceVars = historicProcessInstance.getProcessVariables();
        assertEquals(2, historicProcessInstanceVars.size());
        assertEquals("Kermit", historicProcessInstanceVars.get("name"));
        assertEquals(60, historicProcessInstanceVars.get("age"));
        
        waitForHistoryJobExecutorToProcessAllJobs(10000, 200);

        // cleanup
        deleteDeployment(deploymentId);
    }

}
