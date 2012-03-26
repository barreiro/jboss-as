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
package org.jboss.as.test.integration.web.ssl;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.web.Constants.CA_CERTIFICATE_FILE;
import static org.jboss.as.web.Constants.CERTIFICATE_KEY_FILE;
import static org.jboss.as.web.Constants.CERTIFICATE_FILE;
import static org.jboss.as.web.Constants.KEY_ALIAS;
import static org.jboss.as.web.Constants.PASSWORD;
import static org.jboss.as.web.Constants.PROTOCOL;
import static org.jboss.as.web.Constants.REDIRECT_PORT;
import static org.jboss.as.web.Constants.SECURE;
import static org.jboss.as.web.Constants.SCHEME;
import static org.jboss.as.web.Constants.VERIFY_CLIENT;
import static org.jboss.as.security.Constants.AUTH_MODULES;
import static org.jboss.as.security.Constants.CODE;
import static org.jboss.as.security.Constants.FLAG;
import static org.jboss.as.security.Constants.KEYSTORE;
import static org.jboss.as.security.Constants.LOGIN_MODULE_STACK_REF;
import static org.jboss.as.security.Constants.LOGIN_MODULES;
import static org.jboss.as.security.Constants.MODULE_OPTIONS;
import static org.jboss.as.security.Constants.SERVER_ALIAS;
import static org.jboss.as.security.Constants.TRUSTSTORE;
import static org.jboss.as.security.Constants.URL;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;
import static org.junit.Assert.assertTrue;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.test.integration.web.ssl.security.JASPIAuthenticator;
import org.jboss.as.test.shared.RetryTaskExecutor;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.security.JBossJSSESecurityDomain;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Unit Test the CLIENT-CERT JASPI integration.
 * 
 * This test currently is not available for OpenSSL keys (used when native libs
 * are on)
 * 
 * @author Anil.Saldhana@redhat.com
 * @author lbarreiro@redhat.com
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(ClientCertJaspiWebUnitTestCase.ClientCertJaspiWebUnitTestCaseSetup.class)
public class ClientCertJaspiWebUnitTestCase {

    private static final int HTTP_REDIRECT_PORT = 8081;

    private static Logger log = Logger.getLogger(ClientCertJaspiWebUnitTestCase.class);

    @ArquillianResource
    private URL baseUrl;

    static class ClientCertJaspiWebUnitTestCaseSetup implements ServerSetupTask {

        private static boolean webNative;
        private static int httpsPort;

        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            webNative = getNative(managementClient.getControllerClient());
            httpsPort = getSocketBinding(managementClient.getControllerClient(), "https");

            ClientCertJaspiWebUnitTestCase.createSecurityDomains(managementClient.getControllerClient());
            ClientCertJaspiWebUnitTestCase.createHttpRedirectConnectors(managementClient.getControllerClient(), httpsPort);
            ClientCertJaspiWebUnitTestCase.createHttpsConnectors(managementClient.getControllerClient());

