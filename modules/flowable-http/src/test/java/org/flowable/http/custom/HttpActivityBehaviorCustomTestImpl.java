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
package org.flowable.http.custom;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.http.HttpActivityBehavior;
import org.flowable.http.HttpRequest;
import org.flowable.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of HttpActivityBehavior to test custom behavior class
 *
 * @author Harsha Teja Kanna.
 */
public class HttpActivityBehaviorCustomTestImpl extends HttpActivityBehavior {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpActivityBehaviorCustomTestImpl.class);

    @Override
    protected HttpResponse perform(DelegateExecution execution, HttpRequest request) {
        return null;
    }
}
