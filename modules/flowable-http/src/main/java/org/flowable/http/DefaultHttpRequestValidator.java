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
package org.flowable.http;

import static org.flowable.http.HttpActivityExecutor.HTTP_TASK_REQUEST_METHOD_INVALID;
import static org.flowable.http.HttpActivityExecutor.HTTP_TASK_REQUEST_METHOD_REQUIRED;
import static org.flowable.http.HttpActivityExecutor.HTTP_TASK_REQUEST_URL_REQUIRED;

import org.flowable.common.engine.api.FlowableException;

/**
 * @author Filip Hrisafov
 */
public class DefaultHttpRequestValidator implements HttpRequestValidator {

    @Override
    public void validateRequest(HttpRequest request) {
        if (request.getMethod() == null) {
            throw new FlowableException(HTTP_TASK_REQUEST_METHOD_REQUIRED);
        }

        switch (request.getMethod()) {
            case "GET":
            case "POST":
            case "PUT":
            case "DELETE":
                break;
            default:
                throw new FlowableException(HTTP_TASK_REQUEST_METHOD_INVALID);
        }

        if (request.getUrl() == null) {
            throw new FlowableException(HTTP_TASK_REQUEST_URL_REQUIRED);
        }
    }
}
