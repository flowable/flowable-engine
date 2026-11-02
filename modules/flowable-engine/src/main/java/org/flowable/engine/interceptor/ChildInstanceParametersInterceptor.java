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
package org.flowable.engine.interceptor;

/**
 * Called where a call activity or case task passes variables to its child instance and back: after the in parameters
 * were evaluated into the variables the child process or case instance starts with, and after the out parameters were
 * applied when the child completes. This makes it possible to add to or change what the in and out parameters do, for
 * every call activity and case task, without replacing their behavior.
 */
public interface ChildInstanceParametersInterceptor {

    /**
     * Called after the variables of the parent were inherited, when the task does so, and the in parameters were
     * evaluated: what this adds to the start variables of the context takes precedence.
     */
    void afterInParameters(ChildInstanceInParametersContext context);

    /**
     * Called after the out parameters were applied to the parent, with the completed child instance.
     */
    void afterOutParameters(ChildInstanceOutParametersContext context);

}
