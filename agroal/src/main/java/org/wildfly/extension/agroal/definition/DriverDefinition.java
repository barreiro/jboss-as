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
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.agroal.operation.DriverAdd;
import org.wildfly.extension.agroal.operation.DriverRemove;

import java.util.Collection;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static org.jboss.as.controller.PathElement.pathElement;
import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.wildfly.extension.agroal.AgroalExtension.getResolver;

/**
 * Definition for the driver resource
 *
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 */
public class DriverDefinition extends PersistentResourceDefinition {

    public static final DriverDefinition INSTANCE = new DriverDefinition();

    public static final String DRIVERS_ELEMENT_NAME = "drivers";

    public static final SimpleAttributeDefinition DRIVER_CLASS_ATTRIBUTE = create( "driver-class", ModelType.STRING )
            .setAllowExpression( true )
            .setRestartAllServices()
            .setValidator( new StringLengthValidator( 1 ) )
            .build();

    public static final SimpleAttributeDefinition MODULE_ATTRIBUTE = create( "module", ModelType.STRING )
            .setAllowExpression( true )
            .setRestartAllServices()
            .setValidator( new StringLengthValidator( 1 ) )
            .build();

    public static final SimpleAttributeDefinition SLOT_ATTRIBUTE = create( "slot", ModelType.STRING )
            .setAllowExpression( true )
            .setDefaultValue( new ModelNode( "main" ) )
            .setRequired( false )
            .setRestartAllServices()
            .setValidator( new StringLengthValidator( 1 ) )
            .build();

    private static final Collection<AttributeDefinition> ATTRIBUTES = unmodifiableList( asList( DRIVER_CLASS_ATTRIBUTE, MODULE_ATTRIBUTE, SLOT_ATTRIBUTE ) );

    // --- //

    private DriverDefinition() {
        super( pathElement( "driver" ), getResolver( "driver" ), DriverAdd.INSTANCE, DriverRemove.INSTANCE );
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }
}
