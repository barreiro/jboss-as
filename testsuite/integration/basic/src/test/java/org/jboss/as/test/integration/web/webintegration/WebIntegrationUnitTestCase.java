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

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static org.jboss.as.test.integration.web.webintegration.WebIntegrationBase.addCredentials;
import static org.jboss.as.test.integration.web.webintegration.WebIntegrationBase.accessURL;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.web.webintegration.WebIntegrationBase.Deployable;
import org.jboss.as.test.integration.web.webintegration.WebIntegrationBase.WebIntegrationSetup;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
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
@ServerSetup(WebIntegrationSetup.class)
public class WebIntegrationUnitTestCase {

    private static Logger log = Logger.getLogger(WebIntegrationUnitTestCase.class);

    private static String REALM = "JBossTest Servlets";

    @Deployment(name = "jbosstest-web.ear", testable = false)
    public static EnterpriseArchive deployment() {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        String resourcesLocation = "org/jboss/as/test/integration/web/webintegration/resources/";

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "jbosstest-web.ear");
        ear.setApplicationXML(tccl.getResource(resourcesLocation + "ear/application.xml"));
        // ear.addAsApplicationResource(tccl.getResource(resourcesLocation +
        // "ear/jboss-app.xml"), "jboss-app.xml");
        // ear.addAsApplicationResource(tccl.getResource(resourcesLocation +
        // "ear/jboss-structure.xml"), "jboss-structure.xml");

        ear.addAsModule(WebIntegrationBase.webEjbsArchive());
        ear.addAsModule(WebIntegrationBase.jbosstestWebArchive());
        ear.addAsModule(WebIntegrationBase.notJbosstestWebArchive());
        // TODO AS-2067 ear.addAsModule(WebIntegrationBase.relativeArchive());
        ear.addAsModule(WebIntegrationBase.ctsArchive());

