package org.flowable.ui.idm.rest.app;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.flowable.ui.idm.model.SSOUserInfo;
import org.springframework.util.MultiValueMap;
import org.springframework.stereotype.Component;

@Component
public class EmptySSOHandler implements SSOHandler {

    @Override
    public boolean isActive() {
        return false;
    }
    @Override
    public SSOUserInfo handleSsoReturn(HttpServletRequest request, HttpServletResponse response, MultiValueMap<String, String> body) {
        return null;
    }
    @Override
    public String getExternalUrl(String idmUrl) {
        return null;
    }
}