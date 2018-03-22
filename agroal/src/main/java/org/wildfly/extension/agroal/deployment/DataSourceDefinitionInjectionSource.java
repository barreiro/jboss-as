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
package org.wildfly.extension.agroal.deployment;

import io.agroal.api.configuration.AgroalConnectionFactoryConfiguration;
import io.agroal.api.configuration.supplier.AgroalConnectionFactoryConfigurationSupplier;
import io.agroal.api.configuration.supplier.AgroalConnectionPoolConfigurationSupplier;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.agroal.api.security.NamePrincipal;
import io.agroal.api.security.SimplePassword;
import org.jboss.as.ee.resource.definition.ResourceDefinitionInjectionSource;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.agroal.service.DataSourceDefinitionService;

import javax.sql.DataSource;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import java.sql.Driver;
import java.time.Duration;
import java.util.Map;

import static org.jboss.as.server.deployment.Attachments.MODULE;

/**
 * Injection source for a DataSource
 *
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 */
public class DataSourceDefinitionInjectionSource extends ResourceDefinitionInjectionSource {

    public static final ServiceName DATASOURCE_DEFINITION_SERVICE_PREFIX = ServiceName.of( "wildfly", "agroal", "datasource-definition" );

    private String className;
    private String description;
    private String url;
    private String databaseName = "";
    private String serverName = "";
    private int portNumber = -1;
    private int loginTimeout = -1;
    private int isolationLevel = -1;
    private boolean transactional = true;
    private int initialPoolSize = -1;
    private int maxIdleTime = -1;
    private int maxPoolSize = -1;
    private int maxStatements = -1;
    private int minPoolSize = -1;
    private String user;
    private String password;

    public DataSourceDefinitionInjectionSource(String jndiName) {
        super( jndiName );
    }

    // --- //

