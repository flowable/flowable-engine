/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.flowable.app.oauth2;

import java.util.Collection;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.flowable.idm.spring.authentication.OAuth2AuthorizationHandler;
import org.flowable.idm.spring.authentication.OAuth2AuthorizationRequestor;
import org.flowable.idm.spring.authentication.OAuth2Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 *
 * @author ahmedghonim
 */
@Controller
public class OAuth2Controller {

    private static final Logger LOG = LoggerFactory.getLogger(OAuth2Controller.class);

    @Autowired(required = false)
    protected Collection<OAuth2Configuration> oAuth2Configurations;

    @Autowired
    protected RememberMeServices customPersistentRememberMeServices;

    @RequestMapping("/authorize")
    public String authorize(@RequestParam(value = "config", required = false) String config, Model model,
            HttpServletRequest request) {
        if (oAuth2Configurations == null) {
            LOG.error("No OAuth2 Configurations found on Classpath!");
            throw new RuntimeException("No OAuth2 Configurations found on Classpath!");
        }
        OAuth2Configuration selectedConfig = null;
        for (OAuth2Configuration c : oAuth2Configurations) {
            if (config == null || config.isEmpty()) {
                LOG.debug("No specific configuration requested. Defaulting to the first one!");
                selectedConfig = c;
                break;
            }
            if (c.getId().equals(config)) {
                selectedConfig = c;
                break;
            }
        }
        if (selectedConfig == null) {
            String e = String.format("Did not find default OAuth configuration or one matching name '%1$s'!", config);
            LOG.error(e);
            throw new RuntimeException(e);
        }
        LOG.debug(String.format("Using OAuth configuration with ID '%1$s'!", selectedConfig.getId()));
        OAuth2AuthorizationRequestor requestor = selectedConfig.getAuthorizationRequestor();
        String redirectUrl = requestor.buildAuthorizationURL(request, config);
        return "redirect:" + redirectUrl;
    }

    @RequestMapping("/handle")
    public String handle(HttpServletRequest request, @RequestParam(value = "state",
            required = false) String state,
            HttpServletResponse response) {
        if (oAuth2Configurations == null) {
            LOG.error("No OAuth2 Configurations found on Classpath!");
            throw new RuntimeException("No OAuth2 Configurations found on Classpath!");
        }
        OAuth2Configuration selectedConfig = null;
        for (OAuth2Configuration c : oAuth2Configurations) {
            if (c.getAuthorizationHandler().canHandle(request, state)) {
                selectedConfig = c;
                break;
            }
        }
        if (selectedConfig == null) {
            String e = String.format("Did not find OAuth configuration that can handle the state!");
            LOG.error(e);
            throw new RuntimeException(e);
        }
        LOG.debug(String.format("Using OAuth configuration with ID '%1$s'!", selectedConfig.getId()));
        OAuth2AuthorizationHandler handler = selectedConfig.getAuthorizationHandler();
        Authentication auth = handler.handle(request, state);
        if (auth != null) {
            customPersistentRememberMeServices.loginSuccess(request, response, auth);
            return handler.successRedirectUrl(request, state);
        }
        return "redirect:/";
    }
}
