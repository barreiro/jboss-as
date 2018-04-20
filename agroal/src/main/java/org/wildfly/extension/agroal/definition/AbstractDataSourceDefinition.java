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

import io.agroal.api.configuration.AgroalConnectionFactoryConfiguration;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.PrimitiveListAttributeDefinition;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.agroal.AgroalExtension;
import org.wildfly.extension.agroal.operation.AbstractDataSourceOperations;

import java.util.EnumSet;

import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;

/**
 * Common Definition for the datasource resource
 *
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 */
public abstract class AbstractDataSourceDefinition extends PersistentResourceDefinition {

    // Default values should be kept in sync with the schema
    public static final boolean STATISTICS_ENABLED_DEFAULT_VALUE = false;
    public static final boolean JTA_DEFAULT_VALUE = true;
    public static final boolean CONNECTABLE_DEFAULT_VALUE = false;

    public static final int MIN_SIZE_DEFAULT_VALUE = 0;
    public static final int INITIAL_SIZE_DEFAULT_VALUE = 0;
    public static final int BLOCKING_TIMEOUT_MILLIS_DEFAULT_VALUE = 0;
    public static final int BACKGROUND_VALIDATION_DEFAULT_VALUE = 0;
    public static final int LEAK_DETECTION_DEFAULT_VALUE = 0;
    public static final int IDLE_REMOVAL_DEFAULT_VALUE = 0;

    public static final SimpleAttributeDefinition JNDI_NAME_ATTRIBUTE = create( "jndi-name", ModelType.STRING )
            .setAllowExpression( true )
            .setRestartAllServices()
            .setValidator( new StringLengthValidator( 1 ) )
            .build();

    public static final SimpleAttributeDefinition STATISTICS_ENABLED_ATTRIBUTE = create( "statistics-enabled", ModelType.BOOLEAN )
            .setAllowExpression( true )
            .setDefaultValue( new ModelNode( STATISTICS_ENABLED_DEFAULT_VALUE ) )
            .setRequired( false )
            .build();

    // --- connection-factory attributes //

    public static final SimpleAttributeDefinition DRIVER_ATTRIBUTE = create( "driver", ModelType.STRING )
            .setAllowExpression( true )
            .setValidator( new StringLengthValidator( 1 ) )
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition URL_ATTRIBUTE = create( "url", ModelType.STRING )
            .setAllowExpression( true )
            .setRestartAllServices()
            .setRequired( false )
            .setValidator( new StringLengthValidator( 1 ) )
            .build();

    public static final SimpleAttributeDefinition TRANSACTION_ISOLATION_ATTRIBUTE = create( "transaction-isolation", ModelType.STRING )
            .setAllowExpression( true )
            .setAllowedValues( "NONE", "READ_UNCOMMITTED", "READ_COMMITTED", "REPEATABLE_READ", "SERIALIZABLE" )
            .setRequired( false )
            .setRestartAllServices()
            .setValidator( EnumValidator.create( AgroalConnectionFactoryConfiguration.TransactionIsolation.class, EnumSet.allOf( AgroalConnectionFactoryConfiguration.TransactionIsolation.class ) ) )
            .build();

    public static final SimpleAttributeDefinition NEW_CONNECTION_SQL_ATTRIBUTE = create( "new-connection-sql", ModelType.STRING )
            .setAttributeGroup( "new-connection" )
            .setAllowExpression( true )
            .setRequired( false )
            .setRestartAllServices()
            .setXmlName( "sql" )
            .build();

    public static final SimpleAttributeDefinition SECURITY_USERNAME_ATTRIBUTE = create( "security-username", ModelType.STRING )
            .setAttributeGroup( "security" )
            .setAllowExpression( true )
            .setRequired( false )
            .setRestartAllServices()
            .setValidator( new StringLengthValidator( 1 ) )
            .setXmlName( "username" )
            .build();

