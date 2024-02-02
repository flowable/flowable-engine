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
package org.flowable.spring.boot.cmmn;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Qualifier annotation for a CMMN beans that need to be injected for CMMN Configurations.
 * <p>
 * This can be used when one wants to provide a dedicated {@link org.springframework.core.task.TaskExecutor} or
 * {@link org.flowable.spring.job.service.SpringRejectedJobsHandler} for the Cmmn {@link org.flowable.spring.job.service.SpringAsyncExecutor}.
 *
 * <b>IMPORTANT:</b> When using this for the {@code TaskExecutor} or the {@code RejectedJobsHandler}, one needs to define a {@code @Primary} bean as well,
 * otherwise the Process Engine would use the one from the CMMN as well.
 *
 * @author Filip Hrisafov
 */
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE,
    ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Qualifier
public @interface Cmmn {

}
