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
package org.activiti.rest.exception;

import org.activiti.engine.ActivitiTaskAlreadyClaimedException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Process engine specific exception handler advice base class
 *
 * @author Yvo Swillens
 */
public class ProcessExceptionHandlerAdvice extends BaseExceptionHandlerAdvice {

    @ResponseStatus(HttpStatus.CONFLICT)
    // 409
    @ExceptionHandler(ActivitiTaskAlreadyClaimedException.class)
    @ResponseBody
    public ErrorInfo handleTaskAlreadyClaimed(ActivitiTaskAlreadyClaimedException e) {
        return new ErrorInfo("Task was already claimed", e);
    }
}
