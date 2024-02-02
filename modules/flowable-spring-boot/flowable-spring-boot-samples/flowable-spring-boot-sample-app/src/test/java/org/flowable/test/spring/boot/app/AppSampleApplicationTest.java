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
package org.flowable.test.spring.boot.app;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.app.api.AppRepositoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import flowable.AppSampleApplication;

/**
 * @author Filip Hrisafov
 */
@SpringBootTest(classes = AppSampleApplication.class)
public class AppSampleApplicationTest {

    @Autowired
    private AppRepositoryService repositoryService;

    @Test
    public void queryAppDefinitions() {
        assertThat(repositoryService).as("App repository service").isNotNull();
        assertThat(repositoryService.createAppDefinitionQuery().count())
            .as("All app definitions")
            .isZero();
    }
}
