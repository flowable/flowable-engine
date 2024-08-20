package org.flowable.bpmn.converter;

public class BpmnXMLConverterOptions {

    boolean saveElementNameWithNewLineInExtensionElement = false;

    public boolean getSaveElementNameWithNewLineInExtensionElement() {
        return saveElementNameWithNewLineInExtensionElement;
    }

    public void setSaveElementNameWithNewLineInExtensionElement(boolean saveElementNameWithNewLineInExtensionElement) {
        this.saveElementNameWithNewLineInExtensionElement = saveElementNameWithNewLineInExtensionElement;
    }
}
