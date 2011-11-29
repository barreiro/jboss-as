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
package org.jboss.as.test.integration.web.webintegration;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

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
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests of servlet container integration into the JBoss server. This test
 * requires than a web container be integrated into the JBoss server. The tests
 * currently do NOT use the java.net.HttpURLConnection and associated http
 * client and these do not return valid HTTP error codes so if a failure occurs
 * it is best to connect the webserver using a browser to look for additional
 * error info.
 * 
 * The secure access tests require a user named 'jduke' with a password of
 * 'theduke' with a role of 'AuthorizedUser' in the servlet container.
 * 
 * @author Scott.Stark@jboss.org
 * @author lbarreiro@redhat.com
 */
@RunWith(Arquillian.class)
@RunAsClient
public class WebIntegrationUnitTestCase {

    private static Logger log = Logger.getLogger(WebIntegrationUnitTestCase.class);

    private static String REALM = "JBossTest Servlets";

    @ArquillianResource
    private URL baseURL;

    @ArquillianResource
    private URL baseURLNoAuth;

    @Deployment(name = "jbosstest-web.ear", testable = false)
    public static EnterpriseArchive deployment() {
        return null;
    }

    /**
     * Access the http://{host}/jbosstest/APIServlet to test the getRealPath
     * method
     */
    @Test
    public void testRealPath() throws Exception {
        URL url = new URL(baseURL + "jbosstest/APIServlet?op=testGetRealPath");
        WebIntegrationUnitTestCase.accessURL(url);
    }

    /**
     * Access the http://{host}/jbosstest/APIServlet to test the
     * HttpSessionListener events
     */
    @Test
    public void testHttpSessionListener() throws Exception {
        URL url = new URL(baseURL + "jbosstest/APIServlet?op=testSessionListener");
        WebIntegrationUnitTestCase.accessURL(url);
    }

    /**
     * Access the http://{host}/jbosstest/EJBOnStartupServlet
     */
    @Test
    public void testEJBOnStartupServlet() throws Exception {
        URL url = new URL(baseURL + "jbosstest/EJBOnStartupServlet");
        WebIntegrationUnitTestCase.accessURL(url);
    }

    /**
     * Access the http://{host}/jbosstest/ENCServlet
     */
    @Test
    public void testENCServlet() throws Exception {
        URL url = new URL(baseURL + "jbosstest/ENCServlet");
        WebIntegrationUnitTestCase.accessURL(url);
    }

    /**
     * Access the http://{host}/jbosstest/SimpleServlet to test that servlets in
     * the WEB-INF/lib jar.
     */
    @Test
    public void testServletInJar() throws Exception {
        URL url = new URL(baseURL + "jbosstest/SimpleServlet");
        WebIntegrationUnitTestCase.accessURL(url);
    }

    /**
     * Access the http://{host}/jbosstest/EJBServlet
     */
    @Test
    public void testEJBServlet() throws Exception {
        URL url = new URL(baseURL + "jbosstest/EJBServlet");
        WebIntegrationUnitTestCase.accessURL(url);
    }

    /**
     * Access the http://{host}/jbosstest/EntityServlet
     */
    @Test
    public void testEntityServlet() throws Exception {
        URL url = new URL(baseURL + "jbosstest/EntityServlet");
        WebIntegrationUnitTestCase.accessURL(url);
    }

    /**
     * Access the http://{host}/jbosstest/StatefulSessionServlet
     */
    @Test
    public void testStatefulSessionServlet() throws Exception {
        URL url = new URL(baseURL + "jbosstest/StatefulSessionServlet");
        WebIntegrationUnitTestCase.accessURL(url);
        // Need a mechanism to force passivation...
        WebIntegrationUnitTestCase.accessURL(url);
    }

    /**
     * Access the http://{host}/jbosstest/UserTransactionServlet
     */
    @Test
    public void testUserTransactionServlet() throws Exception {
        URL url = new URL(baseURL + "jbosstest/UserTransactionServlet");
        WebIntegrationUnitTestCase.accessURL(url);
    }

    /**
     * Access the http://{host}/jbosstest/SpeedServlet
     */
    @Test
    public void testSpeedServlet() throws Exception {
        URL url = new URL(baseURL + "jbosstest/SpeedServlet");
        WebIntegrationUnitTestCase.accessURL(url);
    }