        log.info(ear.toString(true));
        return ear;
    }

    /**
     * Access the /jbosstest/APIServlet to test the getRealPath method
     */
    @Test
    public void testRealPath(@ArquillianResource URL base) throws Exception {
        accessURL(new URL(base + "/jbosstest/APIServlet?op=testGetRealPath"));
    }

    /**
     * Access the /jbosstest/APIServlet to test the HttpSessionListener events
     */
    @Test
    public void testHttpSessionListener(@ArquillianResource URL base) throws Exception {
        accessURL(new URL(base + "/jbosstest/APIServlet?op=testSessionListener"));
    }

    /**
     * Access the /jbosstest/ENCServlet
     */
    @Test
    public void testENCServlet(@ArquillianResource URL base) throws Exception {
        accessURL(new URL(base + "/jbosstest/ENCServlet"));
    }

    /**
     * Access the /jbosstest/SimpleServlet to test that servlets in the
     * WEB-INF/lib jar.
     */
    @Test
    public void testServletInJar(@ArquillianResource URL base) throws Exception {
        accessURL(new URL(base + "/jbosstest/SimpleServlet"));
    }

    /**
     * Access the /jbosstest/EntityServlet
     */
    @Test
    public void testEntityServlet(@ArquillianResource URL base) throws Exception {
        accessURL(new URL(base + "/jbosstest/EntityServlet"));
    }

    /**
     * Access the /jbosstest/StatefulSessionServlet
     */
    @Test
    public void testStatefulSessionServlet(@ArquillianResource URL base) throws Exception {
        URL url = new URL(base + "/jbosstest/StatefulSessionServlet");
        accessURL(url);

        // Need a mechanism to force passivation...

        accessURL(url);
    }

    /**
     * Access the /jbosstest/UserTransactionServlet
     */
    @Test
    public void testUserTransactionServlet(@ArquillianResource URL base) throws Exception {
        accessURL(new URL(base + "/jbosstest/UserTransactionServlet"));
    }

    /**
     * Access the /jbosstest/SpeedServlet
     */
    @Test
    public void testSpeedServlet(@ArquillianResource URL base) throws Exception {
        accessURL(new URL(base + "/jbosstest/SpeedServlet"));
    }

    /**
     * Access the /jbosstest/snoop.jsp
     */
    @Test
    public void testSnoopJSP(@ArquillianResource URL base) throws Exception {
        accessURL(new URL(base + "/jbosstest/snoop.jsp"));
    }

    /**
     * Access the /jbosstest/snoop.jsp
     */
    @Test
    public void testSnoopJSPByPattern(@ArquillianResource URL base) throws Exception {
        accessURL(new URL(base + "/jbosstest/test-snoop.snp"));
    }

    /**
     * Access the /jbosstest/test-jsp-mapping
     */
    @Test
    public void testSnoopJSPByMapping(@ArquillianResource URL base) throws Exception {
        accessURL(new URL(base + "/jbosstest/test-jsp-mapping"));
    }

    /**
     * Access the /jbosstest/classpath.jsp
     */
    @Test
    public void testJSPClasspath(@ArquillianResource URL base) throws Exception {
        accessURL(new URL(base + "/jbosstest/classpath.jsp"));
    }

    /**
     * Access the /jbosstest/ClientLoginServlet
     */
    @Test
    public void testClientLoginServlet(@ArquillianResource URL base) throws Exception {
        accessURL(new URL(base + "/jbosstest/ClientLoginServlet"));
    }

    /**
     * Access the /jbosstest/restricted/UserInRoleServlet to test isUserInRole.
     */
    @Test
    public void testUserInRoleServlet(@ArquillianResource URL base) throws Exception {
        URL url = addCredentials(new URL(base + "/jbosstest/restricted/UserInRoleServlet"), "jduke", "theduke");
        HttpResponse response = accessURL(url);

        Header expectedErrors = response.getFirstHeader("X-ExpectedUserRoles-Errors");
        assertTrue("X-ExpectedUserRoles-Errors(" + expectedErrors + ") is null", expectedErrors == null);

        Header unexpectedErrors = response.getFirstHeader("X-UnexpectedUserRoles-Errors");
        assertTrue("X-UnexpectedUserRoles-Errors(" + unexpectedErrors + ") is null", unexpectedErrors == null);
    }

    /**
     * Access the /jbosstest/restricted/SecureServlet
     */
    @Test
    public void testSecureServlet(@ArquillianResource URL base) throws Exception {
        URL url = new URL(base + "/jbosstest/restricted/SecureServlet");
        accessURL(addCredentials(url, "jduke", "theduke"));
    }

    /**
     * Access the /jbosstest/restricted2/SecureServlet
     */
    @Test
    public void testSecureServlet2(@ArquillianResource URL base) throws Exception {
        URL url = new URL(base + "/jbosstest/restricted2/SecureServlet");
        accessURL(addCredentials(url, "jduke", "theduke"));
    }

    /**
     * Access the /jbosstest/restricted/SubjectServlet
     */
    @Test
    public void testSubjectServlet(@ArquillianResource URL base) throws Exception {
        URL url = new URL(base + "/jbosstest/restricted/SubjectServlet");
        HttpResponse response = accessURL(addCredentials(url, "jduke", "theduke"));

        Header servletHeader = response.getFirstHeader("X-SubjectServlet");
        assertTrue("X-SubjectServlet(" + servletHeader + ") is NOT null", servletHeader != null);

        Header filterHeader = response.getFirstHeader("X-SubjectFilter-ENC");
        assertTrue("X-SubjectFilter-ENC(" + filterHeader + ") is NOT null", filterHeader != null);

        Header managerHeader = response.getFirstHeader("X-SubjectFilter-SubjectSecurityManager");
        assertTrue("X-SubjectFilter-SubjectSecurityManager(" + managerHeader + ") is NOT null", managerHeader != null);
    }

    /**
     * Access the /jbosstest/restricted/SecureServlet
     */
    @Test
    public void testSecureServletAndUnsecureAccess(@ArquillianResource URL base) throws Exception {
        log.info("Accessing SecureServlet with valid login");
        accessURL(addCredentials(new URL(base + "/jbosstest/restricted/SecureServlet"), "jduke", "theduke"));

        log.info("Accessing SecureServlet with no login");
        accessURL(new URL(base + "/jbosstest/restricted/UnsecureEJBServlet"), REALM, HTTP_UNAUTHORIZED);
    }

    /**
     * Access the /jbosstest/restricted/SecureServlet
     */
    @Test
    public void testSecureServletWithBadPass(@ArquillianResource URL base) throws Exception {
        URL url = new URL(base + "/jbosstest/restricted/SecureServlet");
        accessURL(WebIntegrationBase.addCredentials(url, "jduke", "badpass"), REALM, HTTP_UNAUTHORIZED);
    }

    /**
     * Access the /jbosstest/restricted/SecureServlet
     */
    @Test
    public void testSecureServletWithNoLogin(@ArquillianResource URL base) throws Exception {
        accessURL(new URL(base + "/jbosstest/restricted/SecureServlet"), REALM, HTTP_UNAUTHORIZED);
    }

    /**
     * Access the /jbosstest-not/unrestricted/SecureServlet
     */
    @Test
    public void testNotJbosstest(@ArquillianResource URL base) throws Exception {
        accessURL(new URL(base + "/jbosstest-not/unrestricted/SecureServlet"), REALM, HTTP_OK);
    }

    /**
     * Access the /jbosstest/restricted/SecuredEntityFacadeServlet
     */
    @Test
    public void testSecuredEntityFacadeServlet(@ArquillianResource URL base) throws Exception {
        accessURL(addCredentials(new URL(base + "/jbosstest/restricted/SecuredEntityFacadeServlet"), "jduke", "theduke"));
    }

    /**
     * Access the /jbosstest/restricted/SecureEJBAccess
     */
    @Test
    public void testSecureEJBAccess(@ArquillianResource URL base) throws Exception {
        accessURL(addCredentials(new URL(base + "/jbosstest/restricted/SecureEJBAccess"), "jduke", "theduke"));
    }

    /**
     * Access the http://{host}/jbosstest/restricted/include_ejb.jsp
     */
    @Test
    public void testIncludeEJB(@ArquillianResource URL base) throws Exception {
        accessURL(addCredentials(new URL(base + "/jbosstest/restricted/include_ejb.jsp"), "jduke", "theduke"));
    }

    /**
     * JBAS-3279: Authenticated user bypass declarative role checks for servlets
     */
    @Test
    public void testUnauthorizedAccess(@ArquillianResource URL base) throws Exception {
        URL url = addCredentials(new URL(base + "/jbosstest/restricted3//SecureServlet"), "jduke", "theduke");
        accessURL(url, REALM, HTTP_FORBIDDEN);

        // BES 2007/02/21 -- %xx encoded '/' is verboten so we now expect 400
        accessURL(new URL(base + "/jbosstest/%2frestricted3//SecureServlet"), REALM, HTTP_BAD_REQUEST);
    }

    /**
     * Access the /jbosstest/UnsecureEJBAccess with method=echo to test that an
     * unsecured servlet cannot access a secured EJB method that requires a
     * valid permission. This should fail.
     */
    @Test
    public void testUnsecureEJBAccess(@ArquillianResource URL base) throws Exception {
        accessURL(new URL(base + "/jbosstest/UnsecureEJBAccess?method=echo"), REALM, HTTP_INTERNAL_ERROR);
    }

    /**
     * Access the /jbosstest/UnsecureEJBAccess with method=unchecked to test
     * that an unsecured servlet can access a secured EJB method that only
     * requires an authenticated user. This requires unauthenticated identity
     * support by the web security domain.
     */
    @Test
    public void testUnsecureAnonEJBAccess(@ArquillianResource URL base) throws Exception {
        accessURL(new URL(base + "/jbosstest/UnsecureEJBAccess?method=unchecked"), REALM, HTTP_OK);
    }

    @Test
    public void testUnsecureRunAsServlet(@ArquillianResource URL base) throws Exception {
        accessURL(new URL(base + "/jbosstest/UnsecureRunAsServlet?method=checkRunAs"), REALM, HTTP_OK);
    }

    /**
     * Access the /jbosstest/UnsecureRunAsServletWithPrincipalName to test that
     * an unsecured servlet can access a secured EJB method by using a run-as
     * role. This should also have a custom run-as principal name.
     */
    @Test
    public void testUnsecureRunAsServletWithPrincipalName(@ArquillianResource URL base) throws Exception {
        String servlet = "/jbosstest/UnsecureRunAsServletWithPrincipalName";
        String target = "ejbName=ejb/UnsecureRunAsServletWithPrincipalNameTarget";
        accessURL(new URL(base + servlet + "?" + target), REALM, HTTP_OK);
    }

    /**
     * Access the /jbosstest/UnsecureRunAsServletWithPrincipalNameAndRoles to
     * test that an unsecured servlet can access a secured EJB method by using a
     * run-as role. This should also have a custom run-as principal name and
     * additional roles.
     */
    @Test
    public void testUnsecureRunAsServletWithPrincipalNameAndRoles(@ArquillianResource URL base) throws Exception {
        String target = "ejbName=ejb/UnsecureRunAsServletWithPrincipalNameAndRolesTarget";
        accessURL(new URL(base + "/jbosstest/UnsecureRunAsServletWithPrincipalNameAndRoles?" + target), REALM, HTTP_OK);
    }

    /**
     * Deploy a second ear that include a notjbosstest-web.war to test ears with
     * the same war names conflicting. Access the
     * /jbosstest-not2/unrestricted/SecureServlet
     */
    @Test
    public void testNotJbosstest2(@ArquillianResource URL base) throws Exception {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        String resourcesLocation = "org/jboss/as/test/integration/web/webintegration/resources/";

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "jbosstest-web2.ear");
        ear.setApplicationXML(tccl.getResource(resourcesLocation + "ear/application2.xml"));
        ear.addAsModule(WebIntegrationBase.notJbosstestWebArchive());
        log.info(ear.toString(true));
        Deployable deployable = new WebIntegrationBase().new Deployable(ear);

        try {
            deployable.deploy();
            URL url = new URL(base + "/jbosstest-not2/unrestricted/SecureServlet");
            accessURL(url, REALM, HttpURLConnection.HTTP_OK);
        } finally {
            deployable.undeploy();
            deployable.delete();
        }
    }

    /**
     * Deploy a bad war and then redploy with a fixed war to test failed war
     * cleanup. Access the /redeploy/index.html
     */
    @Test
    public void testBadWarRedeploy(@ArquillianResource URL base) throws Exception {
        Deployable badDeployable = new WebIntegrationBase().new Deployable(WebIntegrationBase.badWebArchive());
        Deployable goodDeployable = new WebIntegrationBase().new Deployable(WebIntegrationBase.goodWebArchive());
        try {
            badDeployable.deploy();
            fail("The bad-web.war deployment did not fail");
            badDeployable.undeploy();
        } catch (Exception e) {
            log.debug("bad-web.war failed as expected: " + e.getMessage());
        } finally {
            badDeployable.delete();
        }
        try {
            goodDeployable.deploy();
            accessURL(new URL(base + "/redeploy/index.html"), REALM, HTTP_OK);
        } catch (Exception e) {
            fail("The good-web.war deployment failed");
        } finally {
            goodDeployable.undeploy();
            goodDeployable.delete();
        }
    }

    /**
     * Test of a war that accesses classes referred to via the war manifest
     * classpath. Access the /manifest/classpath.jsp
     */
    @Test
    public void testWarManifest(@ArquillianResource URL base) throws Exception {
        Deployable manifestDeployable = new WebIntegrationBase().new Deployable(WebIntegrationBase.manifestArchive());
        try {
            manifestDeployable.deploy();
            HttpResponse response = accessURL(new URL(base + "/manifest/classpath.jsp"));

            Header errors = response.getFirstHeader("X-Exception");
            assertTrue("X-Exception(" + errors + ") is null", errors == null);
        } catch (Exception e) {
            fail("The manifest-web.ear deployment failed");
        } finally {
            manifestDeployable.undeploy();
            manifestDeployable.delete();
        }
    }

    @Test
    public void testBadEarRedeploy(@ArquillianResource URL base) throws Exception {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        String resourcesLocation = "org/jboss/as/test/integration/web/webintegration/resources/";

        EnterpriseArchive badEar = ShrinkWrap.create(EnterpriseArchive.class, "jbosstest-bad.ear");
        badEar.setApplicationXML(tccl.getResource(resourcesLocation + "ear/application-bad.xml"));
        badEar.addAsModule(WebIntegrationBase.badWebArchive());
        log.info(badEar.toString(true));
        Deployable badDeployable = new WebIntegrationBase().new Deployable(badEar);

        EnterpriseArchive goodEar = ShrinkWrap.create(EnterpriseArchive.class, "jbosstest-good.ear");
        goodEar.setApplicationXML(tccl.getResource(resourcesLocation + "ear/application-good.xml"));
        goodEar.addAsModule(WebIntegrationBase.goodWebArchive());
        log.info(goodEar.toString(true));
        Deployable goodDeployable = new WebIntegrationBase().new Deployable(goodEar);

        try {
            badDeployable.deploy();
            fail("The jbosstest-bad.ear deployment did not fail");
        } catch (Exception e) {
            log.debug("jbosstest-bad.ear failed as expected: " + e.getMessage());
        } finally {
            badDeployable.delete();
        }
        try {
            goodDeployable.deploy();
            accessURL(new URL(base + "/redeploy/index.html"), REALM, HTTP_OK);
        } catch (Exception e) {
            fail("The jbosstest-good.ear deployment failed");
        } finally {
            goodDeployable.undeploy();
            goodDeployable.delete();
        }
    }

    /**
     * Validate a war level override of the java2ClassLoadingComplianceOverride
     * flag to true.
     */
    @Test
    public void testJava2ClassLoadingComplianceOverride(@ArquillianResource URL base) throws Exception {
        Deployable classDeployable = new WebIntegrationBase().new Deployable(WebIntegrationBase.classloadingWebArchive());
        try {
            classDeployable.deploy();
            // Load a log4j class
            String target = "class=org.apache.log4j.net.SocketAppender";
            URL url = new URL(base + "/class-loading/ClasspathServlet2?" + target);
            HttpResponse response = accessURL(url, REALM, HTTP_OK);

            // Validate it has not come from the war
            Header cs = response.getFirstHeader("X-CodeSource");
            assertTrue("X-CodeSource(" + cs + ") does not contain war", cs.getValue().indexOf(".war") < 0);
        } catch (Exception e) {
            fail("The class-loading.war deployment failed");
        } finally {
            classDeployable.undeploy();
            classDeployable.delete();
        }
    }

    @Test
    public void testWARWithServletAPIClasses(@ArquillianResource URL base) throws Exception {
        Deployable classesDeployable = new WebIntegrationBase().new Deployable(WebIntegrationBase.servletClassesWebArchive());
        try {
            classesDeployable.deploy();
            // Load a servlet class
            String target = "class=javax.servlet.http.HttpServletResponse";
            URL url = new URL(base + "/servlet-classes/ClasspathServlet2?" + target);
            HttpResponse response = accessURL(url, REALM, HTTP_OK);

            // Validate it has not come from the war
            Header cs = response.getFirstHeader("X-CodeSource");
            assertTrue("X-CodeSource(" + cs + ") does not contain war", cs.getValue().indexOf(".war") < 0);
        } catch (Exception e) {
            fail("The class-loading.war deployment failed");
        } finally {
            classesDeployable.undeploy();
            classesDeployable.delete();
        }
    }

}