            // Restart required to make the ssl properties effective
            ClientCertJaspiWebUnitTestCase.restartServer(managementClient.getControllerClient());
        }

        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            ClientCertJaspiWebUnitTestCase.removeHttpsConnectors(managementClient.getControllerClient());
            ClientCertJaspiWebUnitTestCase.removeHttpRedirectConnectors(managementClient.getControllerClient());
            ClientCertJaspiWebUnitTestCase.removeSecurityDomains(managementClient.getControllerClient());
        }
    }

    @Deployment(testable = false)
    public static WebArchive deployment() {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        String resourcesLocation = "org/jboss/as/test/integration/web/ssl/resources/";

        WebArchive war = ShrinkWrap.create(WebArchive.class, "clientcert-jaspi.war");
        war.setWebXML(tccl.getResource(resourcesLocation + "web.xml"));
        war.addAsWebInfResource(tccl.getResource(resourcesLocation + "jboss-web.xml"), "jboss-web.xml");
        war.addAsWebResource(tccl.getResource(resourcesLocation + "index.html"), "index.html");
        war.addAsWebResource(tccl.getResource(resourcesLocation + "users.properties"), "/WEB-INF/classes/ssl-users.properties");
        war.addAsWebResource(tccl.getResource(resourcesLocation + "roles.properties"), "/WEB-INF/classes/ssl-roles.properties");
        war.addClass(SecureServlet.class);
        war.addPackage(JASPIAuthenticator.class.getPackage());

        log.info(war.toString(true));
        return war;
    }

    public static void createSecurityDomains(final ModelControllerClient client) throws Exception {
        final List<ModelNode> updates = new ArrayList<ModelNode>();
        final String sdNode = "subsystem=security/security-domain=jaspi-test";

        updates.add(createOpNode(sdNode, ADD));

        ModelNode jaspiAuth = createOpNode(sdNode + "/authentication=jaspi", ADD);

        ModelNode amodule = new ModelNode();
        amodule.get(CODE).set("org.jboss.web.tomcat.security.jaspi.modules.HTTPBasicServerAuthModule");
        amodule.get(LOGIN_MODULE_STACK_REF).set("lm-stack");

        jaspiAuth.get(AUTH_MODULES).set(Arrays.asList(amodule));
        updates.add(jaspiAuth);

        ModelNode jaspiStack = createOpNode(sdNode + "/authentication=jaspi/login-module-stack=lm-stack", ADD);

        ModelNode certmodule = new ModelNode();
        certmodule.get(CODE).set("org.jboss.security.auth.spi.BaseCertLoginModule");
        certmodule.get(FLAG).set("required");
        certmodule.get(MODULE_OPTIONS).add("password-stacking", "useFirstPass");
        certmodule.get(MODULE_OPTIONS).add("securityDomain", "java:/jaas/jbosstest-ssl");

        ModelNode rolesmodule = new ModelNode();
        rolesmodule.get(CODE).set("org.jboss.security.auth.spi.UsersRolesLoginModule");
        rolesmodule.get(FLAG).set("required");
        rolesmodule.get(MODULE_OPTIONS).add("password-stacking", "useFirstPass");
        rolesmodule.get(MODULE_OPTIONS).add("usersProperties", "ssl-users.properties");
        rolesmodule.get(MODULE_OPTIONS).add("rolesProperties", "ssl-roles.properties");
        rolesmodule.get(MODULE_OPTIONS).add("roleGroupSeperator", ":");

        jaspiStack.get(LOGIN_MODULES).set(Arrays.asList(certmodule, rolesmodule));
        updates.add(jaspiStack);

        // The java keys are used to authenticate both whith natives and without
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        String resourcesLocation = "org/jboss/as/test/integration/web/ssl/resources/";
        URL authkeystore = tccl.getResource(resourcesLocation + "keystore/jsse.jks");
        URL serverkeystore = tccl.getResource(resourcesLocation + "keystore/server.jks");

        updates.add(createOpNode("subsystem=security/security-domain=jbosstest-ssl", ADD));

        ModelNode jsse = createOpNode("subsystem=security/security-domain=jbosstest-ssl/jsse=classic", ADD);
        jsse.get(KEYSTORE, PASSWORD).set("changeit");
        jsse.get(KEYSTORE, URL).set(serverkeystore.getPath());
        jsse.get(SERVER_ALIAS).set("server");
        jsse.get(TRUSTSTORE, PASSWORD).set("changeit");
        jsse.get(TRUSTSTORE, URL).set(authkeystore.getPath());
        jsse.get(VERIFY_CLIENT).set(true);
        updates.add(jsse);

        // Truststore is not required on natives
        if (!ClientCertJaspiWebUnitTestCaseSetup.webNative) {
        ModelNode tsOp = createOpNode("system-property=javax.net.ssl.trustStore", ADD);
        tsOp.get(VALUE).set(serverkeystore.getPath());
        updates.add(tsOp);

        ModelNode tspOp = createOpNode("system-property=javax.net.ssl.trustStorePassword", ADD);
        tspOp.get(VALUE).set("changeit");
        updates.add(tspOp);
        }

        applyUpdates(updates, client);
    }

    public static void removeSecurityDomains(final ModelControllerClient client) throws Exception {
        final List<ModelNode> updates = new ArrayList<ModelNode>();

        updates.add(createOpNode("subsystem=security/security-domain=jaspi-test", REMOVE));
        updates.add(createOpNode("subsystem=security/security-domain=jbosstest-ssl", REMOVE));
        if (!ClientCertJaspiWebUnitTestCaseSetup.webNative) {
            updates.add(createOpNode("system-property=javax.net.ssl.trustStore", REMOVE));
            updates.add(createOpNode("system-property=javax.net.ssl.trustStorePassword", REMOVE));
        }
        applyUpdates(updates, client);
    }

    public static void createHttpsConnectors(final ModelControllerClient client) throws Exception {
        final List<ModelNode> updates = new ArrayList<ModelNode>();

        ModelNode op = createOpNode("subsystem=web/connector=https", ADD);
        op.get(PROTOCOL).set("HTTP/1.1");
        op.get(SECURE).set(true);
        op.get(SCHEME).set("https");
        op.get(SOCKET_BINDING).set("https");
        updates.add(op);

        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        String resourcesLocation = "org/jboss/as/test/integration/web/ssl/resources/";
        URL keystore = tccl.getResource(resourcesLocation + "keystore/server.jks");
        URL openSslKeystore = tccl.getResource(resourcesLocation + "keystore/server.pem");
        URL openSslCertstore = tccl.getResource(resourcesLocation + "keystore/server-cert.pem");

        // SSL element name must be 'configuration'
        ModelNode ssl = createOpNode("subsystem=web/connector=https/ssl=configuration", ADD);
        ssl.get(PROTOCOL).set("ALL");
        ssl.get(KEY_ALIAS).set("server");
        ssl.get(PASSWORD).set("changeit");
        ssl.get(VERIFY_CLIENT).set(true);
        if (ClientCertJaspiWebUnitTestCaseSetup.webNative) {
            ssl.get(CERTIFICATE_KEY_FILE).set(openSslKeystore.getPath());
            ssl.get(CERTIFICATE_FILE).set(openSslCertstore.getPath());
            ssl.get(CA_CERTIFICATE_FILE).set(openSslCertstore.getPath());
        } else {
            ssl.get(CERTIFICATE_KEY_FILE).set(keystore.getPath());
        }
        updates.add(ssl);

        applyUpdates(updates, client);
    }

    public static void removeHttpsConnectors(final ModelControllerClient client) throws Exception {
        final List<ModelNode> updates = new ArrayList<ModelNode>();

        updates.add(createOpNode("subsystem=web/connector=https", REMOVE));

        applyUpdates(updates, client);
    }

    public static void createHttpRedirectConnectors(final ModelControllerClient client, int targetPort) throws Exception {
        final List<ModelNode> updates = new ArrayList<ModelNode>();

        // A HTTP/1.1 Connector on port 8081 which redirects to https
        ModelNode op = createOpNode("socket-binding-group=standard-sockets/socket-binding=http-redirect", ADD);
        op.get(NAME).set("http-redirect");
        op.get(PORT).set(HTTP_REDIRECT_PORT);
        updates.add(op);

        op = createOpNode("subsystem=web/connector=http-redirect", ADD);
        op.get(PROTOCOL).set("HTTP/1.1");
        op.get(SCHEME).set("http");
        op.get(SOCKET_BINDING).set("http-redirect");
        op.get(REDIRECT_PORT).set(targetPort);
        updates.add(op);

        applyUpdates(updates, client);
    }

    public static void removeHttpRedirectConnectors(final ModelControllerClient client) throws Exception {
        final List<ModelNode> updates = new ArrayList<ModelNode>();

        updates.add(createOpNode("subsystem=web/connector=http-redirect", REMOVE));
        updates.add(createOpNode("socket-binding-group=standard-sockets/socket-binding=http-redirect", REMOVE));

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
     * Method to get the status of the native attribute of the web subsystem.
     * The configuration of HTTPS connector is different with/without natives.
     */
    public static boolean getNative(final ModelControllerClient client) throws Exception {
        ModelNode op = createOpNode("subsystem=web", READ_ATTRIBUTE_OPERATION);
        op.get(NAME).set("native");
        log.info("+++ Update on " + client + ":\n" + op.toString());
        ModelNode result = client.execute(new OperationBuilder(op).build());
        if (result.hasDefined("outcome") && "success".equals(result.get("outcome").asString())) {
            if (result.hasDefined("result")) {
                log.info("Native: " + result.get("result").asBoolean());
                return result.get("result").asBoolean();
            } else
                throw new RuntimeException("Operation successful but no result.");
        } else if (result.hasDefined("failure-description")) {
            throw new RuntimeException(result.get("failure-description").toString());
        } else {
            throw new RuntimeException("Operation not successful; outcome = " + result.get("outcome"));
        }
    }

    /**
     * Method to fetch the port number for a given socket binding. In this test
     * it's used to get the default HTTPS port to use in the redirect connector
     */
    public static int getSocketBinding(final ModelControllerClient client, String binding) throws Exception {
        ModelNode op = createOpNode("socket-binding-group=standard-sockets/socket-binding=" + binding, READ_ATTRIBUTE_OPERATION);
        op.get(NAME).set("port");
        log.info("+++ Update on " + client + ":\n" + op.toString());
        ModelNode result = client.execute(new OperationBuilder(op).build());
        if (result.hasDefined("outcome") && "success".equals(result.get("outcome").asString())) {
            if (result.hasDefined("result")) {
                log.info("Port for binding '" + binding + "': " + result.get("result").asInt());
                return result.get("result").asInt();
            } else
                throw new RuntimeException("Operation successful but no result.");
        } else if (result.hasDefined("failure-description")) {
            throw new RuntimeException(result.get("failure-description").toString());
        } else {
            throw new RuntimeException("Operation not successful; outcome = " + result.get("outcome"));
        }
    }

    // Reload operation is not handled well by Arquillian
    // See ARQ-791: JMX: Arquillian is unable to reconnect to JMX server if the
    // connection is lost
    private static void restartServer(final ModelControllerClient client) {
        try {
            applyUpdates(Arrays.asList(createOpNode(null, "reload")), client);
        } catch (Exception e) {
            throw new RuntimeException("Restart operation not successful. " + e.getMessage());
        }
        try {
            RetryTaskExecutor<Boolean> rte = new RetryTaskExecutor<Boolean>();
            rte.retryTask(new Callable<Boolean>() {
                public Boolean call() throws Exception {
                    ModelNode result = client.execute(new OperationBuilder(createOpNode(null, READ_RESOURCE_OPERATION)).build());
                    if (result.hasDefined("outcome") && "success".equals(result.get("outcome").asString())) {
                        if (result.hasDefined("result"))
                            log.info(result.get("result"));
                        return true;
                    } else {
                        log.info("Server is down.");
                        throw new Exception("Connector not available.");
                    }
                }
            });
        } catch (TimeoutException e) {
            throw new RuntimeException("Timeout on restart operation. " + e.getMessage());
        }
        log.info("Server is up.");
    }

    /**
     * Test the access to the ssl connector
     */
    @Test
    public void testJASPIClientCert() throws Exception {
        // Start by accessing the secured index.html
        log.info("+++ testJASPIClientCert");
        int httpsPort = ClientCertJaspiWebUnitTestCaseSetup.httpsPort;
        doHttps(new URL(changeUrlPort(baseUrl, httpsPort, true), "unrestricted/SecureServlet"));
    }

    /**
     * Test access of the transport constrained redirects to the ssl connector
     */
    @Test
    public void testHttpRedirect() throws Exception {
        // Access of the transport constrained redirects to the ssl connector
        log.info("+++ testHttpRedirect");
        doHttps(new URL(changeUrlPort(baseUrl, HTTP_REDIRECT_PORT, false), "unrestricted/SecureServlet"));
    }

    private URL changeUrlPort(URL base, int port, boolean secure) throws MalformedURLException {
        // Also adds an s to the end of a secure protocol scheme
        return new URL(base.getProtocol() + (secure ? "s" : ""), base.getHost(), port, base.getFile());
    }

    public void doHttps(URL httpsNoAuth) throws Exception {
        log.info("+++ doHttps URL=" + httpsNoAuth);

        HttpGet httpget = new HttpGet(httpsNoAuth.toURI());
        HttpClient httpclient = new DefaultHttpClient();
        httpclient = wrapClient(httpclient, "client");

        log.info("Executing request" + httpget.getRequestLine());
        HttpResponse response = httpclient.execute(httpget);

        int statusCode = response.getStatusLine().getStatusCode();
        Header[] errorHeaders = response.getHeaders("X-Exception");

        log.info("Got status '" + response.getStatusLine().getReasonPhrase() + "'");

        assertTrue("Wrong response code: " + statusCode, statusCode == HttpURLConnection.HTTP_OK);
        assertTrue("X-Exception(" + Arrays.toString(errorHeaders) + ") is null", errorHeaders.length == 0);
    }

    /**
     * Add ssl capabilities to httpClient
     */
    public static HttpClient wrapClient(HttpClient base, String alias) {
        try {
            ClassLoader tccl = Thread.currentThread().getContextClassLoader();
            String resourcesLocation = "org/jboss/as/test/integration/web/ssl/resources/";
            URL keystore = tccl.getResource(resourcesLocation + "keystore/client.jks");

            JBossJSSESecurityDomain jsseSecurityDomain = new JBossJSSESecurityDomain("client-cert");
            jsseSecurityDomain.setKeyStoreURL(keystore.getPath());
            jsseSecurityDomain.setKeyStorePassword("changeit");
            jsseSecurityDomain.setClientAlias(alias);
            jsseSecurityDomain.setTrustStoreURL(keystore.getPath());
            jsseSecurityDomain.setTrustStorePassword("changeit");
            jsseSecurityDomain.reloadKeyAndTrustStore();

            KeyManager[] keyManagers = jsseSecurityDomain.getKeyManagers();
            TrustManager[] trustManagers = jsseSecurityDomain.getTrustManagers();

            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(keyManagers, trustManagers, null);
            SSLSocketFactory ssf = new SSLSocketFactory(ctx, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            ClientConnectionManager ccm = base.getConnectionManager();
            ccm.getSchemeRegistry().register(new Scheme("https", ClientCertJaspiWebUnitTestCaseSetup.httpsPort, ssf));

            return new DefaultHttpClient(ccm, base.getParams());
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
