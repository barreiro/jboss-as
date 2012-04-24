/*
 * JBoss, a division of Red Hat
 * Copyright 2006, Red Hat Middleware, LLC, and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.test.integration.web.sso;

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

import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.test.integration.web.sso.interfaces.StatelessSession;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests of web app single sign-on
 * 
 * @author Scott.Stark@jboss.org
 * @author lbarreiro@redhat.com
 */
@RunWith(Arquillian.class)
@RunAsClient
//@Ignore(value="ARQ-791 Arquillian is unable to reconnect to JMX server if the connection is lost")
public class SingleSignOnUnitTestCase {
    
    private static Logger log = Logger.getLogger(SingleSignOnUnitTestCase.class);

    @ArquillianResource
    protected URL baseURLNoAuth;

    /**
     * One time setup for all SingleSignOnUnitTestCase unit tests
     */
    @Deployment(name = "web-sso.ear", testable = false)
    public static EnterpriseArchive deployment() {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        String resourcesLocation = "org/jboss/as/test/integration/web/sso/resources/";

        try {
            ModelControllerClient mcc = ModelControllerClient.Factory.create(InetAddress.getByName("localhost"), 9999);
            createSecurityDomains(mcc);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        
        WebArchive war1 = SingleSignOnUnitTestCase.createSsoWar("sso-form-auth1.war");
        WebArchive war2 = SingleSignOnUnitTestCase.createSsoWar("sso-form-auth2.war");
        WebArchive warNoA = SingleSignOnUnitTestCase.createSsoWar("sso-with-no-auth.war");
        
        // Use jboss-web-no-auth.xml so the war will not have an authenticator
        warNoA.delete(warNoA.get("WEB-INF/jboss-web.xml").getPath());
        warNoA.addAsWebInfResource(tccl.getResource(resourcesLocation + "jboss-web-no-auth.xml"), "jboss-web.xml");
        
        JavaArchive webEjbs = ShrinkWrap.create(JavaArchive.class, "jbosstest-web-ejbs.jar");

        webEjbs.addAsResource(tccl.getResource(resourcesLocation + "users.properties"), "users.properties");
        webEjbs.addAsResource(tccl.getResource(resourcesLocation + "roles.properties"), "roles.properties");
        
        webEjbs.addAsManifestResource(tccl.getResource(resourcesLocation + "ejb-jar.xml"), "ejb-jar.xml");
        webEjbs.addAsManifestResource(tccl.getResource(resourcesLocation + "jboss.xml"), "jboss.xml");
        
        webEjbs.addPackage(StatelessSession.class.getPackage());

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "web-sso.ear");
        ear.setApplicationXML(tccl.getResource(resourcesLocation + "application.xml"));       
        ear.addAsApplicationResource(tccl.getResource(resourcesLocation + "users.properties"), "/WEB-INF/classes/users.properties");
        ear.addAsApplicationResource(tccl.getResource(resourcesLocation + "roles.properties"), "/WEB-INF/classes/roles.properties");
        
        ear.addAsModule(war1);
        ear.addAsModule(war2);
        ear.addAsModule(warNoA);
        ear.addAsModule(webEjbs);

        log.info(war1.toString(true));
        log.info(war2.toString(true));
        log.info(warNoA.toString(true));
        log.info(webEjbs.toString(true));
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

        ModelNode op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(OP_ADDR).add(SUBSYSTEM, "security");
        op.get(OP_ADDR).add(SECURITY_DOMAIN, "jbosstest-web");

        ModelNode rolesmodule = new ModelNode();
        rolesmodule.get(CODE).set("org.jboss.security.auth.spi.UsersRolesLoginModule");
        rolesmodule.get(FLAG).set("required");
        rolesmodule.get(MODULE_OPTIONS).add("unauthenticatedIdentity", "nobody");
        rolesmodule.get(MODULE_OPTIONS).add("usersProperties", "users.properties");
        rolesmodule.get(MODULE_OPTIONS).add("rolesProperties", "roles.properties");

        op.get(AUTHENTICATION).set(Arrays.asList(rolesmodule));
        updates.add(op);

        applyUpdates(updates, client);
    }

    public static void removeSecurityDomains(final ModelControllerClient client) throws Exception {
        final List<ModelNode> updates = new ArrayList<ModelNode>();

        ModelNode op = new ModelNode();
        op.get(OP).set(REMOVE);
        op.get(OP_ADDR).add(SUBSYSTEM, "security");
        op.get(OP_ADDR).add(SECURITY_DOMAIN, "jbosstest-web");
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

    private static WebArchive createSsoWar(String warName) {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        String resourcesLocation = "org/jboss/as/test/integration/web/sso/resources/";
        
        WebArchive war = ShrinkWrap.create(WebArchive.class, warName);
        war.setWebXML(tccl.getResource(resourcesLocation + "web-form-auth.xml"));
        war.addAsWebInfResource(tccl.getResource(resourcesLocation + "jboss-web.xml"), "jboss-web.xml");

        war.addAsWebResource(tccl.getResource(resourcesLocation + "error.html"), "error.html");
        war.addAsWebResource(tccl.getResource(resourcesLocation + "index.html"), "index.html");
        war.addAsWebResource(tccl.getResource(resourcesLocation + "index.jsp"), "index.jsp");
        war.addAsWebResource(tccl.getResource(resourcesLocation + "login.html"), "login.html");
        
        war.addClass(EJBServlet.class);
        war.addClass(LogoutServlet.class);
        war.addPackage(StatelessSession.class.getPackage());
        
        return war;
    }

    /**
     * Test single sign-on across two web apps using form based auth
     * 
     * @throws Exception
     */
    @Test
    public void testFormAuthSingleSignOn() throws Exception {
        log.info("+++ testFormAuthSingleSignOn");
        SSOBaseCase.executeFormAuthSingleSignOnTest(baseURLNoAuth, baseURLNoAuth, log);
    }

    /**
     * Test single sign-on across two web apps using form based auth
     * 
     * @throws Exception
     */
    @Test
    public void testNoAuthSingleSignOn() throws Exception {
        log.info("+++ testNoAuthSingleSignOn");
        SSOBaseCase.executeNoAuthSingleSignOnTest(baseURLNoAuth, baseURLNoAuth, log);
    }

}
