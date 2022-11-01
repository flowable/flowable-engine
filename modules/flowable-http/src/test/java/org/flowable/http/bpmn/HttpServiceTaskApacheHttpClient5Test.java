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
package org.flowable.http.bpmn;

import org.flowable.engine.test.ConfigurationResource;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * @author Filip Hrisafov
 */
@ConfigurationResource("flowableApacheHttpClient5.cfg.xml")
public class HttpServiceTaskApacheHttpClient5Test extends HttpServiceTaskTest {

    @Test
    @Deployment
    @Override
    // We override because we have a different BPMN XML
    public void testMapException() {
        super.testMapException();
    }

    @Test
    @Deployment
    @Override
    // We override because we have a different BPMN XML
    public void testHttpPost3XX() {
        super.testHttpPost3XX();
    }


    @Override
    protected String get500ResponseReason() {
        return HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase();
    }
}
