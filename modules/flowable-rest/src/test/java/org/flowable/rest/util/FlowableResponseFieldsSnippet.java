package org.flowable.rest.util;

import java.lang.reflect.Type;
import java.util.Map;

import org.flowable.rest.api.DataResponse;
import org.springframework.web.method.HandlerMethod;

import capital.scalable.restdocs.payload.JacksonResponseFieldSnippet;

/**
 * @author Filip Hrisafov
 */
public class FlowableResponseFieldsSnippet extends JacksonResponseFieldSnippet {

    @Override
    protected Type getType(HandlerMethod method) {
        Type type = super.getType(method);
        //TODO this will actually not work if the return type of the method was ResponseEntity, or HttpEntity
        if (type != null && type.equals(DataResponse.class)) {
            type = firstGenericType(method.getReturnType());
        }
        return type;
    }

    @Override
    protected void enrichModel(Map<String, Object> model, HandlerMethod handlerMethod) {
        super.enrichModel(model, handlerMethod);
        model.put("isPageResponse", isPageResponse(handlerMethod));
    }

    private boolean isPageResponse(HandlerMethod handlerMethod) {
        Type type = super.getType(handlerMethod);
        return type instanceof Class && DataResponse.class.isAssignableFrom((Class<?>) type);
    }
}
