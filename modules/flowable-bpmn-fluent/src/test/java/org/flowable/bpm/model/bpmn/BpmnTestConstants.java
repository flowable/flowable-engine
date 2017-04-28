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
package org.flowable.bpm.model.bpmn;

import java.util.Arrays;
import java.util.List;

public final class BpmnTestConstants {

    public static final String COLLABORATION_ID = "collaboration";
    public static final String PARTICIPANT_ID = "participant";
    public static final String PROCESS_ID = "process";
    public static final String START_EVENT_ID = "startEvent";
    public static final String TASK_ID = "task";
    public static final String USER_TASK_ID = "userTask";
    public static final String SERVICE_TASK_ID = "serviceTask";
    public static final String SEND_TASK_ID = "sendTask";
    public static final String SCRIPT_TASK_ID = "scriptTask";
    public static final String SEQUENCE_FLOW_ID = "sequenceFlow";
    public static final String MESSAGE_FLOW_ID = "messageFlow";
    public static final String DATA_INPUT_ASSOCIATION_ID = "dataInputAssociation";
    public static final String ASSOCIATION_ID = "association";
    public static final String CALL_ACTIVITY_ID = "callActivity";
    public static final String BUSINESS_RULE_TASK = "businessRuleTask";
    public static final String END_EVENT_ID = "endEvent";
    public static final String EXCLUSIVE_GATEWAY = "exclusiveGateway";
    public static final String SUB_PROCESS_ID = "subProcess";
    public static final String TRANSACTION_ID = "transaction";
    public static final String CONDITION_ID = "condition";
    public static final String BOUNDARY_ID = "boundary";
    public static final String CATCH_ID = "catch";

    public static final String TEST_STRING_XML = "test";
    public static final String TEST_STRING_API = "api";
    public static final String TEST_CLASS_XML = "org.flowable.test.Test";
    public static final String TEST_CLASS_API = "org.flowable.test.Api";
    public static final String TEST_EXPRESSION_XML = "${" + TEST_STRING_XML + '}';
    public static final String TEST_EXPRESSION_API = "${" + TEST_STRING_API + '}';
    public static final String TEST_DELEGATE_EXPRESSION_XML = "${" + TEST_CLASS_XML + '}';
    public static final String TEST_DELEGATE_EXPRESSION_API = "${" + TEST_CLASS_API + '}';
    public static final String TEST_GROUPS_XML = "group1, ${group2(a, b)}, group3";
    public static final List<String> TEST_GROUPS_LIST_XML = Arrays.asList("group1", "${group2(a, b)}", "group3");
    public static final String TEST_GROUPS_API = "#{group1( c,d)}, group5";
    public static final List<String> TEST_GROUPS_LIST_API = Arrays.asList("#{group1( c,d)}", "group5");
    public static final String TEST_USERS_XML = "user1, ${user2(a, b)}, user3";
    public static final List<String> TEST_USERS_LIST_XML = Arrays.asList("user1", "${user2(a, b)}", "user3");
    public static final String TEST_USERS_API = "#{user1( c,d)}, user5";
    public static final List<String> TEST_USERS_LIST_API = Arrays.asList("#{user1( c,d)}", "user5");
    public static final String TEST_DUE_DATE_XML = "2014-02-27";
    public static final String TEST_DUE_DATE_API = "2015-03-28";
    public static final String TEST_PRIORITY_XML = "12";
    public static final String TEST_PRIORITY_API = "${dateVariable}";
    public static final String TEST_TYPE_XML = "mail";
    public static final String TEST_TYPE_API = "shell";
    public static final String TEST_EXECUTION_EVENT_XML = "start";
    public static final String TEST_EXECUTION_EVENT_API = "end";
    public static final String TEST_TASK_EVENT_XML = "create";
    public static final String TEST_TASK_EVENT_API = "complete";

    public static final String TEST_CONDITION = "${true}";

    private BpmnTestConstants() {}
}
