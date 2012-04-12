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

import static java.net.HttpURLConnection.HTTP_OK;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ARCHIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNDEPLOY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.security.Constants.CODE;
import static org.jboss.as.security.Constants.FLAG;
import static org.jboss.as.security.Constants.LOGIN_MODULES;
import static org.jboss.as.security.Constants.MODULE_OPTIONS;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
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
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.test.integration.web.webintegration.interfaces.StatelessSession;
import org.jboss.as.test.integration.web.webintegration.interfaces.StatelessSessionBean;
import org.jboss.as.test.integration.web.webintegration.interfaces.StatelessSessionHome;
import org.jboss.as.test.integration.web.webintegration.interfaces.StatelessSessionLocal;
import org.jboss.as.test.integration.web.webintegration.interfaces.StatelessSessionLocalHome;
import org.jboss.as.test.integration.web.webintegration.servlets.ClasspathServlet2;
import org.jboss.as.test.integration.web.webintegration.servlets.SecureServlet;
import org.jboss.as.test.integration.web.webintegration.servlets.SimpleServlet;
import org.jboss.as.test.integration.web.webintegration.servlets.SubjectServlet;
import org.jboss.as.test.integration.web.webintegration.util.ClassInClasses;
import org.jboss.as.test.integration.web.webintegration.util.Debug;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.security.auth.spi.UsersRolesLoginModule;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.impl.base.exporter.zip.ZipExporterImpl;
import org.jboss.shrinkwrap.resolver.api.DependencyResolvers;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;

/**
 * Tests of servlet container integration into the JBoss server.
 * 
 * @author lbarreiro@redhat.com
 */
public class WebIntegrationBase {

    private static Logger log = Logger.getLogger(WebIntegrationBase.class);

    static class WebIntegrationSetup implements ServerSetupTask {
        static ModelControllerClient client;

        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            client = managementClient.getControllerClient();

