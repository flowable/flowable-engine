/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.flowable.app.oauth;

import java.util.Collection;
import javax.servlet.http.HttpServletRequest;
import org.flowable.idm.api.OAuth2AuthorizationRequestor;
import org.flowable.idm.api.OAuth2Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 *
 * @author ahmedghonim
 */
@Controller
public class StartAuthorizationController {

    private static final Logger LOG = LoggerFactory.getLogger(StartAuthorizationController.class);

    @Autowired(required = false)
    protected Collection<OAuth2Configuration> oAuth2Configurations;

    @RequestMapping("/authorize")
    public String greeting(@RequestParam(value = "config", required = false) String config, Model model, HttpServletRequest request) {
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
        LOG.debug(String.format("Using OAuth configuration with ID '%1$s'!", config));
        OAuth2AuthorizationRequestor requestor = selectedConfig.getAuthorizationRequestor();
        String redirectUrl = requestor.buildAuthorizationURL(request, config);
        return "redirect:" + redirectUrl;
    }
}
