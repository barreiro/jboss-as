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

import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;

/**
 * Common Definition for the datasource resource
 *
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 */
public abstract class AbstractDatasourceDefinition extends PersistentResourceDefinition {

    private static final ParameterValidator NON_EMPTY_STRING = new StringLengthValidator( 1 );

    public static final SimpleAttributeDefinition JNDI_NAME_ATTRIBUTE = create( "jndi-name", ModelType.STRING )
            .setAllowExpression( true )
            .setRestartAllServices()
            .setValidator( NON_EMPTY_STRING )
            .build();

    public static final SimpleAttributeDefinition DRIVER_ATTRIBUTE = create( "driver", ModelType.STRING )
            .setAllowExpression( true )
            .setFlags( AttributeAccess.Flag.RESTART_ALL_SERVICES )
            .setValidator( NON_EMPTY_STRING )
            .build();

    public static final SimpleAttributeDefinition STATISTICS_ENABLED_ATTRIBUTE = create( "statistics-enabled", ModelType.BOOLEAN )
            .setAllowExpression( true )
            .setDefaultValue( new ModelNode( false ) )
            .setFlags( AttributeAccess.Flag.RESTART_ALL_SERVICES )
            .setRequired( false )
            .build();

    private static final List<PersistentResourceDefinition> CHILDREN = unmodifiableList( asList( ) );

    protected AbstractDatasourceDefinition(PathElement pathElement, ResourceDescriptionResolver descriptionResolver, OperationStepHandler addHandler, OperationStepHandler removeHandler) {
        super( pathElement, descriptionResolver, addHandler, removeHandler );
    }

    @Override
    protected List<? extends PersistentResourceDefinition> getChildren() {
        return CHILDREN;
    }
}
