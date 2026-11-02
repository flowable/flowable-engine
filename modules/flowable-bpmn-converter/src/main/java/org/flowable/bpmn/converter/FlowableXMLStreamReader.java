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
package org.flowable.bpmn.converter;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.util.StreamReaderDelegate;

/**
 * @author Filip Hrisafov
 */
public class FlowableXMLStreamReader extends StreamReaderDelegate {

    public FlowableXMLStreamReader(XMLStreamReader reader) {
        super(reader);
    }

    @Override
    public int getTextCharacters(int sourceStart, char[] target, int targetStart, int length) throws XMLStreamException {
        try {
            return super.getTextCharacters(sourceStart, target, targetStart, length);
        } catch (IndexOutOfBoundsException e) {
            if (length == 0) {
                // The default java stream reader has a bug where an empty CDATA will throw an IndexOutOfBoundsException
                // When the length to copy is 0, then we should not copy anything and just return 0
                return 0;
            }
            throw e;
        }
    }
}
