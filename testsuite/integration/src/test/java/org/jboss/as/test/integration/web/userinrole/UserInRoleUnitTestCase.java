/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.integration.web.userinrole;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.security.Constants.AUTHENTICATION;
import static org.jboss.as.security.Constants.CODE;
import static org.jboss.as.security.Constants.FLAG;
import static org.jboss.as.security.Constants.MODULE_OPTIONS;
import static org.jboss.as.security.Constants.SECURITY_DOMAIN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests of the servlet request isUserInRole call.
 * 
 * @author Scott.Stark@jboss.org
 * @author lbarreiro@redhat.com
 */
@RunWith(Arquillian.class)
@RunAsClient
public class UserInRoleUnitTestCase {
    private static Logger log = Logger.getLogger(UserInRoleUnitTestCase.class);

    @ArquillianResource
    private URL baseURL;

    private static Boolean jacc = Boolean.valueOf(System.getProperty("jboss.security.jacc", "false"));;

    @Deployment(name = "web-sso.ear", testable = false)
    public static EnterpriseArchive deployment() {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        String resourcesLocation = "org/jboss/as/test/integration/web/userinrole/resources/";

        try {
            ModelControllerClient mcc = ModelControllerClient.Factory.create(InetAddress.getByName("localhost"), 9999);
            createSecurityDomains(mcc);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        // <war destfile="${build.lib}/userinrole.war"
        // webxml="${build.resources}/web/userinrole/web.xml">
        // <webinf dir="${build.resources}/web/userinrole">
        // <include name="jboss-web.xml" />
        // </webinf>
        // <classes dir="${build.classes}">
        // <include name="org/jboss/test/web/servlets/UserInRoleServlet.class"/>
        // </classes>
        // </war>

        WebArchive war = ShrinkWrap.create(WebArchive.class, "userinrole.war");
        war.setWebXML(tccl.getResource(resourcesLocation + "web.xml"));
        war.addAsWebInfResource(tccl.getResource(resourcesLocation + "jboss-web.xml"), "jboss-web.xml");
        war.addClass(UserInRoleServlet.class);

        // <war destfile="${build.lib}/userinrole1.war"
        // webxml="${build.resources}/web/userinrole/web1/web.xml">
        // <webinf dir="${build.resources}/web/userinrole/web1">
        // <include name="jboss-web.xml" />
        // </webinf>
        // <fileset dir="${build.resources}/web/userinrole/web1">
        // <include name="*.jsp" />
        // </fileset>
        // </war>

        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "userinrole1.war");
        war1.setWebXML(tccl.getResource(resourcesLocation + "war1/web.xml"));
        war1.addAsWebInfResource(tccl.getResource(resourcesLocation + "war1/jboss-web.xml"), "jboss-web.xml");
        war1.addAsWebResource(tccl.getResource(resourcesLocation + "war1/index.jsp"), "index.jsp");

        // <war destfile="${build.lib}/userinrole2.war"
        // webxml="${build.resources}/web/userinrole/web2/web.xml">
        // <webinf dir="${build.resources}/web/userinrole/web2">
        // <include name="jboss-web.xml" />
        // </webinf>
        // <fileset dir="${build.resources}/web/userinrole/web2">
        // <include name="*.jsp" />
        // </fileset>
        // </war>

        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "userinrole2.war");
        war2.setWebXML(tccl.getResource(resourcesLocation + "war2/web.xml"));
        war2.addAsWebInfResource(tccl.getResource(resourcesLocation + "war2/jboss-web.xml"), "jboss-web.xml");
        war2.addAsWebResource(tccl.getResource(resourcesLocation + "war2/index.jsp"), "index.jsp");

