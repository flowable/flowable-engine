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
package org.flowable.http.common.api.client;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.http.common.api.HttpResponse;

/**
 * @author Filip Hrisafov
 */
public interface AsyncExecutableHttpRequest extends ExecutableHttpRequest {

    @Override
    default HttpResponse call() {
        try {
            return callAsync().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FlowableException("Call was interrupted", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else {
                throw new FlowableException("execution exception", cause);
            }
        }
    }

    CompletableFuture<HttpResponse> callAsync();
}
