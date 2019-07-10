package org.flowable.bpmn.converter.export;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.converter.util.BpmnXMLUtil;
import org.flowable.bpmn.model.MapExceptionEntry;

import javax.xml.stream.XMLStreamWriter;
import java.util.List;

public class MapExceptionExport implements BpmnXMLConstants {

    public static boolean writeMapExceptionExtensions(List<MapExceptionEntry> mapExceptionList, boolean didWriteExtensionStartElement, XMLStreamWriter xtw) throws Exception {

        for (MapExceptionEntry mapException : mapExceptionList) {

            if (!didWriteExtensionStartElement) {
                xtw.writeStartElement(ELEMENT_EXTENSIONS);
                didWriteExtensionStartElement = true;
            }

            if (StringUtils.isNotEmpty(mapException.getErrorCode())) {
                xtw.writeStartElement(FLOWABLE_EXTENSIONS_PREFIX, MAP_EXCEPTION, FLOWABLE_EXTENSIONS_NAMESPACE);

                BpmnXMLUtil.writeDefaultAttribute(MAP_EXCEPTION_ERRORCODE, mapException.getErrorCode(), xtw);
                BpmnXMLUtil.writeDefaultAttribute(MAP_EXCEPTION_ANDCHILDREN, Boolean.toString(mapException.isAndChildren()), xtw);

                if (StringUtils.isNotEmpty(mapException.getClassName())) {
                    xtw.writeCData(mapException.getClassName());
                }
                xtw.writeEndElement(); //end MAP_EXCEPTION
            }
        }
        return didWriteExtensionStartElement;
    }

}
