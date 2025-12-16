 <!DOCTYPE html>

<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page language="java" %>
<%@ page import="org.joda.time.DateTime" %>
<%@ page import="org.nuxeo.ecm.core.api.CoreInstance" %>
<%@ page import="org.nuxeo.ecm.core.api.CoreSession" %>
<%@ page import="org.nuxeo.ecm.core.api.DocumentModel" %>
<%@ page import="org.nuxeo.ecm.core.api.repository.Repository" %>
<%@ page import="org.nuxeo.ecm.core.api.repository.RepositoryManager" %>
<%@ page import="org.nuxeo.ecm.platform.ui.web.auth.LoginScreenHelper" %>
<%@ page import="org.nuxeo.ecm.platform.ui.web.auth.service.LoginScreenConfig" %>
<%@ page import="org.nuxeo.ecm.platform.ui.web.auth.oauth2.NuxeoOAuth2Filter" %>
<%@ page import="org.nuxeo.ecm.user.center.profile.UserProfileConstants" %>
<%@ page import="org.nuxeo.ecm.user.center.profile.UserProfileService" %>
<%@ page import="org.nuxeo.runtime.api.Framework" %>
<%@ page import="java.util.Locale" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%
    String productName = Framework.getProperty("org.nuxeo.ecm.product.name");
    String productVersion = Framework.getProperty("org.nuxeo.ecm.product.version");
    String context = request.getContextPath();

    LoginScreenConfig screenConfig = LoginScreenHelper.getConfig();
    String logoWidth = LoginScreenHelper.getValueWithDefault(screenConfig.getLogoWidth(), "113");
    String logoHeight = LoginScreenHelper.getValueWithDefault(screenConfig.getLogoHeight(), "20");
    String logoAlt = LoginScreenHelper.getValueWithDefault(screenConfig.getLogoAlt(), "Nuxeo");
    String logoUrl = LoginScreenHelper.getValueWithDefault(screenConfig.getLogoUrl(), context + "/img/nuxeo_logo.png");
    String currentYear = new DateTime().toString("Y");
    
    String headerStyle = LoginScreenHelper.getValueWithDefault(screenConfig.getHeaderStyle(), "background-color: rgba(255,255,255,0.7);");
    String footerStyle = LoginScreenHelper.getValueWithDefault(screenConfig.getFooterStyle(), "");