    /**
     * Access the http://{host}/jbosstest/snoop.jsp
     */
    @Test
    public void testSnoopJSP() throws Exception {
        URL url = new URL(baseURL + "jbosstest/snoop.jsp");
        WebIntegrationUnitTestCase.accessURL(url);
    }

    /**
     * Access the http://{host}/jbosstest/snoop.jsp
     */
    @Test
    public void testSnoopJSPByPattern() throws Exception {
        URL url = new URL(baseURL + "jbosstest/test-snoop.snp");
        WebIntegrationUnitTestCase.accessURL(url);
    }

    /**
     * Access the http://{host}/jbosstest/test-jsp-mapping
     */
    @Test
    public void testSnoopJSPByMapping() throws Exception {
        URL url = new URL(baseURL + "jbosstest/test-jsp-mapping");
        WebIntegrationUnitTestCase.accessURL(url);
    }

    /**
     * Access the http://{host}/jbosstest/classpath.jsp
     */
    @Test
    public void testJSPClasspath() throws Exception {
        URL url = new URL(baseURL + "jbosstest/classpath.jsp");
        WebIntegrationUnitTestCase.accessURL(url);
    }

    /**
     * Access the http://{host}/jbosstest/ClientLoginServlet
     */
    @Test
    public void testClientLoginServlet() throws Exception {
        URL url = new URL(baseURL + "jbosstest/ClientLoginServlet");
        WebIntegrationUnitTestCase.accessURL(url);
    }

    /**
     * Access the http://{host}/jbosstest/restricted/UserInRoleServlet to test
     * isUserInRole.
     */
    @Test
    public void testUserInRoleServlet() throws Exception {
        URL url = new URL(baseURL + "jbosstest/restricted/UserInRoleServlet");
        HttpResponse response = WebIntegrationUnitTestCase.accessURL(url);

        Header errors = response.getFirstHeader("X-ExpectedUserRoles-Errors");
        log.info("X-ExpectedUserRoles-Errors: " + errors);
        assertTrue("X-ExpectedUserRoles-Errors(" + errors + ") is null", errors == null);

        errors = response.getFirstHeader("X-UnexpectedUserRoles-Errors");
        log.info("X-UnexpectedUserRoles-Errors: " + errors);
        assertTrue("X-UnexpectedUserRoles-Errors(" + errors + ") is null", errors == null);
    }

    /**
     * Access the http://{host}/jbosstest/restricted/SecureServlet
     */
    @Test
    public void testSecureServlet() throws Exception {
        URL url = new URL(baseURL + "jbosstest/restricted/SecureServlet");
        WebIntegrationUnitTestCase.accessURL(url);
    }

    /**
     * Access the http://{host}/jbosstest/restricted2/SecureServlet
     */
    @Test
    public void testSecureServlet2() throws Exception {
        URL url = new URL(baseURL + "jbosstest/restricted2/SecureServlet");
        WebIntegrationUnitTestCase.accessURL(url);
    }

    /**
     * Access the http://{host}/jbosstest/restricted/SubjectServlet
     */
    @Test
    public void testSubjectServlet() throws Exception {
        URL url = new URL(baseURL + "jbosstest/restricted/SubjectServlet");
        HttpResponse response = WebIntegrationUnitTestCase.accessURL(url);

        Header hdr = response.getFirstHeader("X-SubjectServlet");
        log.info("X-SubjectServlet: " + hdr);
        assertTrue("X-SubjectServlet(" + hdr + ") is NOT null", hdr != null);

        hdr = response.getFirstHeader("X-SubjectFilter-ENC");
        log.info("X-SubjectFilter-ENC: " + hdr);
        assertTrue("X-SubjectFilter-ENC(" + hdr + ") is NOT null", hdr != null);

        hdr = response.getFirstHeader("X-SubjectFilter-SubjectSecurityManager");
        log.info("X-SubjectFilter-SubjectSecurityManager: " + hdr);
        assertTrue("X-SubjectFilter-SubjectSecurityManager(" + hdr + ") is NOT null", hdr != null);
    }

