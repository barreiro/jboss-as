/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.agroal.definition;

import org.jboss.as.controller.AttributeDefinition;
import org.wildfly.extension.agroal.operation.XaDatasourceAdd;
import org.wildfly.extension.agroal.operation.XaDatasourceRemove;

import java.util.Collection;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static org.jboss.as.controller.PathElement.pathElement;
import static org.wildfly.extension.agroal.AgroalExtension.getResolver;

/**
 * Definition for the xa-datasource resource
 *
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 */
public class XaDatasourceDefinition extends AbstractDatasourceDefinition {

    public static final XaDatasourceDefinition INSTANCE = new XaDatasourceDefinition();

    private static final Collection<AttributeDefinition> ATTRIBUTES = unmodifiableList( asList( JNDI_NAME_ATTRIBUTE, DRIVER_ATTRIBUTE, STATISTICS_ENABLED_ATTRIBUTE, MAX_SIZE_ATTRIBUTE, MIN_SIZE_ATTRIBUTE, INITIAL_SIZE_ATTRIBUTE, BLOCKING_TIMEOUT_MILLIS_ATTRIBUTE, URL_ATTRIBUTE, TRANSACTION_ISOLATION_ATTRIBUTE, INTERRUPT_PROTECTION_ATTRIBUTE, NEW_CONNECTION_SQL_ATTRIBUTE ) );

    // --- //

    private XaDatasourceDefinition() {
        super( pathElement( "xa-datasource" ), getResolver( "xa-datasource" ), XaDatasourceAdd.INSTANCE, XaDatasourceRemove.INSTANCE );
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }
}
