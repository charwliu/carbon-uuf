/*
 *  Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.carbon.uuf.core;

import com.google.gson.JsonObject;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.uuf.api.Placeholder;
import org.wso2.carbon.uuf.api.auth.Session;
import org.wso2.carbon.uuf.api.config.Bindings;
import org.wso2.carbon.uuf.api.config.Configuration;
import org.wso2.carbon.uuf.api.config.I18nResources;
import org.wso2.carbon.uuf.api.exception.RenderingException;
import org.wso2.carbon.uuf.api.model.MapModel;
import org.wso2.carbon.uuf.internal.exception.FragmentNotFoundException;
import org.wso2.carbon.uuf.internal.exception.HttpErrorException;
import org.wso2.carbon.uuf.internal.exception.PageNotFoundException;
import org.wso2.carbon.uuf.internal.exception.PageRedirectException;
import org.wso2.carbon.uuf.internal.exception.PluginExecutionException;
import org.wso2.carbon.uuf.internal.exception.SessionNotFoundException;
import org.wso2.carbon.uuf.internal.util.NameUtils;
import org.wso2.carbon.uuf.internal.util.UriUtils;
import org.wso2.carbon.uuf.spi.HttpRequest;
import org.wso2.carbon.uuf.spi.HttpResponse;
import org.wso2.carbon.uuf.spi.auth.Authorizer;
import org.wso2.carbon.uuf.spi.auth.SessionManager;
import org.wso2.carbon.uuf.spi.model.Model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.wso2.carbon.uuf.spi.HttpResponse.STATUS_INTERNAL_SERVER_ERROR;

public class App {

    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    private final String name;
    private final String contextPath;
    private final Lookup lookup;
    private final Map<String, Component> components;
    private final Component rootComponent;
    private final Map<String, Theme> themes;
    private final Theme defaultTheme;
    private final SessionManager sessionManager;
    private final Authorizer authorizer;
    private final Configuration configuration;

    public App(String name, String contextPath, Set<Component> components, Set<Theme> themes,
               Configuration configuration, Bindings bindings, I18nResources i18nResources,
               SessionManager sessionManager, Authorizer authorizer) {
        this.name = name;
        this.contextPath = contextPath;

        this.components = components.stream().collect(Collectors.toMap(Component::getContextPath, cmp -> cmp));
        this.rootComponent = this.components.get(Component.ROOT_COMPONENT_CONTEXT_PATH);

        this.themes = themes.stream().collect(Collectors.toMap(Theme::getName, theme -> theme));
        this.defaultTheme = configuration.getThemeName()
                .map(configuredThemeName -> {
                    Theme configuredTheme = App.this.themes.get(configuredThemeName);
                    if (configuredTheme == null) {
                        throw new IllegalArgumentException(
                                "Theme '" + configuredThemeName + "' which is configured for app '" + name +
                                        "' does not exists. Available themes: " + themes);
                    }
                    return configuredTheme;
                }).orElse(null);

        this.lookup = new Lookup(components, configuration, bindings, i18nResources);
        this.configuration = configuration;
        this.sessionManager = sessionManager;
        this.authorizer = authorizer;
    }

    public String getName() {
        return name;
    }

    public String getContextPath() {
        return contextPath;
    }

    public Map<String, Component> getComponents() {
        return components;
    }

    public Map<String, Fragment> getFragments() {
        return lookup.getAllFragments();
    }

    public Map<String, Layout> getLayouts() {
        return lookup.getAllLayouts();
    }

    public Map<String, Theme> getThemes() {
        return themes;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * Renders the relevant page for the given request.
     *
     * @param request  HTTP request
     * @param response HTTP response
     * @return HTML from page rendering
     * @throws PageRedirectException if a redirection for another page/URL is needed
     * @throws HttpErrorException    if some other HTTP error occurred
     */
    public String renderPage(HttpRequest request, HttpResponse response) {
        RequestLookup requestLookup = createRequestLookup(request, response);
        API api = new API(sessionManager, authorizer, requestLookup);
        Theme theme = getRenderingTheme(api);
        try {
            return renderPageUri(request.getUriWithoutContextPath(), null, requestLookup, api, theme);
        } catch (SessionNotFoundException e) {
            String loginPageUri = configuration.getLoginPageUri().orElseThrow(() -> e);
            // Redirect to the login page.
            throw new PageRedirectException(requestLookup.getContextPath() + loginPageUri, e);
        } catch (PageRedirectException e) {
            throw e;
        } catch (PageNotFoundException e) {
            // See https://googlewebmastercentral.blogspot.com/2010/04/to-slash-or-not-to-slash.html
            // If the tailing '/' is extra or a it is missing, then send 301 with corrected URL.
            String uriWithoutContextPath = request.getUriWithoutContextPath();
            String correctedUriWithoutContextPath = uriWithoutContextPath.endsWith("/") ?
                    uriWithoutContextPath.substring(0, uriWithoutContextPath.length() - 1) :
                    (uriWithoutContextPath + "/");
            if (hasPage(correctedUriWithoutContextPath)) {
                if (request.isGetRequest()) {
                    // Redirecting to the correct page.
                    String correctedUri = request.getContextPath() + correctedUriWithoutContextPath;
                    if (request.getQueryString() != null) {
                        correctedUri = correctedUri + '?' + request.getQueryString();
                    }
                    throw new PageRedirectException(correctedUri, e);
                } else {
                    // If GET, we correct, since this can be an end-user error. But if POST it's the responsibility of
                    // the dev to use correct URL. Because HTTP POST redirect is not well supported.
                    // See : https://softwareengineering.stackexchange.com/q/99894
                    String message = e.getMessage() + " Retry with correct URI ending " + correctedUriWithoutContextPath;
                    return renderErrorPage(new PageNotFoundException(message, e), request, response, theme);
                }
            } else {
                return renderErrorPage(e, request, response, theme);
            }
        } catch (HttpErrorException e) {
            return renderErrorPage(e, request, response, theme);
        } catch (PluginExecutionException e) {
            LOGGER.error("An error occurred while executing a plugin.", e);
            return renderErrorPage(new HttpErrorException(STATUS_INTERNAL_SERVER_ERROR, e.getMessage(), e),
                                   request, response, theme);
        } catch (RenderingException e) {
            LOGGER.error("An error occurred while rendering page for request '{}'.", request, e);
            String message = (e.getCause() != null) ? ExceptionUtils.getRootCause(e).getMessage() : e.getMessage();
            return renderErrorPage(new HttpErrorException(STATUS_INTERNAL_SERVER_ERROR, message, e), request, response,
                                   theme);
        }
    }

    private String renderErrorPage(HttpErrorException e, HttpRequest request, HttpResponse response, Theme theme) {
        String errorPageUri = configuration.getErrorPageUri(e.getHttpStatusCode())
                .orElse(configuration.getDefaultErrorPageUri().orElseThrow(() -> e));

        // Create Model with HTTP status code and error message.
        Map<String, Object> modelMap = new HashMap<>(2);
        modelMap.put("status", e.getHttpStatusCode());
        modelMap.put("message", e.getMessage());
        MapModel model = new MapModel(modelMap);

        RequestLookup requestLookup = createRequestLookup(request, response);
        API api = new API(sessionManager, authorizer, requestLookup);

        return renderPageUri(errorPageUri, model, requestLookup, api, theme);
    }

    private String renderPageUri(String pageUri, Model model, RequestLookup requestLookup, API api, Theme theme) {
        // If theme exists, add theme values to the requestLookup
        if (theme != null) {
            theme.addPlaceHolderValues(requestLookup);
        }
        // First try to addPlaceHolderValues the page with 'root' component.
        Optional<String> output = rootComponent.renderPage(pageUri, model, lookup, requestLookup, api);
        if (output.isPresent()) {
            return output.get();
        }

        // Since 'root' component doesn't have the page, try with other components.
        int secondSlashIndex = pageUri.indexOf('/', 1);
        if (secondSlashIndex == -1) {
            // No component context found in the 'pageUri' URI.
            throw new PageNotFoundException("Requested page '" + pageUri + "' does not exists.");
        }
        String componentContext = pageUri.substring(0, secondSlashIndex);
        Component component = components.get(componentContext);
        if (component == null) {
            // No component found for the 'componentContext' key.
            throw new PageNotFoundException("Requested page '" + pageUri + "' does not exists.");
        }
        String uriWithoutComponentContext = pageUri.substring(secondSlashIndex);
        output = component.renderPage(uriWithoutComponentContext, model, lookup, requestLookup, api);
        if (output.isPresent()) {
            return output.get();
        }
        // No page found for 'uriWithoutComponentContext' in the 'component'.
        throw new PageNotFoundException("Requested page '" + pageUri + "' does not exists.");
    }

    /**
     * @param request  HTTP request
     * @param response HTTP response
     * @return rendered HTML,CSS and JS outputs as JSON
     */
    public JsonObject renderFragment(HttpRequest request, HttpResponse response) {
        String uriWithoutContextPath = request.getUriWithoutContextPath();
        String uriPart = uriWithoutContextPath.substring(UriUtils.FRAGMENTS_URI_PREFIX.length());
        String fragmentName = NameUtils.getFullyQualifiedName(rootComponent.getName(), uriPart);
        // When building the dependency tree, all fragments are accumulated into the rootComponent.
        Fragment fragment = lookup.getAllFragments().get(fragmentName);
        if (fragment == null) {
            throw new FragmentNotFoundException("Requested fragment '" + fragmentName + "' does not exists.");
        }

        Model model = new MapModel(request.getFormParams());
        RequestLookup requestLookup = createRequestLookup(request, response);
        API api = new API(sessionManager, authorizer, requestLookup);

        JsonObject output = new JsonObject();
        output.addProperty("html", fragment.render(model, lookup, requestLookup, api));
        output.addProperty(Placeholder.headJs.name(),
                           requestLookup.getPlaceholderContent(Placeholder.headJs).orElse(null));
        output.addProperty(Placeholder.js.name(), requestLookup.getPlaceholderContent(Placeholder.js).orElse(null));
        output.addProperty(Placeholder.css.name(), requestLookup.getPlaceholderContent(Placeholder.css).orElse(null));
        return output;
    }

    private boolean hasPage(String uriWithoutContextPath) {
        // First check 'root' component has the page.
        if (rootComponent.hasPage(uriWithoutContextPath)) {
            return true;
        }

        // Since 'root' components doesn't have the page, search in other components.
        int secondSlashIndex = uriWithoutContextPath.indexOf('/', 1);
        if (secondSlashIndex == -1) {
            // No component context found in the 'uriWithoutContextPath' URI.
            return false;
        }
        String componentContext = uriWithoutContextPath.substring(0, secondSlashIndex);
        Component component = components.get(componentContext);
        if (component == null) {
            // No component found for the 'componentContext' key.
            return false;
        }
        String pageUri = uriWithoutContextPath.substring(secondSlashIndex);
        return component.hasPage(pageUri);
    }

    private Theme getRenderingTheme(API api) {
        Optional<String> sessionThemeName = api.getSession().map(Session::getThemeName);
        if (!sessionThemeName.isPresent()) {
            return defaultTheme;
        }
        return sessionThemeName
                .map(themes::get)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Theme '" + sessionThemeName.get() + "' which is set as for the current session of app '" +
                                name + "' does not exists. Available themes are " + themes.keySet() + "."));
    }

    private RequestLookup createRequestLookup(HttpRequest request, HttpResponse response) {
        return new RequestLookup((configuration.getContextPath().orElse(null)), request, response);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, contextPath);
    }

    @Override
    public boolean equals(Object obj) {
        return (obj != null) && (obj instanceof App) && (this.name.equals(((App) obj).name));
    }

    @Override
    public String toString() {
        return "{\"name\": \"" + name + "\", \"context\": \"" + contextPath + "\"}";
    }
}
