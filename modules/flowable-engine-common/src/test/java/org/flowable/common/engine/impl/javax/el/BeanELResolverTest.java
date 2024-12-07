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
package org.flowable.common.engine.impl.javax.el;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.flowable.common.engine.impl.el.FlowableElContext;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class BeanELResolverTest {
    
  @Test
  public void testInvokingGetOnArrayNode() {
    // This test checks if the BeanELResolver is able to invoke "get" on an ArrayNode, where a Long is passed as the index.
    FlowableElContext context = new FlowableElContext(null, null);
    
    ObjectMapper om = new ObjectMapper();
    ArrayNode base = om.createArrayNode();
    base.add("firstValue");
    base.add("secondValue");
    base.add("thirdValue");

    String method = "get";
    Class<?>[] paramTypes = null;
    // Note: the parameter is a Long, not an Integer. When a JUEL-expression like "${array.get(0) == \"firstValue\"}" is evaluated, the 0 is treated as a Long.
    Object[] params = {Long.valueOf(0L)}; 

    Object result = new BeanELResolver().invoke(context, base, method, paramTypes, params);

    assertEquals(((TextNode)result).asText(), "firstValue");
  }

}
