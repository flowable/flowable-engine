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
package org.flowable.test.spring.boot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.IdentityService;
import org.flowable.idm.api.IdmIdentityService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import flowable.Application;

/**
 * @author Filip Hrisafov
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
@TestPropertySource(properties = "flowable.dbIdentityUsed=false")
public class BasicApplicationWithoutIdmOldPropertyTest {

    @Autowired(required = false)
    private IdmIdentityService idmIdentityService;

    @Autowired
    private IdentityService identityService;

    @Test
    public void idmShouldBeDisabled() {
        assertThat(idmIdentityService).as("Idm identity service").isNull();

        assertThatThrownBy(() -> identityService.newUser("filiphr"))
            .isInstanceOf(FlowableException.class)
            .hasMessage("Trying to use idm identity service when it is not initialized");
    }
}
