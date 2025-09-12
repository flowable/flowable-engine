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

package org.flowable.cmmn.test.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstanceState;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.interceptor.EndCaseInstanceInterceptor;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.test.FlowableCmmnTestCase;
import org.junit.jupiter.api.Test;

/**
 * @author Christopher Welsch
 */
public class CaseInstanceEndInterceptorTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneHumanTaskCase.cmmn")
    public void testEndProcessInterceptorIsCalled() {
        try {
            TestEndCaseInstanceInterceptor testEndCaseInstanceInterceptor = new TestEndCaseInstanceInterceptor();
            cmmnEngineConfiguration.setEndCaseInstanceInterceptor(testEndCaseInstanceInterceptor);

            cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
            cmmnTaskService.complete(cmmnTaskService.createTaskQuery().singleResult().getId());
            assertThat(testEndCaseInstanceInterceptor.beforeIsCalled).isTrue();
            assertThat(testEndCaseInstanceInterceptor.beforeState).isEqualTo(CaseInstanceState.COMPLETED);

            assertThat(testEndCaseInstanceInterceptor.afterIsCalled).isTrue();
            assertThat(testEndCaseInstanceInterceptor.afterState).isEqualTo(CaseInstanceState.COMPLETED);
        } finally {
            cmmnEngineConfiguration.setEndCaseInstanceInterceptor(null);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneHumanTaskCase.cmmn")
    public void testEndProcessInterceptorIsNotCalledForTermination() {
        try {
            TestEndCaseInstanceInterceptor testEndCaseInstanceInterceptor = new TestEndCaseInstanceInterceptor();
            cmmnEngineConfiguration.setEndCaseInstanceInterceptor(testEndCaseInstanceInterceptor);
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
            cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());
            assertThat(testEndCaseInstanceInterceptor.beforeIsCalled).isTrue();
            assertThat(testEndCaseInstanceInterceptor.beforeState).isEqualTo(CaseInstanceState.TERMINATED);

            assertThat(testEndCaseInstanceInterceptor.afterIsCalled).isTrue();
            assertThat(testEndCaseInstanceInterceptor.afterState).isEqualTo(CaseInstanceState.TERMINATED);
        } finally {
            cmmnEngineConfiguration.setEndCaseInstanceInterceptor(null);
        }
    }

    public static class TestEndCaseInstanceInterceptor implements EndCaseInstanceInterceptor {

        protected boolean beforeIsCalled = false;
        protected String beforeState = null;

        protected boolean afterIsCalled = false;
        protected String afterState = null;

        @Override
        public void beforeEndCaseInstance(CaseInstanceEntity caseInstance, String endingState) {
            beforeIsCalled = true;
            beforeState = endingState;
        }

        @Override
        public void afterEndCaseInstance(String caseInstanceId, String endingState) {
            afterIsCalled = true;
            afterState = endingState;
        }
    }
}