    public void setClassName(String className) {
        this.className = className;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public void setPortNumber(int portNumber) {
        this.portNumber = portNumber;
    }

    public void setLoginTimeout(int loginTimeout) {
        this.loginTimeout = loginTimeout;
    }

    public void setIsolationLevel(int isolationLevel) {
        this.isolationLevel = isolationLevel;
    }

    public void setTransactional(boolean transactional) {
        this.transactional = transactional;
    }

    public void setInitialPoolSize(int initialPoolSize) {
        this.initialPoolSize = initialPoolSize;
    }

    public void setMaxIdleTime(int maxIdleTime) {
        this.maxIdleTime = maxIdleTime;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public void setMaxStatements(int maxStatements) {
        this.maxStatements = maxStatements;
    }

    public void setMinPoolSize(int minPoolSize) {
        this.minPoolSize = minPoolSize;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    // --- //

    @Override
    public void getResourceValue(ResolutionContext context, ServiceBuilder<?> serviceBuilder, DeploymentPhaseContext phaseContext, Injector<ManagedReferenceFactory> injector) throws DeploymentUnitProcessingException {

        AgroalConnectionFactoryConfigurationSupplier connectionFactoryConfiguration = new AgroalConnectionFactoryConfigurationSupplier();

        try {
            Class<?> providerClass = phaseContext.getDeploymentUnit().getAttachment( MODULE ).getClassLoader().loadClass( className );
            if ( providerClass != null && !DataSource.class.isAssignableFrom( providerClass ) && !Driver.class.isAssignableFrom( providerClass ) ) {
                throw new DeploymentUnitProcessingException( "Invalid connection provider. Either a java.sql.Driver or javax.sql.DataSource implementation is required. Fix the connection-provider for the driver" );
            }
            connectionFactoryConfiguration.connectionProviderClass( providerClass );
        } catch ( ClassNotFoundException e ) {
            throw new DeploymentUnitProcessingException( "Unable to load connection provider class " + className, e );
        }
        for ( Map.Entry<String, String> property : properties.entrySet() ) {
            connectionFactoryConfiguration.jdbcProperty( property.getKey(), property.getValue() );
        }
        if ( databaseName != null && !databaseName.isEmpty() ) {
            connectionFactoryConfiguration.jdbcProperty( "databaseName", databaseName );
        }
        if ( description != null && !description.isEmpty() ) {
            connectionFactoryConfiguration.jdbcProperty( "description", description );
        }
        if ( serverName != null && !serverName.isEmpty() ) {
            connectionFactoryConfiguration.jdbcProperty( "serverName", serverName );
        }
        if ( portNumber >= 0 ) {
            connectionFactoryConfiguration.jdbcProperty( "portNumber", Integer.toString( portNumber ) );
        }
        if ( loginTimeout >= 0 ) {
            connectionFactoryConfiguration.jdbcProperty( "loginTimeout", Integer.toString( loginTimeout ) );
        }
        if ( maxStatements >= 0 ) {
            connectionFactoryConfiguration.jdbcProperty( "maxStatements", Integer.toString( maxStatements ) );
        }

        if ( url != null && !url.isEmpty() ) {
            connectionFactoryConfiguration.jdbcUrl( url );
        }
        if ( user != null && !user.isEmpty() ) {
            connectionFactoryConfiguration.principal( new NamePrincipal( user ) );
        }
        if ( password != null && !password.isEmpty() ) {
            connectionFactoryConfiguration.credential( new SimplePassword( password ) );
        }
        connectionFactoryConfiguration.jdbcTransactionIsolation( AgroalConnectionFactoryConfiguration.TransactionIsolation.fromLevel( isolationLevel ) );

        AgroalConnectionPoolConfigurationSupplier connectionPoolConfiguration = new AgroalConnectionPoolConfigurationSupplier();
        connectionPoolConfiguration.connectionFactoryConfiguration( connectionFactoryConfiguration );

        if ( initialPoolSize >= 0 ) {
            connectionPoolConfiguration.initialSize( initialPoolSize );
        }
        if ( minPoolSize >= 0 ) {
            connectionPoolConfiguration.minSize( minPoolSize );
        }
        if ( maxPoolSize >= 0 ) {
            connectionPoolConfiguration.maxSize( maxPoolSize );
        }
        if ( maxIdleTime >= 0 ) {
            connectionPoolConfiguration.reapTimeout( Duration.ofSeconds( maxIdleTime ) );
        }

        AgroalDataSourceConfigurationSupplier dataSourceConfiguration = new AgroalDataSourceConfigurationSupplier();
        dataSourceConfiguration.connectionPoolConfiguration( connectionPoolConfiguration );

        ContextNames.BindInfo bindInfo = ContextNames.bindInfoForEnvEntry( context.getApplicationName(), context.getModuleName(), context.getComponentName(), !context.isCompUsesModule(), jndiName );
        ServiceName dataSourceServiceName = DATASOURCE_DEFINITION_SERVICE_PREFIX.append( bindInfo.getBinderServiceName().getCanonicalName() );

        // This is the service responsible for the JNDI binding, with a dependency on the datasource service that acts as a ManagedReferenceFactory and is used as the injection source
        BinderService binderService = new BinderService( bindInfo.getBindName(), this );
        ServiceBuilder<?> binderBuilder = phaseContext.getServiceTarget().addService( bindInfo.getBinderServiceName(), binderService )
                .addDependency( dataSourceServiceName, ManagedReferenceFactory.class, binderService.getManagedObjectInjector() )
                .addDependency( bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binderService.getNamingStoreInjector() );
        binderBuilder.install();

        DataSourceDefinitionService dataSourceService = new DataSourceDefinitionService( bindInfo, dataSourceConfiguration );
        ServiceBuilder<ManagedReferenceFactory> dataSourceServiceBuilder = phaseContext.getServiceTarget().addService( dataSourceServiceName, dataSourceService );

        if ( transactional ) {
            dataSourceServiceBuilder.addDependency( TxnServices.JBOSS_TXN_TRANSACTION_MANAGER, TransactionManager.class, dataSourceService.getTransactionManagerInjector() );
            dataSourceServiceBuilder.addDependency( TxnServices.JBOSS_TXN_SYNCHRONIZATION_REGISTRY, TransactionSynchronizationRegistry.class, dataSourceService.getTransactionSynchronizationRegistryInjector() );
        }

        dataSourceServiceBuilder.install();

        serviceBuilder.addDependency( bindInfo.getBinderServiceName() ).addDependency( dataSourceServiceName, ManagedReferenceFactory.class, injector );
    }
}