    /**
     * Access the http://{host}/jbosstest/restricted/SecureServlet
     */
    @Test
    public void testSecureServletAndUnsecureAccess() throws Exception {
        log.info("+++ testSecureServletAndUnsecureAccess");
        URL url = new URL(baseURL + "jbosstest/restricted/SecureServlet");
        log.info("Accessing SecureServlet with valid login");
        WebIntegrationUnitTestCase.accessURL(url);

        String baseURL2 = "http://" + getServerHostForURL() + ":" + Integer.getInteger("web.port", 8080) + '/';
        URL url2 = new URL(baseURL2 + "jbosstest/restricted/UnsecureEJBServlet");
        log.info("Accessing SecureServlet with no login");
        WebIntegrationUnitTestCase.accessURL(url2, REALM, HttpURLConnection.HTTP_UNAUTHORIZED);
    }

    /**
     * Access the http://{host}/jbosstest/restricted/SecureServlet
     */
    @Test
    public void testSecureServletWithBadPass() throws Exception {
        String baseURL = "http://jduke:badpass@" + getServerHostForURL() + ":" + Integer.getInteger("web.port", 8080) + '/';
        URL url = new URL(baseURL + "jbosstest/restricted/SecureServlet");
        WebIntegrationUnitTestCase.accessURL(url, REALM, HttpURLConnection.HTTP_UNAUTHORIZED);
    }

    /**
     * Access the http://{host}/jbosstest/restricted/SecureServlet
     */
    @Test
    public void testSecureServletWithNoLogin() throws Exception {
        String baseURL = "http://" + getServerHostForURL() + ":" + Integer.getInteger("web.port", 8080) + '/';
        URL url = new URL(baseURL + "jbosstest/restricted/SecureServlet");
        WebIntegrationUnitTestCase.accessURL(url, REALM, HttpURLConnection.HTTP_UNAUTHORIZED);
    }

    /**
     * Access the http://{host}/jbosstest-not/unrestricted/SecureServlet
     */
    @Test
    public void testNotJbosstest() throws Exception {
        String baseURL = "http://" + getServerHostForURL() + ":" + Integer.getInteger("web.port", 8080) + '/';
        URL url = new URL(baseURL + "jbosstest-not/unrestricted/SecureServlet");
        WebIntegrationUnitTestCase.accessURL(url, REALM, HttpURLConnection.HTTP_OK);
    }

    /**
     * Access the http://{host}/jbosstest/restricted/SecuredEntityFacadeServlet
     */
    @Test
    public void testSecuredEntityFacadeServlet() throws Exception {
        URL url = new URL(baseURL + "jbosstest/restricted/SecuredEntityFacadeServlet");
        WebIntegrationUnitTestCase.accessURL(url);
    }

    /**
     * Access the http://{host}/jbosstest/restricted/SecureEJBAccess
     */
    @Test
    public void testSecureEJBAccess() throws Exception {
        URL url = new URL(baseURL + "jbosstest/restricted/SecureEJBAccess");
        WebIntegrationUnitTestCase.accessURL(url);
    }

    /**
     * Access the http://{host}/jbosstest/restricted/include_ejb.jsp
     */
    @Test
    public void testIncludeEJB() throws Exception {
        URL url = new URL(baseURL + "jbosstest/restricted/include_ejb.jsp");
        WebIntegrationUnitTestCase.accessURL(url);
    }

    /**
     * JBAS-3279: Authenticated user can bypass declarative role checks for
     * servlets
     */
    @Test
    public void testUnauthorizedAccess() throws Exception {
        URL url = new URL(baseURL + "jbosstest//restricted3//SecureServlet");
        WebIntegrationUnitTestCase.accessURL(url, REALM, HttpURLConnection.HTTP_FORBIDDEN);
        url = new URL(baseURL + "jbosstest/%2frestricted3//SecureServlet");
        // BES 2007/02/21 -- %xx encoded '/' is verboten so we now expect 400
        // HttpUtils.accessURL(url,REALM, HttpURLConnection.HTTP_FORBIDDEN);
        WebIntegrationUnitTestCase.accessURL(url, REALM, HttpURLConnection.HTTP_BAD_REQUEST);
    }

    /**
     * Access the http://{host}/jbosstest/UnsecureEJBAccess with method=echo to
     * test that an unsecured servlet cannot access a secured EJB method that
     * requires a valid permission. This should fail.
     */
    @Test
    public void testUnsecureEJBAccess() throws Exception {
        URL url = new URL(baseURLNoAuth + "jbosstest/UnsecureEJBAccess?method=echo");
        WebIntegrationUnitTestCase.accessURL(url, REALM, HttpURLConnection.HTTP_INTERNAL_ERROR);
    }

