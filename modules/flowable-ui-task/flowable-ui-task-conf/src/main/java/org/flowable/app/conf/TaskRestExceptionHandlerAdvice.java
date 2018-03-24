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
package org.flowable.app.conf;

import org.flowable.common.rest.exception.BaseExceptionHandlerAdvice;
import org.flowable.common.rest.exception.ErrorInfo;
import org.flowable.engine.FlowableTaskAlreadyClaimedException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Yvo Swillens
 */
@ControllerAdvice
public class TaskRestExceptionHandlerAdvice extends BaseExceptionHandlerAdvice {

    @ResponseStatus(HttpStatus.CONFLICT) // 409
    @ExceptionHandler(FlowableTaskAlreadyClaimedException.class)
    @ResponseBody
    public ErrorInfo handleTaskAlreadyClaimed(FlowableTaskAlreadyClaimedException e) {
        return new ErrorInfo("Task was already claimed", e);
    }
}