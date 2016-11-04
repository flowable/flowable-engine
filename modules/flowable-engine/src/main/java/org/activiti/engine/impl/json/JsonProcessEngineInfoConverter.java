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
package org.activiti.engine.impl.json;

import java.io.Reader;

import org.activiti.engine.EngineInfo;
import org.activiti.engine.impl.util.json.JSONObject;

/**
 * @author Tom Baeyens
 */
public class JsonProcessEngineInfoConverter extends JsonObjectConverter<EngineInfo> {

  public JSONObject toJsonObject(EngineInfo processEngineInfo) {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("name", processEngineInfo.getName());
    jsonObject.put("resourceUrl", processEngineInfo.getResourceUrl());
    jsonObject.put("exception", processEngineInfo.getException());
    return jsonObject;
  }

  public EngineInfo toObject(Reader reader) {
    return null;
  }
}
