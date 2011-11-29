<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@page import="org.jboss.as.test.integration.web.jsfintegration.*"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>JBoss JSF Integration Test</title>
</head>
<body>
    <f:view>
        <h1>
            <font color="red" size="+3"><h:outputText value="#{injectionBean.name}: hit this page twice to complete the test." />
            </font>
        </h1>
        <h3>
            <h:outputText rendered="#{injectionBean.postConstructCalled}" value="@PostConstruct was called." />
        </h3>
        <h3>
            <h:outputText rendered="#{mySessionBean.preDestroyCalled}" value="@PreDestroy was called." />
        </h3>
        <h3>
            <h:outputText rendered="#{injectionBean.datasourceInjected}" value="Datasource was injected." />
        </h3>
        <%
            // I think that the fact I need to do this constitutes a bug in JSTL
                if (session.getAttribute("mySessionBean") == null) {
                    session.setAttribute("mySessionBean", new MySessionBean());
                }
        %>
        <h1>
            <font color="red">Classic test of JSTL 1.2/JSF 1.2 with deferred expressions:</font>
        </h1>
        <c:forEach var="item" items="#{mySessionBean.numList}">
            <h3>
                <h:outputText value="#{item}" />
            </h3>
        </c:forEach>
        <h1>
            <font color="red">ServletContext Minor Version Test (should return 5)</font>
        </h1>
        <h3>
            <h:outputText value="ServletContext.getMinorVersion() = #{application.minorVersion}" />
        </h3>
        <h1>
            <font color="red">Enum Test</font>
        </h1>
        <h3>
            <h:outputText rendered="#{mySessionBean.color == 'PURPLE'}" value="JBoss Color selection is #{mySessionBean.color}" />
        </h3>
        <h1>
            <font color="red">Test using JDK class as a managed bean</font>
        </h1>
        <h3>
            <h:outputText value="JButton value = #{myJButton}" />
        </h3>
    </f:view>
</body>
</html>