        // <zip destfile="${build.lib}/userinrole.ear">
        // <zipfileset dir="${build.resources}/web/userinrole"
        // prefix="META-INF">
        // <include name="application.xml"/>
        // <include name="jboss-app.xml"/>
        // </zipfileset>
        // <zipfileset dir="${build.lib}">
        // <include name="userinrole.war"/>
        // <include name="userinrole1.war"/>
        // <include name="userinrole2.war"/>
        // </zipfileset>
        // <zipfileset dir="${build.resources}/web/userinrole">
        // <include name="security-service.xml"/>
        // </zipfileset>
        // </zip>

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "userinrole.ear");
        ear.setApplicationXML(tccl.getResource(resourcesLocation + "application.xml"));

        ear.addAsModule(war);
        ear.addAsModule(war1);
        ear.addAsModule(war2);

        log.info(war.toString(true));
        log.info(war1.toString(true));
        log.info(war2.toString(true));
        log.info(ear.toString(true));
        return ear;
    }

    @AfterClass
    public static void undeployment() {
        try {
            ModelControllerClient mcc = ModelControllerClient.Factory.create(InetAddress.getByName("localhost"), 9999);
            removeSecurityDomains(mcc);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private static void createSecurityDomains(ModelControllerClient client) throws Exception {
        final List<ModelNode> updates = new ArrayList<ModelNode>();

        // ### userinrole
        
        ModelNode op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(OP_ADDR).add(SUBSYSTEM, "security");
        op.get(OP_ADDR).add(SECURITY_DOMAIN, "userinrole");

        ModelNode xmlModule = new ModelNode();
        xmlModule.get(CODE).set("org.jboss.security.auth.spi.XMLLoginModule");
        xmlModule.get(FLAG).set("required");
        
        StringBuilder userInfo = new StringBuilder();
        userInfo.append("<ur:users xsi:schemaLocation=\"urn:jboss:user-roles:1.0 resource:user-roles_1_0.xsd\" xmlns:ur=\"urn:jboss:user-roles:1.0\">");
        userInfo.append("  <ur:user name=\"jduke\" password=\"theduke\">");
        userInfo.append("    <ur:role name=\"ServletUserRole\" />");
        userInfo.append("    <ur:role name=\"AnotherUserRole\" />");
        userInfo.append("    <ur:role name=\"AuthorizedUser\" />");
        userInfo.append("    <ur:role name=\"callerJduke\" group=\"CallerPrincipal\" />");
        userInfo.append("  </ur:user>");
        userInfo.append("</ur:users>");
        
        xmlModule.get(MODULE_OPTIONS).add("userInfo", userInfo.toString());
        xmlModule.get(MODULE_OPTIONS).add("unauthenticatedIdentity", "guest");

        op.get(AUTHENTICATION).set(Arrays.asList(xmlModule));
        updates.add(op);

        // ### userinrole1
        
        ModelNode op1 = new ModelNode();
        op1.get(OP).set(ADD);
        op1.get(OP_ADDR).add(SUBSYSTEM, "security");
        op1.get(OP_ADDR).add(SECURITY_DOMAIN, "userinrole1");

        ModelNode xmlModule1 = new ModelNode();
        xmlModule1.get(CODE).set("org.jboss.security.auth.spi.XMLLoginModule");
        xmlModule1.get(FLAG).set("required");
        
        StringBuilder userInfo1 = new StringBuilder();
        userInfo1.append("<ur:users xsi:schemaLocation=\"urn:jboss:user-roles:1.0 resource:user-roles_1_0.xsd\" xmlns:ur=\"urn:jboss:user-roles:1.0\">");
        userInfo1.append("  <ur:user name=\"sa\" password=\"sa\">");
        userInfo1.append("    <ur:role name=\"X\" />");
        userInfo1.append("    <ur:role name=\"Z\" />");
        userInfo1.append("  </ur:user>");
        userInfo1.append("</ur:users>");
        
        xmlModule1.get(MODULE_OPTIONS).add("userInfo", userInfo1.toString());
        xmlModule1.get(MODULE_OPTIONS).add("unauthenticatedIdentity", "guest");

        op1.get(AUTHENTICATION).set(Arrays.asList(xmlModule1));
        updates.add(op1);

        // ### userinrole2
        
        ModelNode op2 = new ModelNode();
        op2.get(OP).set(ADD);
        op2.get(OP_ADDR).add(SUBSYSTEM, "security");
        op2.get(OP_ADDR).add(SECURITY_DOMAIN, "userinrole2");

        ModelNode xmlModule2 = new ModelNode();
        xmlModule2.get(CODE).set("org.jboss.security.auth.spi.XMLLoginModule");
        xmlModule2.get(FLAG).set("required");
        
        StringBuilder userInfo2 = new StringBuilder();
        userInfo2.append("<ur:users xsi:schemaLocation=\"urn:jboss:user-roles:1.0 resource:user-roles_1_0.xsd\" xmlns:ur=\"urn:jboss:user-roles:1.0\">");
        userInfo2.append("  <ur:user name=\"sa\" password=\"sa\">");
        userInfo2.append("    <ur:role name=\"Y\" />");
        userInfo2.append("    <ur:role name=\"Z\" />");
        userInfo2.append("  </ur:user>");
        userInfo2.append("</ur:users>");
        
        xmlModule2.get(MODULE_OPTIONS).add("userInfo", userInfo2.toString());
        xmlModule2.get(MODULE_OPTIONS).add("unauthenticatedIdentity", "guest");

        op2.get(AUTHENTICATION).set(Arrays.asList(xmlModule2));
        updates.add(op2);
        
        applyUpdates(updates, client);
    }

    public static void removeSecurityDomains(final ModelControllerClient client) throws Exception {
        final List<ModelNode> updates = new ArrayList<ModelNode>();

        ModelNode op = new ModelNode();
        op.get(OP).set(REMOVE);
        op.get(OP_ADDR).add(SUBSYSTEM, "security");
        op.get(OP_ADDR).add(SECURITY_DOMAIN, "userinrole");
        updates.add(op);
        
        op = new ModelNode();
        op.get(OP).set(REMOVE);
        op.get(OP_ADDR).add(SUBSYSTEM, "security");
        op.get(OP_ADDR).add(SECURITY_DOMAIN, "userinrole1");
        updates.add(op);
        
        op = new ModelNode();
        op.get(OP).set(REMOVE);
        op.get(OP_ADDR).add(SUBSYSTEM, "security");
        op.get(OP_ADDR).add(SECURITY_DOMAIN, "userinrole2");
        updates.add(op);

        applyUpdates(updates, client);
    }

    public static void applyUpdates(final List<ModelNode> updates, final ModelControllerClient client) throws Exception {
        for (ModelNode update : updates) {
            log.info("+++ Update on " + client + ":\n" + update.toString());
            ModelNode result = client.execute(new OperationBuilder(update).build());
            if (result.hasDefined("outcome") && "success".equals(result.get("outcome").asString())) {
                if (result.hasDefined("result"))
                    log.info(result.get("result"));
            } else if (result.hasDefined("failure-description")) {
                throw new RuntimeException(result.get("failure-description").toString());
            } else {
                throw new RuntimeException("Operation not successful; outcome = " + result.get("outcome"));
            }
        }
    }

    /**
     * Test that the custom 404 error page is seen
     */
    @Test
    public void testRoleWithLink() throws Exception {
        log.info("+++ testRoleWithLink");
        URL url = new URL(baseURL + "/userinrole/testRoleWithLink");
        UserInRoleUnitTestCase.accessURL(url, "UserInRoleRealm", HttpURLConnection.HTTP_OK);
    }

    @Test
    public void testRoleWithoutLink() throws Exception {
        log.info("+++ testUnreferencedRole");
        URL url = new URL(baseURL + "/userinrole/testUnreferencedRole");
        UserInRoleUnitTestCase.accessURL(url, "UserInRoleRealm", HttpURLConnection.HTTP_OK);
    }

    /**
     * Test that two wars from different security domains with common principal
     * names do not conflict in terms of isUserInRole results.
     * http://jira.jboss.com/jira/browse/JBAS-3043
     * 
     * This is the non-jacc version where the programmatic security of
     * isUserInRole will work off of the roles populated in the subject,
     * irrespective of whether the roles are fully defined in the web.xml
     * 
     * @throws Exception
     */
    @Test
    public void testConflictingUserInRole() throws Exception {
        log.info("+++ testConflictingUserInRole");
        if (jacc == Boolean.TRUE) {
            log.info("Skipped because jacc property is defined");
            return;
        }
        String base = UserInRoleUnitTestCase.getBaseURL("sa", "sa", baseURL);

        // Hit the first web app and validate isUserInRole calls
        URL url1 = new URL(base + "userinrole1");
        HttpResponse response = UserInRoleUnitTestCase.accessURL(url1, "JBAS-3043-1", HttpURLConnection.HTTP_OK);
        Header X = response.getFirstHeader("X-isUserInRole-X");
        log.info("X " + X);
        assertEquals("X-isUserInRole-X(" + X + ") is true", "true", X.getValue());
        Header Y = response.getFirstHeader("X-isUserInRole-Y");
        log.info("Y " + Y);
        assertEquals("X-isUserInRole-Y(" + Y + ") is false", "false", Y.getValue());
        Header Z = response.getFirstHeader("X-isUserInRole-Z");
        log.info("Z " + Z);
        assertEquals("X-isUserInRole-Z(" + Z + ") is true", "true", Z.getValue());

        // Hit the second web app and validate isUserInRole calls
        URL url2 = new URL(base + "userinrole2");
        response = UserInRoleUnitTestCase.accessURL(url2, "JBAS-3043-2", HttpURLConnection.HTTP_OK);
        X = response.getFirstHeader("X-isUserInRole-X");
        log.info("X " + X);
        assertEquals("X-isUserInRole-X(" + X + ") is false", "false", X.getValue());
        Y = response.getFirstHeader("X-isUserInRole-Y");
        log.info("Y " + Y);
        assertEquals("X-isUserInRole-Y(" + Y + ") is true", "true", Y.getValue());
        Z = response.getFirstHeader("X-isUserInRole-Z");
        log.info("Z " + Z);
        assertEquals("X-isUserInRole-Z(" + Z + ") is true", "true", Z.getValue());

        response = UserInRoleUnitTestCase.accessURL(url1, "JBAS-3043-1", HttpURLConnection.HTTP_OK);
        X = response.getFirstHeader("X-isUserInRole-X");
        log.info("X " + X);
        assertEquals("X-isUserInRole-X(" + X + ") is true", "true", X.getValue());
        Y = response.getFirstHeader("X-isUserInRole-Y");
        log.info("Y " + Y);
        assertEquals("X-isUserInRole-Y(" + Y + ") is false", "false", Y.getValue());
        Z = response.getFirstHeader("X-isUserInRole-Z");
        log.info("Z " + Z);
        assertEquals("X-isUserInRole-Z(" + Z + ") is true", "true", Z.getValue());
    }

    /**
     * Test that two wars from different security domains with common principal
     * names do not conflict in terms of isUserInRole results.
     * http://jira.jboss.com/jira/browse/JBAS-3043
     * 
     * This is the jacc version where the programmatic security of isUserInRole
     * will work only of the roles are fully defined in the web.xml
     * 
     * @throws Exception
     */
    @Test
    public void testConflictingUserInRoleJaccVersion() throws Exception {
        log.info("+++ testConflictingUserInRoleJaccVersion");
        if (jacc == Boolean.FALSE) {
            log.info("Skipped because jacc property is not defined");
            return;
        }
        String base = UserInRoleUnitTestCase.getBaseURL("sa", "sa", baseURL);

        // Hit the first web app and validate isUserInRole calls
        URL url1 = new URL(base + "userinrole1");
        HttpResponse response = UserInRoleUnitTestCase.accessURL(url1, "JBAS-3043-1", HttpURLConnection.HTTP_OK);
        Header X = response.getFirstHeader("X-isUserInRole-X");
        log.info("X " + X);
        assertEquals("X-isUserInRole-X(" + X + ") is false", "false", X.getValue());
        Header Y = response.getFirstHeader("X-isUserInRole-Y");
        log.info("Y " + Y);
        assertEquals("X-isUserInRole-Y(" + Y + ") is false", "false", Y.getValue());
        Header Z = response.getFirstHeader("X-isUserInRole-Z");
        log.info("Z " + Z);
        assertEquals("X-isUserInRole-Z(" + Z + ") is true", "true", Z.getValue());

        // Hit the second web app and validate isUserInRole calls
        URL url2 = new URL(base + "userinrole2");
        response = UserInRoleUnitTestCase.accessURL(url2, "JBAS-3043-2", HttpURLConnection.HTTP_OK);
        X = response.getFirstHeader("X-isUserInRole-X");
        log.info("X " + X);
        assertEquals("X-isUserInRole-X(" + X + ") is false", "false", X.getValue());
        Y = response.getFirstHeader("X-isUserInRole-Y");
        log.info("Y " + Y);
        assertEquals("X-isUserInRole-Y(" + Y + ") is false", "false", Y.getValue());
        Z = response.getFirstHeader("X-isUserInRole-Z");
        log.info("Z " + Z);
        assertEquals("X-isUserInRole-Z(" + Z + ") is true", "true", Z.getValue());

        response = UserInRoleUnitTestCase.accessURL(url1, "JBAS-3043-1", HttpURLConnection.HTTP_OK);
        X = response.getFirstHeader("X-isUserInRole-X");
        log.info("X " + X);
        assertEquals("X-isUserInRole-X(" + X + ") is false", "false", X.getValue());
        Y = response.getFirstHeader("X-isUserInRole-Y");
        log.info("Y " + Y);
        assertEquals("X-isUserInRole-Y(" + Y + ") is false", "false", Y.getValue());
        Z = response.getFirstHeader("X-isUserInRole-Z");
        log.info("Z " + Z);
        assertEquals("X-isUserInRole-Z(" + Z + ") is true", "true", Z.getValue());
    }

    private static HttpResponse accessURL(URL url, String realm, int expectedHttpCode) throws IOException {
        return accessURL(url, realm, expectedHttpCode, new Header[0], MethodType.GET);
    }

    private static HttpResponse accessURL(URL url, String realm, int expectedHttpCode, Header[] headers, MethodType type)
            throws IOException {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpRequestBase request = createMethod(url, type);

        for (Header h : headers) {
            request.addHeader(h);
        }

        log.debug("Connecting to: " + url);
        if (url.getUserInfo() != null) {
            UsernamePasswordCredentials auth = new UsernamePasswordCredentials(url.getUserInfo());
            httpclient.getCredentialsProvider().setCredentials(new AuthScope(url.getHost(), url.getPort(), realm), auth);
        }

        log.debug("RequestURI: " + request.getURI());
        log.info("Executing request " + request.getRequestLine());
        HttpResponse response = httpclient.execute(request);

        int statusCode = response.getStatusLine().getStatusCode();
        String content = EntityUtils.toString(response.getEntity());

        log.debug("responseCode=" + statusCode + ", response=" + response.getStatusLine().getReasonPhrase());
        log.debug(content);

        // Validate that we are seeing the requested response code
        assertTrue("Expected reply code=" + expectedHttpCode + ", actual=" + statusCode, statusCode == expectedHttpCode);

        return response;
    }

    private static HttpRequestBase createMethod(URL url, MethodType type) {
        switch (type) {
            case GET:
                return new HttpGet(url.toString());
            case POST:
                return new HttpPost(url.toString());
            case HEAD:
                return new HttpHead(url.toString());
            case OPTIONS:
                return new HttpOptions(url.toString());
            case PUT:
                return new HttpPut(url.toString());
            case DELETE:
                return new HttpDelete(url.toString());
            case TRACE:
                return new HttpTrace(url.toString());
            default:
                return null;
        }
    }

    private static enum MethodType {
        GET, POST, HEAD, OPTIONS, PUT, DELETE, TRACE;
    }

    public static String getBaseURL(String username, String password, URL url) {
        String newURL = url.getProtocol() + "://" + username + ":" + password + "@" + url.getHost() + ":" + url.getPort() + "/";
        log.debug("newURL for URL=" + url + ", user=" + username + ", password=" + password + "is [" + newURL + "]");
        return newURL;
    }

}
