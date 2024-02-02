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
package org.flowable.external.job.rest.service;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.flowable.cmmn.spring.impl.test.FlowableCmmnSpringExtension;
import org.flowable.external.job.rest.conf.BpmnEngineTestConfiguration;
import org.flowable.external.job.rest.conf.CmmnEngineTestConfiguration;
import org.flowable.external.job.rest.conf.CmmnEngineWithBpmnConfiguration;
import org.flowable.external.job.rest.conf.ExternalJobRestTestApplication;
import org.flowable.spring.impl.test.FlowableSpringExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * A meta annotation over {@link SpringBootTest} which sets the main class under test {@link ExternalJobRestTestApplication}.
 * Configures the test to run under a random port and registers the rest template.
 *
 * @author Filip Hrisafov
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ExtendWith(SpringExtension.class)
@ExtendWith(FlowableSpringExtension.class)
@ExtendWith(FlowableCmmnSpringExtension.class)
@SpringBootTest(classes = ExternalJobRestTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebClient(registerRestTemplate = true)
@Import({
        BpmnEngineTestConfiguration.class,
        CmmnEngineTestConfiguration.class,
        CmmnEngineWithBpmnConfiguration.class
})
public @interface ExternalJobRestSpringBootTest {

}