    /**
     * Access the http://{host}/jbosstest/UnsecureEJBAccess with
     * method=unchecked to test that an unsecured servlet can access a secured
     * EJB method that only requires an authenticated user. This requires
     * unauthenticated identity support by the web security domain.
     */
    @Test
    public void testUnsecureAnonEJBAccess() throws Exception {
        URL url = new URL(baseURLNoAuth + "jbosstest/UnsecureEJBAccess?method=unchecked");
        WebIntegrationUnitTestCase.accessURL(url, REALM, HttpURLConnection.HTTP_OK);
    }

    @Test
    public void testUnsecureRunAsServlet() throws Exception {
        URL url = new URL(baseURLNoAuth + "jbosstest/UnsecureRunAsServlet?method=checkRunAs");
        WebIntegrationUnitTestCase.accessURL(url, REALM, HttpURLConnection.HTTP_OK);
    }

    /**
     * Access the http://{host}/jbosstest/UnsecureRunAsServletWithPrincipalName
     * to test that an unsecured servlet can access a secured EJB method by
     * using a run-as role. This should also have a custom run-as principal
     * name.
     * 
     * @throws Exception
     */
    @Test
    public void testUnsecureRunAsServletWithPrincipalName() throws Exception {
        URL url = new URL(baseURLNoAuth
                + "jbosstest/UnsecureRunAsServletWithPrincipalName?ejbName=ejb/UnsecureRunAsServletWithPrincipalNameTarget");
        WebIntegrationUnitTestCase.accessURL(url, REALM, HttpURLConnection.HTTP_OK);
    }

    /**
     * Access the
     * http://{host}/jbosstest/UnsecureRunAsServletWithPrincipalNameAndRoles to
     * test that an unsecured servlet can access a secured EJB method by using a
     * run-as role. This should also have a custom run-as principal name and
     * additional roles.
     * 
     * @throws Exception
     */
    @Test
    public void testUnsecureRunAsServletWithPrincipalNameAndRoles() throws Exception {
        URL url = new URL(
                baseURLNoAuth
                        + "jbosstest/UnsecureRunAsServletWithPrincipalNameAndRoles?ejbName=ejb/UnsecureRunAsServletWithPrincipalNameAndRolesTarget");
        WebIntegrationUnitTestCase.accessURL(url, REALM, HttpURLConnection.HTTP_OK);
    }

    /**
     * Deploy a second ear that include a notjbosstest-web.war to test ears with
     * the same war names conflicting. Access the
     * http://{host}/jbosstest-not2/unrestricted/SecureServlet
     */
    @Test
    public void testNotJbosstest2() throws Exception {
        try {
            deploy("jbosstest-web2.ear");
            String baseURL = "http://" + getServerHostForURL() + ":" + Integer.getInteger("web.port", 8080) + '/';
            URL url = new URL(baseURL + "jbosstest-not2/unrestricted/SecureServlet");
            WebIntegrationUnitTestCase.accessURL(url, REALM, HttpURLConnection.HTTP_OK);
        } finally {
            undeploy("jbosstest-web2.ear");
        } // end of try-finally
    }

    /**
     * Deploy a bad war and then redploy with a fixed war to test failed war
     * cleanup. Access the http://{host}/redeploy/index.html
     */
    @Test
    public void testBadWarRedeploy() throws Exception {
        try {
            deploy("bad-web.war");
            fail("The bad-web.war deployment did not fail");
        } catch (Exception e) {
            log.debug("bad-web.war failed as expected", e);
        } finally {
            undeploy("bad-web.war");
        } // end of try-finally
        try {
            deploy("good-web.war");
            String baseURL = "http://" + getServerHostForURL() + ":" + Integer.getInteger("web.port", 8080) + '/';
            URL url = new URL(baseURL + "redeploy/index.html");
            WebIntegrationUnitTestCase.accessURL(url, REALM, HttpURLConnection.HTTP_OK);
        } finally {
            undeploy("good-web.war");
        } // end of try-finally
    }

