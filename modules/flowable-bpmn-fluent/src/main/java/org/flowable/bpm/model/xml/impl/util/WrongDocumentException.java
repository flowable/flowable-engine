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
package org.flowable.bpm.model.xml.impl.util;

import org.flowable.bpm.model.xml.ModelException;
import org.flowable.bpm.model.xml.instance.DomDocument;
import org.w3c.dom.Node;

/**
 * Thrown when a Model Element is added to the wrong document.
 */
public class WrongDocumentException
        extends ModelException {

    private static final long serialVersionUID = 1L;

    public WrongDocumentException(Node nodeToAdd, DomDocument targetDocument) {
        super("Cannot add attribute '" + nodeToAdd + "' to document '" + targetDocument + "' not created by document.");
    }

}
