<%-- <%@page import="org.apache.commons.httpclient.HttpClient"%> --%>
<%-- <%@page import="org.apache.commons.httpclient.HttpStatus"%> --%>
<%-- <%@page import="org.apache.commons.httpclient.methods.PostMethod"%> --%>
<%-- <%@page import="java.io.BufferedReader"%> --%>
<%-- <%@page import="java.io.InputStreamReader"%> --%>
<%-- <%@page import="org.codehaus.jackson.map.ObjectMapper"%> --%>
<%-- <%@page import="com.aritu.eloraplm.oauth2.OAuth2Response"%> --%>

<%-- <%@page import="org.nuxeo.runtime.api.Framework"%> --%>
<%-- <%@page import="org.nuxeo.ecm.platform.oauth2.clients.ClientRegistry"%> --%>
<%-- <%@page import="org.nuxeo.ecm.platform.oauth2.clients.OAuth2Client"%> --%>

<%
	String error = "";
	String authorizationCode = "";
	if (request.getParameter("error") != null) {
	    error = request.getParameter("error");
	}
	if (request.getParameter("code") != null) {
	    authorizationCode = request.getParameter("code");
	}
	
// 	br = new BufferedReader(new InputStreamReader(method.getResponseBodyAsStream()));
// 	String responseJson = "";
// 	String readLine;
// 	while(((readLine = br.readLine()) != null)) {
// 		responseJson += readLine;
// 	}
	
// 	OAuth2Response responseObject = new ObjectMapper().readValue(responseJson, OAuth2Response.class);
// 	accessToken = responseObject.getAccessToken();
// 	refreshToken = responseObject.getRefreshToken();
// 	expiresIn = Integer.toString(responseObject.getExpiresIn());

%>	
<html>
<head>
</head>
<body>
	<p id="error"><%= error%></p>
	<p id="authorization_code"><%= authorizationCode%></p>
</body>
</html>