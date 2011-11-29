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
package org.jboss.as.test.integration.web.webctxloader;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

/**
 * Testcase that tests various things with the JBossWebLoader set to true in the
 * tomcat configuration
 * 
 * @author <mailto:Anil.Saldhana@jboss.org>Anil Saldhana
 * @author lbarreiro@redhat.com
 */
@RunWith(Arquillian.class)
@RunAsClient
public class WebCtxLoaderTestCase {
   
    private static Logger log = Logger.getLogger(WebCtxLoaderTestCase.class);

    /**
     * Test that the WebCtxLoader only takes in the jar files from the
     * WEB-INF/lib directory
     */
    @Deployment(name = "jbosstest-webctx.war", testable = false)
    public static WebArchive deployment() {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        String resourcesLocation = "org/jboss/as/test/integration/web/webctxloader/resources/";

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "util.jar");
        jar.addClass(EJBManifestClass.class);
        
        WebArchive war = ShrinkWrap.create(WebArchive.class, "jbosstest-webctx.war");
        war.setWebXML(tccl.getResource(resourcesLocation + "webctx-web.xml"));
        war.addClass(WebCtxLoaderTestContextListener.class);
        war.addAsLibrary(jar);
        
        log.info(war.toString());
        log.info(jar.toString());
        return war;
    }
}
