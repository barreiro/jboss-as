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
package org.jboss.as.test.integration.web.ear;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.security.Constants.CODE;
import static org.jboss.as.security.Constants.FLAG;
import static org.jboss.as.security.Constants.LOGIN_MODULES;
import static org.jboss.as.security.Constants.MODULE_OPTIONS;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.security.Constants;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.security.auth.spi.UsersRolesLoginModule;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test case for security domain in a EAR
 * 
 * @author lbarreiro@redhat.com
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(EarMetadataUnitTestCase.SecurityDomainEarDefinitionSetup.class)
public class EarMetadataUnitTestCase {

    protected static Logger log = Logger.getLogger(EarMetadataUnitTestCase.class);

    @ArquillianResource(LoginServlet.class)
    protected URL loginURL;

    @Deployment(name = "jbosstest.war", testable = false)
    public static WebArchive deploymentWar() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "jbosstest.war");
        war.setWebXML(EarMetadataUnitTestCase.class.getPackage(), "web.xml");
        //war.addAsWebInfResource(EarMetadataUnitTestCase.class.getPackage(), "jboss-web.xml", "jboss-web.xml");
        war.addClass(LoginServlet.class);
        log.info(war.toString(true));
        return war;
    }

    @Deployment(name = "jbosstest.ear", testable = false)
    public static EnterpriseArchive deploymentEar() {
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "jbosstest-ear.ear");
        ear.setApplicationXML(EarMetadataUnitTestCase.class.getPackage(), "application.xml");
        ear.addAsApplicationResource(EarMetadataUnitTestCase.class.getPackage(), "jboss-app.xml", "jboss-app.xml");
        ear.addAsModule(deploymentWar());
        log.info(ear.toString(true));
        return ear;
    }

    static class SecurityDomainEarDefinitionSetup implements ServerSetupTask {
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            createSecurityDomains(managementClient.getControllerClient());
        }

        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            removeSecurityDomains(managementClient.getControllerClient());
        }
    }

    private static void createSecurityDomains(ModelControllerClient client) throws Exception {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        String resources = "org/jboss/as/test/integration/web/ear/";

        final List<ModelNode> updates = new ArrayList<ModelNode>();

        updates.add(createOpNode("subsystem=security/security-domain=jbosstest-domain", ADD));

        ModelNode authNode = createOpNode("subsystem=security/security-domain=jbosstest-domain/authentication=classic", ADD);
        authNode.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);

        ModelNode loginModule = authNode.get(Constants.LOGIN_MODULES).add();
        loginModule.get(CODE).set("UsersRoles");
        loginModule.get(FLAG).set("required");

        ModelNode rolesmodule = new ModelNode();
        rolesmodule.get(CODE).set(UsersRolesLoginModule.class.getName());
        rolesmodule.get(FLAG).set("required");
        rolesmodule.get(MODULE_OPTIONS).add("unauthenticatedIdentity", "nobody");
        rolesmodule.get(MODULE_OPTIONS).add("usersProperties", tccl.getResource(resources + "users.properties").getFile());
        rolesmodule.get(MODULE_OPTIONS).add("rolesProperties", tccl.getResource(resources + "roles.properties").getFile());

        authNode.get(LOGIN_MODULES).set(Arrays.asList(rolesmodule));
        updates.add(authNode);

        applyUpdates(updates, client);
    }

    public static void removeSecurityDomains(final ModelControllerClient client) throws Exception {
        final List<ModelNode> updates = new ArrayList<ModelNode>();

        updates.add(createOpNode("subsystem=security/security-domain=jbosstest-domain", REMOVE));

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

    @Test
    @OperateOnDeployment(value = "jbosstest.war")
    public void testWarAccess(@ArquillianResource(value = LoginServlet.class) URL loginURL) throws Exception {
        access(loginURL);
    }

    @Test
    @OperateOnDeployment(value = "jbosstest.ear")
    public void testEarAccess(@ArquillianResource(value = LoginServlet.class) URL loginURL) throws Exception {
        access(loginURL);
    }

    private void access(URL loginURL) throws Exception {
        URL url = new URL(loginURL + "LoginServlet");
        DefaultHttpClient httpclient = new DefaultHttpClient();

        UsernamePasswordCredentials auth = new UsernamePasswordCredentials("user", "strangepass");
        httpclient.getCredentialsProvider().setCredentials(new AuthScope(url.getHost(), url.getPort(), "JBossTest"), auth);

        HttpGet get = new HttpGet(url.toURI());
        HttpResponse response = httpclient.execute(get);
        int statusCode = response.getStatusLine().getStatusCode();
        assertTrue("Expected reply code:" + HTTP_OK + ", actual=" + statusCode, statusCode == HTTP_OK);
        log.info(EntityUtils.toString(response.getEntity()));
    }

}
