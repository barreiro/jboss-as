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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.security.Constants.AUTHENTICATION_JASPI;
import static org.jboss.as.security.Constants.AUTH_MODULE;
import static org.jboss.as.security.Constants.CODE;
import static org.jboss.as.security.Constants.FLAG;
import static org.jboss.as.security.Constants.JSSE;
import static org.jboss.as.security.Constants.LOGIN_MODULE_STACK;
import static org.jboss.as.security.Constants.LOGIN_MODULE_STACK_REF;
import static org.jboss.as.security.Constants.MODULE_OPTIONS;
import static org.jboss.as.security.Constants.SECURITY_DOMAIN;
import static org.jboss.as.security.Constants.TRUSTSTORE_PASSWORD;
import static org.jboss.as.security.Constants.TRUSTSTORE_URL;
import static org.junit.Assert.assertTrue;

import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.test.integration.web.ssl.security.JASPIAuthenticator;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.security.JBossJSSESecurityDomain;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Unit Test the CLIENT-CERT JASPI integration
 *
 * @author Anil.Saldhana@redhat.com
 * @author lbarreiro@redhat.com
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ClientCertJaspiWebUnitTestCase {

    private static Logger log = Logger.getLogger(ClientCertJaspiWebUnitTestCase.class);

    private String baseHttpNoAuth = "http://localhost" + ":" + 8081 + "/";
    private String baseHttpsNoAuth = "https://localhost" + ":" + Integer.getInteger("secureweb.port", 8443) + "/";

    // FIXME Duplicated from org.jboss.as.web.Constants.Constants
    private static String CERTIFICATE_KEY_FILE = "certificate-key-file";
    private static String CONNECTOR = "connector";
    private static String KEY_ALIAS = "key-alias";
    //private static String NAME = "name";
    private static String PASSWORD = "password";
    private static String PROTOCOL = "protocol";
    private static String REDIRECT_PORT = "redirect-port";
    private static String SECURE = "secure";
    private static String SCHEME = "scheme";
    private static String SSL = "ssl";
    private static String VERIFY_CLIENT = "verify-client";

    @Deployment(testable = false)
    public static WebArchive deployment() {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        String resourcesLocation = "org/jboss/as/test/integration/web/ssl/resources/";

        try {
            ModelControllerClient mcc = ModelControllerClient.Factory.create(InetAddress.getByName("localhost"), 9999);
            createSecurityDomains(mcc);
            createHttpRedirectConnectors(mcc);
            createHttpsConnectors(mcc);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        WebArchive war = ShrinkWrap.create(WebArchive.class, "clientcert-jaspi.war");
        war.setWebXML(tccl.getResource(resourcesLocation + "web.xml"));
        war.addAsWebInfResource(tccl.getResource(resourcesLocation + "context.xml"), "context.xml");
        war.addAsWebInfResource(tccl.getResource(resourcesLocation + "jboss-web.xml"), "jboss-web.xml");
        war.addAsWebResource(tccl.getResource(resourcesLocation + "index.html"), "index.html");
        war.addAsWebResource(tccl.getResource(resourcesLocation + "users.properties"), "/WEB-INF/classes/ssl-users.properties");
        war.addAsWebResource(tccl.getResource(resourcesLocation + "roles.properties"), "/WEB-INF/classes/ssl-roles.properties");
        war.addClass(SecureServlet.class);
        war.addPackage(JASPIAuthenticator.class.getPackage());

        System.out.println(war.toString(true));
        return war;
    }

    @AfterClass
    public static void undeployment() {
        try {
            ModelControllerClient mcc = ModelControllerClient.Factory.create(InetAddress.getByName("localhost"), 9999);
            removeHttpRedirectConnectors(mcc);
            removeHttpsConnectors(mcc);
            removeSecurityDomains(mcc);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public static void createSecurityDomains(final ModelControllerClient client) throws Exception {
        final List<ModelNode> updates = new ArrayList<ModelNode>();

        ModelNode op = new ModelNode();
        op.get(OP).set(ADD);

        ModelNode lmstackname = new ModelNode();
        lmstackname.get(NAME).set("lm-stack");

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

        ModelNode lmstackmodules = new ModelNode().set(Arrays.asList(certmodule, rolesmodule));
        ModelNode lmstack = new ModelNode().set(Arrays.asList(lmstackname, lmstackmodules));
        op.get(AUTHENTICATION_JASPI, LOGIN_MODULE_STACK).set(Arrays.asList(lmstack));

        ModelNode amodule = new ModelNode();
        amodule.get(CODE).set("org.jboss.web.tomcat.security.jaspi.modules.HTTPBasicServerAuthModule");
        amodule.get(LOGIN_MODULE_STACK_REF).set("lm-stack");

        op.get(AUTHENTICATION_JASPI, AUTH_MODULE).set(Arrays.asList(amodule));

        ModelNode address = op.get(OP_ADDR);
        address.add(SUBSYSTEM, "security");
        address.add(SECURITY_DOMAIN, "jaspi-test");
        updates.add(op);

        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        String resourcesLocation = "org/jboss/as/test/integration/web/ssl/resources/";
        URL authkeystore = tccl.getResource(resourcesLocation + "keystore/jsse.jks");

        op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(OP_ADDR).add(SUBSYSTEM, "security");
        op.get(OP_ADDR).add(SECURITY_DOMAIN, "jbosstest-ssl");
        ModelNode jsse = op.get(JSSE);
        jsse.get(TRUSTSTORE_PASSWORD).set("changeit");
        jsse.get(TRUSTSTORE_URL).set(authkeystore.getPath());
        updates.add(op);

        URL serverkeystore = tccl.getResource(resourcesLocation + "keystore/server.jks");

        op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(OP_ADDR).add(SYSTEM_PROPERTY, "javax.net.ssl.trustStore");
        op.get(VALUE).set(serverkeystore.getPath());
        updates.add(op);

        op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(OP_ADDR).add(SYSTEM_PROPERTY, "javax.net.ssl.trustStorePassword");
        op.get(VALUE).set("changeit");
        updates.add(op);

        applyUpdates(updates, client);
    }

    public static void removeSecurityDomains(final ModelControllerClient client) throws Exception {
        final List<ModelNode> updates = new ArrayList<ModelNode>();

        ModelNode op = new ModelNode();
        op.get(OP).set(REMOVE);
        op.get(OP_ADDR).add(SUBSYSTEM, "security");
        op.get(OP_ADDR).add(SECURITY_DOMAIN, "jaspi-test");
        updates.add(op);

        op = new ModelNode();
        op.get(OP).set(REMOVE);
        op.get(OP_ADDR).add(SUBSYSTEM, "security");
        op.get(OP_ADDR).add(SECURITY_DOMAIN, "jbosstest-ssl");
        updates.add(op);

        op = new ModelNode();
        op.get(OP).set(REMOVE);
        op.get(OP_ADDR).add(SYSTEM_PROPERTY, "javax.net.ssl.trustStore");
        updates.add(op);

        op = new ModelNode();
        op.get(OP).set(REMOVE);
        op.get(OP_ADDR).add(SYSTEM_PROPERTY, "javax.net.ssl.trustStorePassword");
        updates.add(op);

        applyUpdates(updates, client);
    }

    public static void createHttpsConnectors(final ModelControllerClient client) throws Exception {
        final List<ModelNode> updates = new ArrayList<ModelNode>();

        ModelNode op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(PROTOCOL).set("HTTP/1.1");
        op.get(SECURE).set(true);
        op.get(SCHEME).set("https");
        op.get(SOCKET_BINDING).set("https");

        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        String resourcesLocation = "org/jboss/as/test/integration/web/ssl/resources/";
        URL keystore = tccl.getResource(resourcesLocation + "keystore/server.jks");

        ModelNode ssl = op.get(SSL);
        ssl.get(NAME).set("https");
        ssl.get(PROTOCOL).set("TLS");
        ssl.get(KEY_ALIAS).set("server");
        ssl.get(PASSWORD).set("changeit");
        ssl.get(CERTIFICATE_KEY_FILE).set(keystore.getPath());
        ssl.get(VERIFY_CLIENT).set(true);

        ModelNode address = op.get(OP_ADDR);
        address.add(SUBSYSTEM, "web");
        address.add(CONNECTOR, "https");
        updates.add(op);

        applyUpdates(updates, client);
    }

    public static void removeHttpsConnectors(final ModelControllerClient client) throws Exception {
        final List<ModelNode> updates = new ArrayList<ModelNode>();

        ModelNode op = new ModelNode();
        op.get(OP).set(REMOVE);
        op.get(OP_ADDR).add(SUBSYSTEM, "web");
        op.get(OP_ADDR).add(CONNECTOR, "https");
        updates.add(op);

        applyUpdates(updates, client);
    }

    public static void createHttpRedirectConnectors(final ModelControllerClient client) throws Exception {
        final List<ModelNode> updates = new ArrayList<ModelNode>();

        // A HTTP/1.1 Connector on port 9080 which redirects to 9443 for https
        ModelNode op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(NAME).set("http-redirect");
        op.get(PORT).set(8081);
        op.get(OP_ADDR).add(SOCKET_BINDING_GROUP, "standard-sockets");
        op.get(OP_ADDR).add(SOCKET_BINDING ,"http-redirect");
        updates.add(op);

        op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(PROTOCOL).set("HTTP/1.1");
        op.get(SCHEME).set("http");
        op.get(SOCKET_BINDING).set("http-redirect");
        op.get(REDIRECT_PORT).set(8443);

        op.get(OP_ADDR).add(SUBSYSTEM, "web");
        op.get(OP_ADDR).add(CONNECTOR, "http-redirect");
        updates.add(op);
        applyUpdates(updates, client);
    }

    public static void removeHttpRedirectConnectors(final ModelControllerClient client) throws Exception {
        final List<ModelNode> updates = new ArrayList<ModelNode>();

        ModelNode op = new ModelNode();
        op.get(OP).set(REMOVE);
        op.get(OP_ADDR).add(SUBSYSTEM, "web");
        op.get(OP_ADDR).add(CONNECTOR, "http-redirect");
        updates.add(op);

        op = new ModelNode();
        op.get(OP).set(REMOVE);
        op.get(OP_ADDR).add(SOCKET_BINDING_GROUP, "standard-sockets");
        op.get(OP_ADDR).add(SOCKET_BINDING, "http-redirect");
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

    @Test
    public void testJASPIClientCert() throws Exception {
        // Start by accessing the secured index.html
        log.info("+++ testJASPIClientCert");
        doHttps(new URL(baseHttpsNoAuth + "clientcert-jaspi/unrestricted/SecureServlet"));
    }

    /**
     * Test that access of the transport constrained redirects to the ssl
     * connector
     *
     * @throws Exception
     */
    @Test
    public void testHttpRedirect() throws Exception {
        // Access of the transport constrained redirects to the ssl connector
        log.info("+++ testHttpRedirect");
        doHttps(new URL(baseHttpNoAuth + "clientcert-jaspi/unrestricted/SecureServlet"));
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

    /* Add ssl capabilities to httpClient
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
            ccm.getSchemeRegistry().register(new Scheme("https", Integer.getInteger("secureweb.port", 8443), ssf));

            return new DefaultHttpClient(ccm, base.getParams());
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