    public static final SimpleAttributeDefinition SECURITY_PASSWORD_ATTRIBUTE = create( "security-password", ModelType.STRING )
            .setAttributeGroup( "security" )
            .setAllowExpression( true )
            .setRequired( false )
            .setRestartAllServices()
            .setValidator( new StringLengthValidator( 1 ) )
            .setXmlName( "password" )
            .build();

    public static final PropertiesAttributeDefinition CONNECTION_PROPERTIES_ATTRIBUTE = new PropertiesAttributeDefinition.Builder( "connection-properties", true )
            .setAllowExpression( true )
            .setRequired( false )
            .setRestartAllServices()
            .build();

    public static final ObjectTypeAttributeDefinition CONNECTION_FACTORY_ATTRIBUTE = ObjectTypeAttributeDefinition.create( "connection-factory", DRIVER_ATTRIBUTE, URL_ATTRIBUTE, TRANSACTION_ISOLATION_ATTRIBUTE, NEW_CONNECTION_SQL_ATTRIBUTE, SECURITY_USERNAME_ATTRIBUTE, SECURITY_PASSWORD_ATTRIBUTE, CONNECTION_PROPERTIES_ATTRIBUTE )
            .setAttributeMarshaller( AgroalObjectAttribute.MARSHALLER )
            .setAttributeParser( AgroalObjectAttribute.PARSER )
            .setRestartAllServices()
            .build();

    // --- connection-pool attributes //

    public static final SimpleAttributeDefinition MAX_SIZE_ATTRIBUTE = create( "max-size", ModelType.INT )
            .setAllowExpression( true )
            .build();

    public static final SimpleAttributeDefinition MIN_SIZE_ATTRIBUTE = create( "min-size", ModelType.INT )
            .setAllowExpression( true )
            .setDefaultValue( new ModelNode( MIN_SIZE_DEFAULT_VALUE ) )
            .setRequired( false )
            .build();

    public static final SimpleAttributeDefinition INITIAL_SIZE_ATTRIBUTE = create( "initial-size", ModelType.INT )
            .setAllowExpression( true )
            .setDefaultValue( new ModelNode( INITIAL_SIZE_DEFAULT_VALUE ) )
            .setRequired( false )
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition BLOCKING_TIMEOUT_MILLIS_ATTRIBUTE = create( "blocking-timeout-millis", ModelType.INT )
            .setAllowExpression( true )
            .setDefaultValue( new ModelNode( BLOCKING_TIMEOUT_MILLIS_DEFAULT_VALUE ) )
            .setRequired( false )
            .build();

    public static final SimpleAttributeDefinition BACKGROUND_VALIDATION_ATTRIBUTE = create( "background-validation-millis", ModelType.INT )
            .setAttributeGroup( "background-validation" )
            .setAllowExpression( true )
            .setDefaultValue( new ModelNode( BACKGROUND_VALIDATION_DEFAULT_VALUE ) )
            .setRequired( false )
            .setRestartAllServices()
            .setXmlName( "millis" )
            .build();

    public static final SimpleAttributeDefinition LEAK_DETECTION_ATTRIBUTE = create( "leak-detection-millis", ModelType.INT )
            .setAttributeGroup( "leak-detection" )
            .setAllowExpression( true )
            .setDefaultValue( new ModelNode( LEAK_DETECTION_DEFAULT_VALUE ) )
            .setRequired( false )
            .setRestartAllServices()
            .setXmlName( "millis" )
            .build();

    public static final SimpleAttributeDefinition IDLE_REMOVAL_ATTRIBUTE = create( "idle-removal-minutes", ModelType.INT )
            .setAttributeGroup( "idle-removal" )
            .setAllowExpression( true )
            .setDefaultValue( new ModelNode( IDLE_REMOVAL_DEFAULT_VALUE ) )
            .setRequired( false )
            .setRestartAllServices()
            .setXmlName( "minutes" )
            .build();