%>
<html>
    <head>
        <title><%= productName %></title>
        
        <meta http-equiv="x-ua-compatible" content="ie=edge" />
        
        <link rel="icon" type="image/svg+xml" href="<%=context%>/icons/favicon.svg" />
        <link rel="icon" type="image/png" href="<%=context%>/icons/favicon.png" />
        <link rel="shortcut icon" type="image/x-icon" href="<%=context%>/icons/favicon.ico" />
        
        <style type="text/css">
            <!--
            html {
                height: 100%;
            }
            
            body {
                height: 100%;
                margin: 0;
                font: normal 11px "Lucida Grande", sans-serif;
                color: #343434;
            }
            
            #main {
                display: flex;
                flex-direction: column;
                align-items: stretch;
                height: 100%;
            }
            
            #topBar {
                flex-grow: 0;
                background: none #212325;
                width: 100%;
                height: 60px;
                <%= headerStyle %>;
            }
            
            #topBar img {
                margin-left: 55px;
                margin-top: 10px;
            }
            
            h1 {
                color: #343434;
                font: bold 14px "Lucida Grande", sans-serif;
                padding: 0;
                margin: 2px 0 15px 0;
                border-bottom: 1px dotted #8B8B8B;
            }
            
            h2 {
                color: #999;
                font: bold 13px "Lucida Grande", sans-serif;
                padding: 0;
                margin: 0 0 0 0;
            }
            
            #content {
                flex-grow: 1;
                display: flex;
                align-items: center;
            }
            
            #content form {
                width: 100%;
                text-align: center;
            }
            
            
            #content form input[type="button"],
            #content form input[type="submit"] {
              padding: 0.2em 1em;
              margin: 0.2em 1em;
              min-width: 6em;
              font-weight: bold;
              border-radius: 0;
              border: 1px solid #053832;
            }
            
            #content form input[type="button"]:hover,
            #content form input[type="submit"]:hover {
              cursor: pointer;
              box-shadow: 0 0 3px #8cd600;
            }
            
            #content form input[type="button"] {
              color: #053832;
              background-color: #ececec;
            }
            
            #content form input[type="submit"] {
              background-color: #053832;
              color: #fff;
              text-transform: uppercase;
            }
            
            #footer {
                flex-grow: 0;
                color: #d6d6d6;
                font-size:1em;
                height: 35px;
                padding: 10px 5px 5px;
                display: flex;
                justify-content: space-between;
                padding-top: 10px;
                <%= footerStyle %>;
            }
            
            #loginLegal {
                padding: 0;
                margin: 0 0 10px 0;
                line-height: 1.4em;
            }
            
            #loginLegal .smallText {
                font-size: 0.85em;
            }
            
            #version {
                white-space: nowrap;
                text-align: right;
                font-weight: bold;
                line-height: 1.4em;
            }
            
            -->
        </style>
    </head>
    <body>
        
        <div id="main">
            
            <div id="topBar">
                <img width="<%= logoWidth %>" height="<%= logoHeight %>" alt="<%= logoAlt %>" src="<%= logoUrl %>" />
            </div>
            
            <div id="content">
                <%
                    String nuxeoUsername = request.getUserPrincipal().getName();
                    request.getSession().setAttribute(NuxeoOAuth2Filter.USERNAME_KEY, nuxeoUsername);
                    
                    //  ELORA: Retrieve userProfile locale
                    String nuxeoLocale = "";
                    Framework.login();
                    RepositoryManager mgr = Framework.getService(RepositoryManager.class);
                    Repository defaultRepository = mgr.getDefaultRepository();
                    
                    String clientName = (String) request.getSession().getAttribute("client_name");
                    if(clientName == null) {
                        clientName = "";
                    }
                    
                    CoreSession coreSession = CoreInstance.openCoreSession(defaultRepository.getName());
                    UserProfileService userProfileService = Framework.getLocalService(UserProfileService.class);
                    DocumentModel userProfileDoc = userProfileService.getUserProfileDocument(nuxeoUsername, coreSession);
                    
                    if (userProfileDoc.getPropertyValue(UserProfileConstants.USER_PROFILE_LOCALE) != null) {
                        nuxeoLocale = (String) userProfileDoc.getPropertyValue(UserProfileConstants.USER_PROFILE_LOCALE);
                    }
                    if (nuxeoLocale == null || nuxeoLocale.isEmpty()) {
                        Locale browserLocale = request.getLocale();
                        if(browserLocale != null && (browserLocale.getLanguage().startsWith("eu")
                                || browserLocale.getLanguage().startsWith("es")
                                || browserLocale.getLanguage().startsWith("en"))) {
                            nuxeoLocale = browserLocale.getLanguage();
                        } else {
                            nuxeoLocale = "en";
                        }
                    }
                    
                    CoreInstance.closeCoreSession(coreSession);
                %>
                <fmt:setLocale value="<%= nuxeoLocale %>"/>
                <fmt:setBundle basename="messages" var="messages"/>
                
                <p id="nuxeo_username" style="display:none;"><%= nuxeoUsername %></p>
                <p id="nuxeo_locale" style="display:none;"><%= nuxeoLocale %></p>
                
                <form action="oauth2/authorization" method="POST">
                    <h2>
                        <fmt:message bundle="${messages}" key="eloraplm.label.oauth2.grantConfirmation">
                            <fmt:param value="<%= clientName %>"/>
                        </fmt:message>
                    </h2>
                    <br/>
                    <input name="authorization_key" type="hidden"
                        value="<%= request.getSession().getAttribute("authorization_key")%>"/>
                    <input name="state" type="hidden" value="<%= request.getSession().getAttribute("state") %>"/>
                    <fmt:message bundle="${messages}" key="label.no" var="label_no" />
                    <fmt:message bundle="${messages}" key="label.yes" var="label_yes" />
                    <input type="button" value="${label_no}" />
                    <input type="submit" value="${label_yes}"/>
                </form>
            </div>
            <div id="footer">
                <div id="loginLegal">
                    <strong>
                        <fmt:message bundle="${messages}" key="eloraplm.label.copyright">
                            <fmt:param value="<%= currentYear %>" />
                        </fmt:message>
                    </strong>
                    <br />
                    <span class="smallText">
                        <fmt:message bundle="${messages}" key="eloraplm.label.poweredByNuxeo" />
                        :&nbsp;
                        <fmt:message bundle="${messages}" key="label.login.copyright">
                            <fmt:param value="<%= currentYear %>" />
                        </fmt:message>
                    </span>
                </div>
                
                <div id="version">
                    <%= productName %> &nbsp; <%= productVersion %>
                </div>
            </div>
            
        </div>
        
    </body>
</html>
