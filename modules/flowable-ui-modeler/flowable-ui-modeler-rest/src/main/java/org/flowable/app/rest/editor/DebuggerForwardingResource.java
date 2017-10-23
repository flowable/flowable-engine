package org.flowable.app.rest.editor;

import com.google.common.base.Function;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import static org.flowable.app.rest.HttpRequestHelper.executeHttpGet;
import static org.flowable.app.rest.HttpRequestHelper.executePostRequest;

/**
 * Forwards debugger requests to the engine where execution is performed
 *
 * @author martin.grofcik
 */
@RestController
public class DebuggerForwardingResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(DebuggerForwardingResource.class);

    @Autowired
    protected Environment environment;

    @RequestMapping(value = "/rest/debugger/breakpoints", method = RequestMethod.GET, produces = "application/json")
    public void getBreakpoints(final HttpServletResponse response) {
        String basicAuthUser = environment.getRequiredProperty("idm.admin.user");
        String basicAuthPassword = environment.getRequiredProperty("idm.admin.password");
        String breakpointUrl = getBreakpointUrl().concat("breakpoints?version=" + System.currentTimeMillis());
        executeHttpGet(breakpointUrl, basicAuthUser, basicAuthPassword,
                new Function<HttpResponse, Void>() {
                    @Override
                    public Void apply(HttpResponse forwardedResponse) {
                        try {
                            IOUtils.copy(forwardedResponse.getEntity().getContent(), response.getOutputStream());
                            return null;
                        } catch (IOException e) {
                            LOGGER.error("Error in reading response content", e);
                            throw new RuntimeException(e);
                        }
                    }
                });
    }

    @RequestMapping(value = "/rest/debugger/breakpoints", method = RequestMethod.POST)
    public void addBreakpoint(@RequestBody String breakpointRepresentation) throws UnsupportedEncodingException {
        forwardPostRequest(breakpointRepresentation, "breakpoints?version=" + System.currentTimeMillis());
    }

    @RequestMapping(value = "/rest/debugger/breakpoints/delete", method = RequestMethod.POST)
    public void deleteBreakpoint(@RequestBody String breakpointRepresentation) throws UnsupportedEncodingException {
        forwardPostRequest(breakpointRepresentation, "breakpoints/delete?version=" + System.currentTimeMillis());
    }

    protected void forwardPostRequest(@RequestBody String breakpointRepresentation, String url) throws UnsupportedEncodingException {
        String basicAuthUser = environment.getRequiredProperty("idm.admin.user");
        String basicAuthPassword = environment.getRequiredProperty("idm.admin.password");
        String breakpointUrl = getBreakpointUrl().concat(url);

        executePostRequest(breakpointUrl, basicAuthUser, basicAuthPassword, new StringEntity(breakpointRepresentation, ContentType.APPLICATION_JSON),
                200);
    }

    protected String getBreakpointUrl() {
        String breakpointUrl = environment.getRequiredProperty("debugger.api.url");

        if (!breakpointUrl.endsWith("/")) {
            breakpointUrl = breakpointUrl.concat("/");
        }
        return breakpointUrl;
    }

}