    public static final ObjectTypeAttributeDefinition CONNECTION_POOL_ATTRIBUTE = ObjectTypeAttributeDefinition.create( "connection-pool", MAX_SIZE_ATTRIBUTE, MIN_SIZE_ATTRIBUTE, INITIAL_SIZE_ATTRIBUTE, BLOCKING_TIMEOUT_MILLIS_ATTRIBUTE, BACKGROUND_VALIDATION_ATTRIBUTE, LEAK_DETECTION_ATTRIBUTE, IDLE_REMOVAL_ATTRIBUTE )
            .setAttributeMarshaller( AgroalObjectAttribute.MARSHALLER )
            .setAttributeParser( AgroalObjectAttribute.PARSER )
            .build();

    // --- Operations //

    public static final OperationDefinition FLUSH_ALL = new SimpleOperationDefinitionBuilder( "flush-all", AgroalExtension.getResolver() ).build();

    public static final OperationDefinition FLUSH_GRACEFUL = new SimpleOperationDefinitionBuilder( "flush-graceful", AgroalExtension.getResolver() ).build();

    public static final OperationDefinition FLUSH_INVALID = new SimpleOperationDefinitionBuilder( "flush-invalid", AgroalExtension.getResolver() ).build();

    public static final OperationDefinition FLUSH_IDLE = new SimpleOperationDefinitionBuilder( "flush-idle", AgroalExtension.getResolver() ).build();

    public static final OperationDefinition STATISTICS_RESET = new SimpleOperationDefinitionBuilder( "statistics-reset", AgroalExtension.getResolver() ).build();

    // --- Runtime attributes //

    public static final PrimitiveListAttributeDefinition STATISTICS = PrimitiveListAttributeDefinition.Builder.of( "statistics", ModelType.LIST )
            .setRequired( false )
            .setStorageRuntime()
            .build();

    // --- //

    protected AbstractDataSourceDefinition(PathElement pathElement, ResourceDescriptionResolver descriptionResolver, OperationStepHandler addHandler, OperationStepHandler removeHandler) {
        super( pathElement, descriptionResolver, addHandler, removeHandler );
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        ReloadRequiredWriteAttributeHandler handler = new ReloadRequiredWriteAttributeHandler( getAttributes() );
        for ( AttributeDefinition attributeDefinition : getAttributes() ) {
            OperationStepHandler writeHandler = handler;

            if ( attributeDefinition == STATISTICS_ENABLED_ATTRIBUTE ) {
                writeHandler = AbstractDataSourceOperations.STATISTICS_ENABLED_WRITE_OPERATION;
            }

            if ( attributeDefinition == CONNECTION_POOL_ATTRIBUTE ) {
                writeHandler = AbstractDataSourceOperations.CONNECTION_POOL_WRITE_OPERATION;
            }

            resourceRegistration.registerReadWriteAttribute( attributeDefinition, null, writeHandler );
        }

        // Runtime attributes
        resourceRegistration.registerReadOnlyAttribute( STATISTICS, AbstractDataSourceOperations.STATISTICS_GET_OPERATION );
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations( resourceRegistration );
        resourceRegistration.registerOperationHandler( STATISTICS_RESET, AbstractDataSourceOperations.STATISTICS_RESET_OPERATION );
        resourceRegistration.registerOperationHandler( FLUSH_ALL, AbstractDataSourceOperations.FLUSH_ALL_OPERATION );
        resourceRegistration.registerOperationHandler( FLUSH_GRACEFUL, AbstractDataSourceOperations.FLUSH_GRACEFUL_OPERATION );
        resourceRegistration.registerOperationHandler( FLUSH_INVALID, AbstractDataSourceOperations.FLUSH_INVALID_OPERATION );
        resourceRegistration.registerOperationHandler( FLUSH_IDLE, AbstractDataSourceOperations.FLUSH_IDLE_OPERATION );
    }
}
