<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<!-- Nuxeo Enterprise Platform -->
<%@ page contentType="text/html; charset=UTF-8"%>
<%@ page language="java"%>
<%@ page import="java.util.List"%>
<%@ page import="org.joda.time.DateTime"%>
<%@ page import="org.nuxeo.ecm.platform.ui.web.auth.LoginScreenHelper"%>
<%@ page import="org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants"%>
<%@ page import="org.nuxeo.ecm.platform.ui.web.auth.NuxeoAuthenticationFilter"%>
<%@ page import="org.nuxeo.ecm.platform.ui.web.auth.service.LoginProviderLink"%>
<%@ page import="org.nuxeo.ecm.platform.ui.web.auth.service.LoginScreenConfig"%>
<%@ page import="org.nuxeo.ecm.platform.web.common.admin.AdminStatusHelper"%>
<%@ page import="org.nuxeo.runtime.api.Framework"%>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%
String productName = Framework.getProperty("org.nuxeo.ecm.product.name");
String productVersion = Framework.getProperty("org.nuxeo.ecm.product.version");
String testerName = Framework.getProperty("org.nuxeo.ecm.tester.name");
String context = request.getContextPath();

HttpSession httpSession = request.getSession(false);
if (httpSession!=null && httpSession.getAttribute(NXAuthConstants.USERIDENT_KEY)!=null) {
  response.sendRedirect(context + "/" + NuxeoAuthenticationFilter.DEFAULT_START_PAGE);
}

// Read Seam locale cookie
String localeCookieName = "org.jboss.seam.core.Locale";
Cookie localeCookie = null;
Cookie cookies[] = request.getCookies();
if (cookies != null) {
  for (int i = 0; i < cookies.length; i++) {
    if (localeCookieName.equals(cookies[i].getName())) {
      localeCookie = cookies[i];
      break;
    }
  }
}
String selectedLanguage = null;
if (localeCookie != null) {
    selectedLanguage = localeCookie.getValue();
}

boolean maintenanceMode = AdminStatusHelper.isInstanceInMaintenanceMode();
String maintenanceMessage = AdminStatusHelper.getMaintenanceMessage();

LoginScreenConfig screenConfig = LoginScreenHelper.getConfig();
List<LoginProviderLink> providers = screenConfig.getProviders();
boolean useExternalProviders = providers!=null && providers.size()>0;

// fetch Login Screen config and manage default
boolean showNews = screenConfig.getDisplayNews();
String iframeUrl = screenConfig.getNewsIframeUrl();

String backgroundPath = context + "/img/login_bg.jpg";
String bodyBackgroundStyle = LoginScreenHelper.getValueWithDefault(screenConfig.getBodyBackgroundStyle(), "url('" + backgroundPath + "') no-repeat center center fixed #333");
String headerStyle = LoginScreenHelper.getValueWithDefault(screenConfig.getHeaderStyle(), "background-color: rgba(255,255,255,0.7);");
String loginBoxBackgroundStyle = LoginScreenHelper.getValueWithDefault(screenConfig.getLoginBoxBackgroundStyle(), "none repeat scroll 0 0");
String footerStyle = LoginScreenHelper.getValueWithDefault(screenConfig.getFooterStyle(), "");
boolean disableBackgroundSizeCover = Boolean.TRUE.equals(screenConfig.getDisableBackgroundSizeCover());

String logoWidth = LoginScreenHelper.getValueWithDefault(screenConfig.getLogoWidth(), "113");
String logoHeight = LoginScreenHelper.getValueWithDefault(screenConfig.getLogoHeight(), "20");
String logoAlt = LoginScreenHelper.getValueWithDefault(screenConfig.getLogoAlt(), "Nuxeo");
String logoUrl = LoginScreenHelper.getValueWithDefault(screenConfig.getLogoUrl(), context + "/img/nuxeo_logo.png");
String currentYear = new DateTime().toString("Y");

%>

<html>
<%
if (selectedLanguage != null) { %>
<fmt:setLocale value="<%= selectedLanguage %>"/>
<%
}%>
<fmt:setBundle basename="messages" var="messages"/>

<head>
<title><%=productName%></title>
<link rel="icon" type="image/png" href="<%=context%>/icons/favicon.png" />
<link rel="shortcut icon" type="image/x-icon" href="<%=context%>/icons/favicon.ico" />
<script type="text/javascript" src="<%=context%>/scripts/detect_timezone.js"></script>
<script type="text/javascript" src="<%=context%>/scripts/nxtimezone.js"></script>
<script type="text/javascript">
  nxtz.resetTimeZoneCookieIfNotSet();
</script>
<style type="text/css">
<!--
html, body {
  height: 100%;
  overflow: hidden;
}

body {
  font: normal 12px/18pt "Lucida Grande", Arial, sans-serif;
  background: <%=bodyBackgroundStyle%>;
  color: #343434;
  margin: 0;
  <% if (!disableBackgroundSizeCover) { %>
  -webkit-background-size: cover;
  -moz-background-size: cover;
  -o-background-size: cover;
  background-size: cover;
  <%} %>
}

