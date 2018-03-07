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
package org.flowable.test.spring.boot.cmmn;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import flowable.CmmnSampleApplication;

/**
 * @author Filip Hrisafov
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = CmmnSampleApplication.class)
public class CmmnSampleApplicationTest {

    @Autowired
    private CmmnRuntimeService runtimeService;

    @Test
    public void contextLoads() {

        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("case1").start();

        assertThat(caseInstance).as("Case instance").isNotNull();

        List<CaseInstance> caseInstances = runtimeService.createCaseInstanceQuery().list();

        assertThat(caseInstances)
            .extracting(CaseInstance::getId)
            .containsExactly(caseInstance.getId());
    }
}
