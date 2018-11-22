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
package org.flowable.cmmn.test.parser;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.Collections;

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.parser.CmmnParseHandler;
import org.flowable.cmmn.engine.impl.parser.CmmnParseResult;
import org.flowable.cmmn.engine.impl.parser.CmmnParser;
import org.flowable.cmmn.engine.impl.parser.handler.HumanTaskParseHandler;
import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.HumanTask;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.test.impl.CustomCmmnConfigurationFlowableTestCase;
import org.flowable.task.api.Task;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class CmmnParseHandlerTest extends CustomCmmnConfigurationFlowableTestCase {

    @Override
    protected String getEngineName() {
        return this.getClass().getName();
    }

    @Override
    protected void configureConfiguration(CmmnEngineConfiguration cmmnEngineConfiguration) {
        cmmnEngineConfiguration.setPreCmmnParseHandlers(Collections.singletonList(new TestPreCmmnParseHandler()));
        cmmnEngineConfiguration.setPostCmmnParseHandlers(Collections.singletonList(new TestPostCmmnParseHandler()));
        cmmnEngineConfiguration.setCustomCmmnParseHandlers(Collections.singletonList(new TestCustomCmmnParseHandler()));
    }

    @Test
    public void testCmmnParseHandlersInvoked() {
        assertThat(TestPreCmmnParseHandler.invoked).isFalse();
        assertThat(TestPostCmmnParseHandler.invoked).isFalse();

        deployAndStartOneHumanTaskCaseModel();
        Task task = cmmnTaskService.createTaskQuery().singleResult();

        assertThat(task.getAssignee()).isEqualTo("fixedAssignee");
        assertThat(TestPreCmmnParseHandler.invoked).isTrue();
        assertThat(TestPostCmmnParseHandler.invoked).isTrue();
    }

    static class TestPreCmmnParseHandler implements CmmnParseHandler {

        static boolean invoked;

        @Override
        public Collection<Class<? extends BaseElement>> getHandledTypes() {
            return Collections.singletonList(HumanTask.class);
        }
        @Override
        public void parse(CmmnParser cmmnParser, CmmnParseResult cmmnParseResult, BaseElement element) {
            invoked = true;
        }
    }

    static class TestPostCmmnParseHandler implements CmmnParseHandler {

        static boolean invoked;

        @Override
        public Collection<Class<? extends BaseElement>> getHandledTypes() {
            return Collections.singletonList(HumanTask.class);
        }
        @Override
        public void parse(CmmnParser cmmnParser, CmmnParseResult cmmnParseResult, BaseElement element) {
            invoked = true;
        }
    }

    // Replaces the default HumanTaskParseHandlers and sets a fixed assignee
    static class TestCustomCmmnParseHandler extends HumanTaskParseHandler {
        @Override
        public void parse(CmmnParser cmmnParser, CmmnParseResult cmmnParseResult, BaseElement element) {
            super.parse(cmmnParser, cmmnParseResult, element);

            PlanItem planItem = (PlanItem) element;
            HumanTask humanTask = (HumanTask) planItem.getPlanItemDefinition();
            humanTask.setAssignee("fixedAssignee");
        }
    }
}
