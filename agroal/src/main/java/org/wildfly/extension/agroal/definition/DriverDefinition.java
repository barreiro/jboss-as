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
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.agroal.AgroalExtension;
import org.wildfly.extension.agroal.operation.DriverAdd;
import org.wildfly.extension.agroal.operation.DriverRemove;

import java.util.Collection;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static org.jboss.as.controller.PathElement.pathElement;

/**
 * Definition for the driver element
 *
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 */
public class DriverDefinition extends PersistentResourceDefinition {

    public static final DriverDefinition INSTANCE = new DriverDefinition();

    public static final String DRIVERS_ELEMENT_NAME = "drivers";
    private static final String DRIVER_ELEMENT_NAME = "driver";

    private static final String DRIVER_CLASS_ATTRIBUTE_NAME = "driver-class";
    public static final SimpleAttributeDefinition DRIVER_CLASS_ATTRIBUTE = new SimpleAttributeDefinitionBuilder( DRIVER_CLASS_ATTRIBUTE_NAME, ModelType.STRING, false )
            .setFlags( AttributeAccess.Flag.RESTART_ALL_SERVICES )
            .setValidator( new StringLengthValidator( 1 ) )
            .build();

    private static final String MODULE_ATTRIBUTE_NAME = "module";
    public static final SimpleAttributeDefinition MODULE_ATTRIBUTE = new SimpleAttributeDefinitionBuilder( MODULE_ATTRIBUTE_NAME, ModelType.STRING, false )
            .setFlags( AttributeAccess.Flag.RESTART_ALL_SERVICES )
            .setValidator( new StringLengthValidator( 1 ) )
            .build();

    private static final String SLOT_ATTRIBUTE_NAME = "slot";
    public static final SimpleAttributeDefinition SLOT_ATTRIBUTE = new SimpleAttributeDefinitionBuilder( SLOT_ATTRIBUTE_NAME, ModelType.STRING, false )
            .setFlags( AttributeAccess.Flag.RESTART_ALL_SERVICES )
            .setValidator( new StringLengthValidator( 1 ) )
            .setRequired( false )
            .build();

    private static final Collection<AttributeDefinition> ATTRIBUTES = unmodifiableList( asList( DRIVER_CLASS_ATTRIBUTE, MODULE_ATTRIBUTE, SLOT_ATTRIBUTE ) );

    // --- //

    private DriverDefinition() {
        super( pathElement( DRIVER_ELEMENT_NAME ), AgroalExtension.getResolver( DRIVER_ELEMENT_NAME ), DriverAdd.INSTANCE, DriverRemove.INSTANCE );
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }
}
