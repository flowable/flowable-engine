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
package org.flowable.app.security;

/**
 * @author Joram Barrez
 */
public interface DefaultPrivileges {

    String ACCESS_IDM = "access-idm";

    String ACCESS_MODELER = "access-modeler";

    String ACCESS_ADMIN = "access-admin";

    String ACCESS_TASK = "access-task";
    
    String ACCESS_REST_API = "access-rest-api";

}