.leftColumn {
  width: 400px
}

/* Header */
.topBar {
  width: 100%;
  height: 60px;
  <%=headerStyle%>;
}

.topBar img {
  margin-left: 55px;
}
/* Login block */
.login {
  background: <%=loginBoxBackgroundStyle%>;
  padding: 1.5em 1em 1em;
  width: 300px;
}

.login_input {
  border: 1px solid #066;
  border-radius: 2px;
  padding: .7em;
  margin: 0 0 .4em;
  font-size:115%;
  box-shadow: 0 1px 2px #FFF, 1px 0 2px #FFF, -1px 0 2px #FFF, 0 -1px 2px #FFF, 1px 1px 2px #e0e0e0 inset;
}

.login_button {
    background-color: #8cd600;
    background: linear-gradient(to bottom right, #8cd600, #7cd660);
    border-radius: 50px;
    text-transform: uppercase;
    padding: 0.6em 1.2em 0.6em 1.2em;
    width: initial;
    box-shadow: 0px 0px 2px 3px rgba(33, 91, 51, 0.3);
    border: 1px solid #2c5800;
    color: white;
    cursor: pointer;
    font-size: 115%;
    font-weight: bold;
    margin: 0 .9em .9em 0;
    text-decoration: none;
    text-shadow: 0px 0px 2px rgb(0, 50, 150);
}

.login_button:hover {
    background-color: #215b33;
    background: linear-gradient(to bottom right, #215b33, #315b53);
    border-color: #215b33;
}

.login_input {
  width: 220px;
}

.login_input:focus {
    border-color: #215b33;
}

/* Other ids */
.loginOptions {
  border-top: 1px solid #ccc;
  color: #999;
  font-size: .85em;
}

.loginOptions p {
  margin: 1em .5em .7em;
  font-size: .95em;
}

.idList {
  padding: 0 1em;
}

.idItem {
  display: inline;
}

.idItem a, .idItem a:visited {
  background: url(<%=context%>/icons/default.png) no-repeat 5px center #eee;
  border: 1px solid #ddd;
  border-radius: 3px;
  color: #666;
  display: inline-block;
  font-weight: bold;
  margin: .3em 0;
  padding: .1em .2em .1em 2em;
  text-decoration: none;
}

.idItem a:hover {
  background-color: #fff;
  color: #333;
}

/* Messages */
.maintenanceModeMessage {
  color: #833;
  font-size: 12px;
  font-weight: bold;
}

.warnMessage,.infoMessage {
  margin: 0 0 10px;
  font-weight: bold;
}

.infoMessage {
  color: #b31500
}

.feedbackMessage {
  color: #366;
  font-weight: bold;
  font-size: 100%;
  margin: 0.5em 0;
  padding: 0.5em 0;
  width: 220px;
  text-align: center;
  text-shadow: 0 2px 8px #fff, 2px 0 8px #fff, -2px 0 8px #fff, 0 -2px 8px #fff;
}

.errorMessage {
  color: #833;
  font-weight: bold;
  
}

.welcome {
  background: none repeat scroll 0 0 #fff;
  bottom: 3%;
  margin: 10px;
  opacity: 0.8;
  padding: 20px;
  position: absolute;
  width: 60%;
}
.welcomeText {
  font: 12px "Lucida Grande", sans-serif;
  text-align: left;
  color: #454545;
  margin: 0 0 .8em
}

/* Footer */
.footer {
  color: #d6d6d6;
  font-size: .65em;
  height:35px;
  <%=footerStyle%>
}

.loginLegal {
  padding: 0;
  margin: 0 0 10px
}

.version {
  padding-right: 50px
}

/* News Container Block */
.news_container {
  text-align: left;
  width: 400px;
}

.block_container {
  border: none;
  height: 500px;
  width: 365px;
  overflow: auto;
  background-color: rgba(255,255,255,0.7);
  opacity: .8;
  filter: alpha(opacity = 80)
}
-->
</style>

</head>

<body>
<!-- Locale: <%= selectedLanguage %> -->
<table cellspacing="0" cellpadding="0" border="0" width="100%" height="100%" class="container">
  <tbody>
    <tr class="topBar">
      <td colspan="2">
        <img width="<%=logoWidth%>" height="<%=logoHeight%>" alt="<%=logoAlt%>" src="<%=logoUrl%>" />
      </td>
    </tr>
    <tr>
      <td align="center" colspan="2">
        <%@ include file="login_welcome.jsp" %>
        <form method="post" action="nxstartup.faces">
          <!-- To prevent caching -->
          <%
              response.setHeader("Cache-Control", "no-cache"); // HTTP 1.1
              response.setHeader("Pragma", "no-cache"); // HTTP 1.0
              response.setDateHeader("Expires", -1); // Prevents caching at the proxy server
          %>
          <!-- ;jsessionid=<%=request.getSession().getId()%> -->
          <!-- ImageReady Slices (login_cutted.psd) -->

          <div class="login">
            <% if (maintenanceMode) { %>
              <div class="maintenanceModeMessage">
                <div class="warnMessage">
                  <fmt:message bundle="${messages}" key="label.maintenancemode.active" /><br/>
                  <fmt:message bundle="${messages}" key="label.maintenancemode.adminLoginOnly" />
                </div>
              <div class="infoMessage">
                <fmt:message bundle="${messages}" key="label.maintenancemode.message" /> : <br/>
                <%=maintenanceMessage%>
              </div>
              </div>
            <%} %>
            <table>
             <tr>
               <td>
                 <c:if test="${param.nxtimeout}">
                   <div class="feedbackMessage">
                     <fmt:message bundle="${messages}" key="label.login.timeout" />
                   </div>
                 </c:if>
                 <c:if test="${param.connectionFailed}">
                   <div class="feedbackMessage errorMessage">
                     <fmt:message bundle="${messages}" key="label.login.connectionFailed" />
                   </div>
                 </c:if>
                 <c:if test="${param.loginFailed == 'true' and param.connectionFailed != 'true'}">
                   <div class="feedbackMessage errorMessage">
                     <fmt:message bundle="${messages}" key="label.login.invalidUsernameOrPassword" />
                   </div>
                 </c:if>
                 <c:if test="${param.loginMissing}">
                   <div class="feedbackMessage errorMessage">
                     <fmt:message bundle="${messages}" key="label.login.missingUsername" />
                   </div>
                 </c:if>
                 <c:if test="${param.securityError}">
                   <div class="feedbackMessage errorMessage">
                     <fmt:message bundle="${messages}" key="label.login.securityError" />
                   </div>
                 </c:if>
               </td>
             </tr>
              <tr>
                <td>
                  <input class="login_input" type="text" name="user_name" id="username" placeholder="<fmt:message bundle="${messages}" key="label.login.username" />"/>
                </td>
              </tr>
              <tr>
                <td>
                  <input class="login_input" type="password" name="user_password" id="password" placeholder="<fmt:message bundle="${messages}" key="label.login.password" />">
                </td>
              </tr>
              <tr>
                <td align="center">
                  <% // label.login.logIn %>
                  <% if (selectedLanguage != null) { %>
                  <input type="hidden" name="language"
                      id="language" value="<%= selectedLanguage %>" />
                  <% } %>
                  <input type="hidden" name="requestedUrl"
                      id="requestedUrl" value="${fn:escapeXml(param.requestedUrl)}" />
                  <input type="hidden" name="forceAnonymousLogin"
                      id="true" />
                  <input type="hidden" name="form_submitted_marker"
                      id="form_submitted_marker" />
                  <input class="login_button" type="submit" name="Submit"
                    value="<fmt:message bundle="${messages}" key="label.login.logIn" />" />
                </td>
              </tr>
            </table>

            <% if (useExternalProviders) {%>
            <div class="loginOptions">
              <p><fmt:message bundle="${messages}" key="label.login.loginWithAnotherId" /></p>
              <div class="idList">
                <% for (LoginProviderLink provider : providers) { %>
                <div class="idItem">
                  <a href="<%= provider.getLink(request, request.getContextPath() + request.getParameter("requestedUrl")) %>"
                    style="background-image:url('<%=(context + provider.getIconPath())%>')" title="<%=provider.getDescription()%>"><%=provider.getLabel()%>
                  </a>
                </div>
                <%}%>
              </div>
            </div>
            <%}%>

          </div>
        </form>
      </td>
      <!-- 
      <td class="news_container" align="right" valign="middle">
        <% if (showNews && !"Nuxeo-Selenium-Tester".equals(testerName)) { %>
          <iframe class="block_container" style="visibility:hidden"
            onload="javascript:this.style.visibility='visible';"
            src="<%=iframeUrl%>"></iframe>
        <% } %>
      </td>
       -->
    </tr>
    
    
    <tr class="footer">
      <td align="center" valign="bottom">
      <div class="loginLegal">
        
        <strong>
            <fmt:message bundle="${messages}" key="eloraplm.label.copyright">
              <fmt:param value="<%=currentYear %>" />
            </fmt:message>
        </strong>
        
        &nbsp;-&nbsp;

        <fmt:message bundle="${messages}" key="eloraplm.label.poweredByNuxeo" />

        :&nbsp;

        <fmt:message bundle="${messages}" key="label.login.copyright">
          <fmt:param value="<%=currentYear %>" />
        </fmt:message>
        
      </div>
      </td>
      <td align="right" class="version" valign="bottom">
        <div class="loginLegal">
         <%=productName%>
         &nbsp;
         <%=productVersion%>
        </div>
      </td>
    </tr>
  </tbody>
</table>

<script type="text/javascript">
  document.getElementById('username').focus();
</script>

<!--   Current User = <%=request.getRemoteUser()%> -->

</body>
</html>
