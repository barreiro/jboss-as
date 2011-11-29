/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.test.integration.web.enc.util;

import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.ValueManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.test.integration.web.enc.mock.EntityHomeBean;
import org.jboss.as.test.integration.web.enc.mock.MockDataSource;
import org.jboss.as.test.integration.web.enc.mock.QueueBean;
import org.jboss.as.test.integration.web.enc.mock.StatelessSessionLocalHomeBean;
import org.jboss.as.test.integration.web.enc.mock.TopicBean;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistryException;
import org.jboss.msc.value.ImmediateValue;

/**
 * ServiceActivators allow you to register your own services as part of the
 * deployment process
 * 
 * This particular implementation adds BinderService instances that will bound
 * the ENC mocking objects
 * 
 * @author lbarreiro@redhat.com
 */
public class ENCServiceActivator implements ServiceActivator {

    private static final Logger log = Logger.getLogger(ENCServiceActivator.class);

    @Override
    public void activate(ServiceActivatorContext context) throws ServiceRegistryException {

        // class="org.jboss.test.web.mock.MockDataSource" name="java:/MockDS"
        addBinding(context, ContextNames.JAVA_CONTEXT_SERVICE_NAME, "MockDS", new MockDataSource());

        // class="org.jboss.test.web.mock.StatelessLocalHomeBean" name="jbosstest/ejbs/local/ENCBean1"
        addBinding(context, ContextNames.JAVA_CONTEXT_SERVICE_NAME, "jbosstest/ejbs/local/ENCBean1", new StatelessSessionLocalHomeBean());

        // class="org.jboss.test.web.mock.EntityHomeBean" name="ejbcts/BMPBean"
        addBinding(context, ContextNames.JAVA_CONTEXT_SERVICE_NAME, "ejbcts/BMPBean", new EntityHomeBean());

        // class="org.jboss.test.web.mock.QueueBean" name="MockQueueA"
        addBinding(context, ContextNames.JAVA_CONTEXT_SERVICE_NAME, "MockQueueA", new QueueBean("QUEUE.A"));

        // class="org.jboss.test.web.mock.QueueBean" name="MockQueueB"
        addBinding(context, ContextNames.JAVA_CONTEXT_SERVICE_NAME, "MockQueueB", new QueueBean("QUEUE.B"));

        // class="org.jboss.test.web.mock.TopicBean" name="MockTopicA"
        addBinding(context, ContextNames.JAVA_CONTEXT_SERVICE_NAME, "MockTopicA", new TopicBean("TOPIC.testTopic"));
    }

    /**
     * Add a JNDI binding to an object
     */
    private void addBinding(ServiceActivatorContext context, final ServiceName namespace, final String name, final Object value) {
        final ServiceName serviceName = namespace.append(name);
        final BinderService binderService = new BinderService(name);

        ServiceBuilder<ManagedReferenceFactory> builder = context.getServiceTarget().addService(serviceName, binderService);
        builder.addDependency(namespace, ServiceBasedNamingStore.class, binderService.getNamingStoreInjector());
        binderService.getManagedObjectInjector().inject(new ValueManagedReferenceFactory(new ImmediateValue<Object>(value)));
        builder.install();

        log.debug("ENC ACTIVATOR: Added '" + serviceName.getCanonicalName() + "' binding");
    }

}