            createSecurityDomains(client);
            createMessageDestinations(client);
        }

        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            removeMessageDestinations(client);
            removeSecurityDomains(client);
        }
    }

    static class WebIntegrationOptimizedSetup extends WebIntegrationSetup implements ServerSetupTask {
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            super.setup(managementClient, containerId);

            // Set pass by reference (required by Optimized EJBs)
            setPassByValue(client, false);
        }

        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            super.tearDown(managementClient, containerId);

            setPassByValue(client, true);
        }
    }

    private static void createSecurityDomains(ModelControllerClient client) throws Exception {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        String rLocation = "org/jboss/as/test/integration/web/webintegration/resources/";

        final List<ModelNode> updates = new ArrayList<ModelNode>();

        updates.add(createOpNode("subsystem=security/security-domain=jbosstest-domain", ADD));

        ModelNode authNode = createOpNode("subsystem=security/security-domain=jbosstest-domain/authentication=classic", ADD);
        authNode.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);

        ModelNode rolesmodule = new ModelNode();
        rolesmodule.get(CODE).set(UsersRolesLoginModule.class.getName());
        rolesmodule.get(FLAG).set("required");
        rolesmodule.get(MODULE_OPTIONS).add("unauthenticatedIdentity", "nobody");
        rolesmodule.get(MODULE_OPTIONS).add("usersProperties", tccl.getResource(rLocation + "users.properties").getFile());
        rolesmodule.get(MODULE_OPTIONS).add("rolesProperties", tccl.getResource(rLocation + "roles.properties").getFile());

        authNode.get(LOGIN_MODULES).set(Arrays.asList(rolesmodule));
        updates.add(authNode);

        applyUpdates(updates, client);
    }

    public static void removeSecurityDomains(final ModelControllerClient client) throws Exception {
        final List<ModelNode> updates = new ArrayList<ModelNode>();

        updates.add(createOpNode("subsystem=security/security-domain=jbosstest-domain", REMOVE));

        applyUpdates(updates, client);
    }

    private static void createMessageDestinations(ModelControllerClient client) throws Exception {
        final List<ModelNode> updates = new ArrayList<ModelNode>();

        ModelNode topic = createOpNode("subsystem=messaging/hornetq-server=default/jms-topic=TestTopic", ADD);
        topic.get("entries").add("topic/testTopic");
        updates.add(topic);

        ModelNode queue = createOpNode("subsystem=messaging/hornetq-server=default/jms-queue=TestQueue", ADD);
        queue.get("entries").add("queue/testQueue");
        updates.add(queue);

        ModelNode queueA = createOpNode("subsystem=messaging/hornetq-server=default/jms-queue=A", ADD);
        queueA.get("entries").add("queue/A");
        updates.add(queueA);

        applyUpdates(updates, client);
    }

    private static void removeMessageDestinations(ModelControllerClient client) throws Exception {
        final List<ModelNode> updates = new ArrayList<ModelNode>();

        updates.add(createOpNode("subsystem=messaging/hornetq-server=default/jms-queue=A", REMOVE));
        updates.add(createOpNode("subsystem=messaging/hornetq-server=default/jms-queue=TestQueue", REMOVE));
        updates.add(createOpNode("subsystem=messaging/hornetq-server=default/jms-topic=TestTopic", REMOVE));

        applyUpdates(updates, client);
    }

    private static void setPassByValue(ModelControllerClient client, boolean passByValue) throws Exception {
        final List<ModelNode> updates = new ArrayList<ModelNode>();

        ModelNode op = createOpNode("subsystem=ejb3", WRITE_ATTRIBUTE_OPERATION);
        op.get(NAME).set("in-vm-remote-interface-invocation-pass-by-value");
        op.get(VALUE).set(passByValue);
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

    // Archives ---------------------------------------------------------------

    public static JavaArchive webEjbsArchive() {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        String resourcesLocation = "org/jboss/as/test/integration/web/webintegration/resources/";

        JavaArchive webEjbs = ShrinkWrap.create(JavaArchive.class, "jbosstest-web-ejbs.jar");

        webEjbs.addAsManifestResource(tccl.getResource(resourcesLocation + "ejb/ejb-jar.xml"), "ejb-jar.xml");
        webEjbs.addAsManifestResource(tccl.getResource(resourcesLocation + "ejb/jboss.xml"), "jboss.xml");
        webEjbs.addAsManifestResource(tccl.getResource(resourcesLocation + "ejb/jboss-ejb3.xml"), "jboss-ejb3.xml");

        webEjbs.addPackage(StatelessSession.class.getPackage());

        log.info(webEjbs.toString(true));
        return webEjbs;
    }

    public static WebArchive jbosstestWebArchive() {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        String resourcesLocation = "org/jboss/as/test/integration/web/webintegration/resources/";

        WebArchive war = ShrinkWrap.create(WebArchive.class, "jbosstest-web.war");
        war.setWebXML(tccl.getResource(resourcesLocation + "war/jbosstest-web.xml"));
        war.addAsWebInfResource(tccl.getResource(resourcesLocation + "war/jboss-web.xml"), "jboss-web.xml");

        war.addAsWebResource(tccl.getResource(resourcesLocation + "html/classpath.jsp"), "classpath.jsp");
        war.addAsWebResource(tccl.getResource(resourcesLocation + "html/index.html"), "index.html");
        war.addAsWebResource(tccl.getResource(resourcesLocation + "html/snoop.jsp"), "snoop.jsp");
        war.addAsWebResource(tccl.getResource(resourcesLocation + "html/ejb.jsp"), "restricted/ejb.jsp");
        war.addAsWebResource(tccl.getResource(resourcesLocation + "html/include_ejb.jsp"), "restricted/include_ejb.jsp");

        war.addPackage(SubjectServlet.class.getPackage());
        war.addPackage(ClassInClasses.class.getPackage());

        log.info(war.toString(true));
        return war;
    }

    public static WebArchive notJbosstestWebArchive() {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        String resourcesLocation = "org/jboss/as/test/integration/web/webintegration/resources/";

        WebArchive notWar = ShrinkWrap.create(WebArchive.class, "notjbosstest-web.war");
        notWar.setWebXML(tccl.getResource(resourcesLocation + "war/notjbosstest-web.xml"));
        notWar.addAsWebInfResource(tccl.getResource(resourcesLocation + "war/notjboss-web.xml"), "jboss-web.xml");

        notWar.addAsWebResource(tccl.getResource(resourcesLocation + "html/notindex.html"), "index.html");
        notWar.addAsWebResource(tccl.getResource(resourcesLocation + "html/error.html"), "restricted/error.html");
        notWar.addAsWebResource(tccl.getResource(resourcesLocation + "html/login.html"), "restricted/login.html");

        notWar.addClass(SecureServlet.class);

        log.info(notWar.toString(true));
        return notWar;
    }

    public static JavaArchive relativeArchive() {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        String resourcesLocation = "org/jboss/as/test/integration/web/webintegration/resources/";

        JavaArchive relative = ShrinkWrap.create(JavaArchive.class, "websubdir/relative.jar");
        relative.addAsManifestResource(tccl.getResource(resourcesLocation + "ejb/subdir-ejb-jar.xml"), "ejb-jar.xml");

        relative.addClass(StatelessSessionBean.class);
        relative.addClass(StatelessSession.class);
        relative.addClass(StatelessSessionHome.class);
        relative.addClass(StatelessSessionLocal.class);
        relative.addClass(StatelessSessionLocalHome.class);

        log.info(relative.toString(true));
        return relative;
    }

    public static JavaArchive ctsArchive() {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        String resourcesLocation = "org/jboss/as/test/integration/web/webintegration/resources/";

        JavaArchive cts = ShrinkWrap.create(JavaArchive.class, "cts.jar");
        cts.addAsManifestResource(tccl.getResource(resourcesLocation + "ejb/cts-ejb-jar.xml"), "ejb-jar.xml");
        cts.addAsManifestResource(tccl.getResource(resourcesLocation + "ejb/cts-jboss.xml"), "jboss.xml");

        cts.addPackage(StatelessSession.class.getPackage());

        log.info(cts.toString(true));
        return cts;
    }

    public static WebArchive badWebArchive() {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        String resourcesLocation = "org/jboss/as/test/integration/web/webintegration/resources/";

        WebArchive war = ShrinkWrap.create(WebArchive.class, "bad-web.war");
        war.setWebXML(tccl.getResource(resourcesLocation + "war/bad-web.xml"));
        war.addAsWebInfResource(tccl.getResource(resourcesLocation + "war/good-jboss-web.xml"), "jboss-web.xml");
        war.addAsWebResource(tccl.getResource(resourcesLocation + "html/good-index.html"), "index.html");

        log.info(war.toString(true));
        return war;
    }

    public static WebArchive goodWebArchive() {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        String resourcesLocation = "org/jboss/as/test/integration/web/webintegration/resources/";

        WebArchive war = ShrinkWrap.create(WebArchive.class, "good-web.war");
        war.setWebXML(tccl.getResource(resourcesLocation + "war/good-web.xml"));
        war.addAsWebInfResource(tccl.getResource(resourcesLocation + "war/good-jboss-web.xml"), "jboss-web.xml");
        war.addAsWebResource(tccl.getResource(resourcesLocation + "html/good-index.html"), "index.html");

        log.info(war.toString(true));
        return war;
    }

    public static EnterpriseArchive manifestArchive() {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        String resourcesLocation = "org/jboss/as/test/integration/web/webintegration/resources/";

        JavaArchive libservlet = ShrinkWrap.create(JavaArchive.class, "jbosstest-web-libservlet.jar");
        libservlet.addClass(SimpleServlet.class);
        log.info(libservlet.toString(true));

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "manifest-web.ear");
        ear.setApplicationXML(tccl.getResource(resourcesLocation + "ear/application-mf.xml"));
        ear.setManifest(new StringAsset("Class-Path: jbosstest-web-util.jar"));

        WebArchive war = ShrinkWrap.create(WebArchive.class, "manifest-web.war");
        war.setWebXML(tccl.getResource(resourcesLocation + "war/manifest-web.xml"));
        war.addAsWebResource(tccl.getResource(resourcesLocation + "html/classpath.jsp"), "classpath.jsp");
        war.addClass(ClasspathServlet2.class);
        war.addPackage(Debug.class.getPackage());
        war.addAsLibrary(libservlet);
        log.info(war.toString(true));

        ear.addAsModule(war);
        log.info(ear.toString(true));
        return ear;
    }

    public static WebArchive classloadingWebArchive() {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        String resourcesLocation = "org/jboss/as/test/integration/web/webintegration/resources/";

        WebArchive war = ShrinkWrap.create(WebArchive.class, "class-loading.war");
        war.setWebXML(tccl.getResource(resourcesLocation + "war/class-loading-web.xml"));
        war.addAsWebInfResource(tccl.getResource(resourcesLocation + "war/class-loading-jboss-web.xml"), "jboss-web.xml");

        MavenDependencyResolver resolver = DependencyResolvers.use(MavenDependencyResolver.class);
        war.addAsLibrary(resolver.artifact("log4j:log4j:1.2.16").resolveAs(JavaArchive.class).iterator().next());

        war.addClass(ClasspathServlet2.class);
        war.addPackage(Debug.class.getPackage());

        log.info(war.toString(true));
        return war;
    }

    public static WebArchive servletClassesWebArchive() {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        String resourcesLocation = "org/jboss/as/test/integration/web/webintegration/resources/";

        WebArchive war = ShrinkWrap.create(WebArchive.class, "servlet-classes.war");
        war.setWebXML(tccl.getResource(resourcesLocation + "war/servlet-classes-web.xml"));

        String mavenCoordinates = "org.jboss.spec.javax.servlet:jboss-servlet-api_3.0_spec:1.0.1.Final";
        MavenDependencyResolver resolver = DependencyResolvers.use(MavenDependencyResolver.class);
        war.addAsLibrary(resolver.artifact(mavenCoordinates).resolveAs(JavaArchive.class).iterator().next());

        war.addClass(ClasspathServlet2.class);
        war.addPackage(Debug.class.getPackage());

        log.info(war.toString(true));
        return war;
    }

    // URL Handling -----------------------------------------------------------

    public static URL addCredentials(URL base, String username, String password) throws MalformedURLException {
        String credentials = username + ":" + password;
        String host = base.getHost() + ":" + base.getPort();
        return new URL(base.getProtocol() + "://" + credentials + "@" + host + "/" + base.getFile());
    }

    // HTTP Client ------------------------------------------------------------

    public static HttpResponse accessURL(URL url) throws IOException {
        return accessURL(url, null, HTTP_OK, null, MethodType.GET);
    }

    public static HttpResponse accessURL(URL url, String realm, int expectedHttpCode) throws IOException {
        return accessURL(url, realm, expectedHttpCode, null, MethodType.GET);
    }

    public static HttpResponse accessURL(URL url, String realm, int expectedHttpCode, Header[] headers, MethodType type)
            throws IOException {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpRequestBase request = createMethod(url, type);

        if (headers != null) {
            for (Header h : headers) {
                request.addHeader(h);
            }
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

    public static enum MethodType {
        GET, POST, HEAD, OPTIONS, PUT, DELETE, TRACE;
    }

    // ########################################################################

    public class Deployable {

        final File file;
        final String name;

        Deployable(Archive<?> archive) {
            this.name = archive.getName();
            this.file = new File(System.getProperty("java.io.tmpdir") + File.separator + name);
            file.deleteOnExit(); // To avoid leaking due to bad usage
            new ZipExporterImpl(archive).exportTo(this.file, true);
        }

        public void deploy() throws Exception {
            final List<ModelNode> updates = new ArrayList<ModelNode>();

            ModelNode addNode = createOpNode("deployment=" + name, ADD);

            ModelNode pathNode = new ModelNode();
            pathNode.get(PATH).set(file.getAbsolutePath());
            pathNode.get(ARCHIVE).set(true);

            addNode.get(CONTENT).set(Arrays.asList(pathNode));
            addNode.get(ENABLED).set(true);

            updates.add(addNode);

            applyUpdates(updates, WebIntegrationSetup.client);
        }

        public void undeploy() throws Exception {
            final List<ModelNode> updates = new ArrayList<ModelNode>();

            updates.add(createOpNode("deployment=" + name, UNDEPLOY));
            updates.add(createOpNode("deployment=" + name, REMOVE));

            applyUpdates(updates, WebIntegrationSetup.client);
        }

        public void delete() {
            file.delete();
        }
    }
}