    /**
     * Test of a war that accesses classes referred to via the war manifest
     * classpath. Access the http://{host}/manifest/classpath.jsp
     */
    @Test
    public void testWarManifest() throws Exception {
        deploy("manifest-web.ear");
        try {
            String baseURL = "http://" + getServerHostForURL() + ":" + Integer.getInteger("web.port", 8080) + '/';
            URL url = new URL(baseURL + "manifest/classpath.jsp");
            HttpResponse response = WebIntegrationUnitTestCase.accessURL(url);
            Header errors = response.getFirstHeader("X-Exception");
            log.info("X-Exception: " + errors);
            assertTrue("X-Exception(" + errors + ") is null", errors == null);
        } finally {
            undeploy("manifest-web.ear");
        }
    }

    @Test
    public void testBadEarRedeploy() throws Exception {
        try {
            deploy("jbosstest-bad.ear");
            fail("The jbosstest-bad.ear deployment did not fail");
        } catch (Exception e) {
            log.debug("jbosstest-bad.ear failed as expected", e);
        } finally {
            undeploy("jbosstest-bad.ear");
        } // end of finally
        try {
            deploy("jbosstest-good.ear");
            String baseURL = "http://" + getServerHostForURL() + ":" + Integer.getInteger("web.port", 8080) + '/';
            URL url = new URL(baseURL + "redeploy/index.html");
            WebIntegrationUnitTestCase.accessURL(url, REALM, HttpURLConnection.HTTP_OK);
        } finally {
            undeploy("jbosstest-good.ear");
        } // end of try-finally

    }

    /**
     * Validate a war level override of the java2ClassLoadingComplianceOverride
     * flag to true.
     * 
     * @throws Exception
     */
    @Test
    public void testJava2ClassLoadingComplianceOverride() throws Exception {
        log.info("+++ Begin testJava2ClassLoadingComplianceOverride");
        deploy("class-loading.war");
        try {
            String baseURL = "http://" + getServerHostForURL() + ":" + Integer.getInteger("web.port", 8080) + '/';
            // Load a log4j class
            URL url = new URL(baseURL + "class-loading/ClasspathServlet2?class=org.apache.log4j.net.SocketAppender");
            HttpResponse response = WebIntegrationUnitTestCase.accessURL(url, REALM, HttpURLConnection.HTTP_OK);
            Header cs = response.getFirstHeader("X-CodeSource");
            log.info(cs);
            // Validate it has not come from the war
            assertTrue("X-CodeSource(" + cs + ") does not contain war", cs.getValue().indexOf(".war") < 0);
            log.debug(url + " OK");
        } finally {
            undeploy("class-loading.war");
            log.info("+++ End testJava2ClassLoadingComplianceOverride");
        }
    }

    @Test
    public void testWARWithServletAPIClasses() throws Exception {
        log.info("+++ Begin testWARWithServletAPIClasses");
        deploy("servlet-classes.war");
        try {
            String baseURL = "http://" + getServerHostForURL() + ":" + Integer.getInteger("web.port", 8080) + '/';
            // Load a servlet class
            URL url = new URL(baseURL + "servlet-classes/ClasspathServlet2?class=javax.servlet.http.HttpServletResponse");
            HttpResponse response = WebIntegrationUnitTestCase.accessURL(url, REALM, HttpURLConnection.HTTP_OK);
            Header cs = response.getFirstHeader("X-CodeSource");
            log.info(cs);
            // Validate it has not come from the war
            assertTrue("X-CodeSource(" + cs + ") does not contain war", cs.getValue().indexOf(".war") < 0);
            log.debug(url + " OK");
        } finally {
            undeploy("servlet-classes.war");
            log.info("+++ End testWARWithServletAPIClasses");
        }
    }

    private static HttpResponse accessURL(URL url) throws IOException {
        return accessURL(url, null, HttpURLConnection.HTTP_OK, null, MethodType.GET);
    }

    private static HttpResponse accessURL(URL url, String realm, int expectedHttpCode) throws IOException {
        return accessURL(url, realm, expectedHttpCode, null, MethodType.GET);
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
        assertTrue("Expected reply code:" + expectedHttpCode + ", actual=" + statusCode, statusCode == expectedHttpCode);

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

    // ########################################################################

    private void deploy(String string) {
        log.error(" +++ trying deploy of " + string);
        // TODO Auto-generated method stub

    }

    private void undeploy(String string) {
        log.error(" +++ trying undeploy of " + string);
        // TODO Auto-generated method stub

    }

    private String getServerHostForURL() {
        log.error(" +++ trying getServerHostForURL");
        // TODO Auto-generated method stub
        return null;
    }

}
