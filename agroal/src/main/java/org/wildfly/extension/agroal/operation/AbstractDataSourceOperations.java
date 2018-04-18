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
package org.wildfly.extension.agroal.operation;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.AgroalDataSourceMetrics;
import io.agroal.api.configuration.AgroalConnectionPoolConfiguration;
import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;
import org.wildfly.extension.agroal.definition.AbstractDataSourceDefinition;
import org.wildfly.extension.agroal.logging.AgroalLogger;
import org.wildfly.extension.agroal.service.DataSourceService;

import java.time.Duration;

/**
 * Operations common to XA and non-XA DataSources
 *
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 */
public class AbstractDataSourceOperations {

    public static final OperationStepHandler STATISTICS_ENABLED_WRITE_OPERATION = new StatisticsEnabledAttributeWriter( AbstractDataSourceDefinition.STATISTICS_ENABLED_ATTRIBUTE );

    public static final OperationStepHandler CONNECTION_POOL_WRITE_OPERATION = new ConnectionPoolAttributeWriter( AbstractDataSourceDefinition.CONNECTION_POOL_ATTRIBUTE.getValueTypes() );

    // --- //

    public static final OperationStepHandler FLUSH_ALL_OPERATION = new FlushOperation( AgroalDataSource.FlushMode.ALL );

    public static final OperationStepHandler FLUSH_GRACEFUL_OPERATION = new FlushOperation( AgroalDataSource.FlushMode.GRACEFUL );

    public static final OperationStepHandler FLUSH_INVALID_OPERATION = new FlushOperation( AgroalDataSource.FlushMode.INVALID );

    public static final OperationStepHandler FLUSH_IDLE_OPERATION = new FlushOperation( AgroalDataSource.FlushMode.IDLE );

    public static final OperationStepHandler STATISTICS_GET_OPERATION = new StatisticsGetOperation();

    public static final OperationStepHandler STATISTICS_RESET_OPERATION = new StatisticsResetOperation();

    // --- //

    private static AgroalDataSource getDataSource(OperationContext context) throws OperationFailedException {
        ServiceRegistry registry = context.getServiceRegistry( false );
        String dataSourceName = context.getCurrentAddressValue();

        switch ( context.getCurrentAddress().getLastElement().getKey() ) {
            case DataSourceOperations.DATASOURCE_SERVICE_NAME: {
                ServiceController<?> controller = registry.getService( DataSourceOperations.DATASOURCE_SERVICE_PREFIX.append( dataSourceName ) );
                return ( (DataSourceService) controller.getService() ).getValue();
            }
            case XADataSourceOperations.XADATASOURCE_SERVICE_NAME: {
                ServiceController<?> controller = registry.getService( XADataSourceOperations.XADATASOURCE_SERVICE_PREFIX.append( dataSourceName ) );
                return ( (DataSourceService) controller.getService() ).getValue();
            }
            default:
                throw new OperationFailedException( "Unknown datasource service of type : " + context.getCurrentAddress().getLastElement().getKey() );
        }
    }

    // --- //

    private static class StatisticsEnabledAttributeWriter extends AbstractWriteAttributeHandler<Boolean> {

        private StatisticsEnabledAttributeWriter(AttributeDefinition... definitions) {
            super( definitions );
        }

        @Override
        protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Boolean> handbackHolder) throws OperationFailedException {
            getDataSource( context ).getConfiguration().setMetricsEnabled( resolvedValue.asBoolean() );
            return false;
        }

        @Override
        protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, Boolean handback) throws OperationFailedException {
            getDataSource( context ).getConfiguration().setMetricsEnabled( valueToRevert.asBoolean() );
        }
    }

    private static class ConnectionPoolAttributeWriter extends AbstractWriteAttributeHandler<AgroalConnectionPoolConfiguration> {

        private ConnectionPoolAttributeWriter(AttributeDefinition... definitions) {
            super( definitions );
        }

        @Override
        protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<AgroalConnectionPoolConfiguration> handbackHolder) throws OperationFailedException {
            ModelNode newBlockingTimeout = resolvedValue.remove( AbstractDataSourceDefinition.BLOCKING_TIMEOUT_MILLIS_ATTRIBUTE.getName() );
            ModelNode newMaxSize = resolvedValue.remove( AbstractDataSourceDefinition.MAX_SIZE_ATTRIBUTE.getName() );
            ModelNode newMinSize = resolvedValue.remove( AbstractDataSourceDefinition.MIN_SIZE_ATTRIBUTE.getName() );

            for ( String attribute : resolvedValue.keys() ) {
                if ( !currentValue.hasDefined( attribute ) || !resolvedValue.get( attribute ).equals( currentValue.get( attribute ) ) ) {
                    // Other attributes changed. Restart required
                    return true;
                }
            }

            if ( newBlockingTimeout != null ) {
                getDataSource( context ).getConfiguration().connectionPoolConfiguration().setAcquisitionTimeout( Duration.ofMillis( newBlockingTimeout.asInt() ) );
            }
            if ( newMaxSize != null ) {
                getDataSource( context ).getConfiguration().connectionPoolConfiguration().setMaxSize( newMaxSize.asInt() );
            }
            if ( newMinSize != null ) {
                getDataSource( context ).getConfiguration().connectionPoolConfiguration().setMinSize( newMinSize.asInt() );
                getDataSource( context ).flush( AgroalDataSource.FlushMode.FILL );
            }
            return false;
        }

        @Override
        protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, AgroalConnectionPoolConfiguration handback) throws OperationFailedException {
            ModelNode newBlockingTimeout = valueToRevert.remove( AbstractDataSourceDefinition.BLOCKING_TIMEOUT_MILLIS_ATTRIBUTE.getName() );
            ModelNode newMaxSize = valueToRevert.remove( AbstractDataSourceDefinition.MAX_SIZE_ATTRIBUTE.getName() );
            ModelNode newMinSize = valueToRevert.remove( AbstractDataSourceDefinition.MIN_SIZE_ATTRIBUTE.getName() );

            if ( newBlockingTimeout != null ) {
                getDataSource( context ).getConfiguration().connectionPoolConfiguration().setAcquisitionTimeout( Duration.ofMillis( newBlockingTimeout.asInt() ) );
            }
            if ( newMinSize != null ) {
                getDataSource( context ).getConfiguration().connectionPoolConfiguration().setMinSize( newMinSize.asInt() );
                getDataSource( context ).flush( AgroalDataSource.FlushMode.IDLE );
            }
            if ( newMaxSize != null ) {
                getDataSource( context ).getConfiguration().connectionPoolConfiguration().setMaxSize( newMaxSize.asInt() );
            }
        }
    }

    // --- //

    private static class FlushOperation implements OperationStepHandler {

        private AgroalDataSource.FlushMode mode;

        private FlushOperation(AgroalDataSource.FlushMode mode) {
            this.mode = mode;
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            if ( context.isNormalServer() ) {
                AgroalLogger.POOL_LOGGER.infov( "Performing flush operation, mode {0}", mode );
                getDataSource( context ).flush( mode );
            }
        }
    }

    // --- //

    private static class StatisticsGetOperation implements OperationStepHandler {

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            if ( context.isNormalServer() ) {
                AgroalDataSourceMetrics metrics = getDataSource( context ).getMetrics();

                ModelNode result = new ModelNode();

                result.get( "acquire-count" ).set( metrics.acquireCount() );
                result.get( "active-count" ).set( metrics.activeCount() );
                result.get( "available-count" ).set( metrics.availableCount() );
                result.get( "awaiting-count" ).set( metrics.awaitingCount() );
                result.get( "creation-count" ).set( metrics.creationCount() );
                result.get( "destroy-count" ).set( metrics.destroyCount() );
                result.get( "flush-count" ).set( metrics.flushCount() );
                result.get( "invalid-count" ).set( metrics.invalidCount() );
                result.get( "leak-detection-count" ).set( metrics.leakDetectionCount() );
                result.get( "max-used-count" ).set( metrics.maxUsedCount() );
                result.get( "reap-count" ).set( metrics.reapCount() );

                result.get( "blocking-time-average-ms" ).set( metrics.blockingTimeAverage().toMillis() );
                result.get( "blocking-time-max-ms" ).set( metrics.blockingTimeMax().toMillis() );
                result.get( "blocking-time-total-ms" ).set( metrics.blockingTimeTotal().toMillis() );

                result.get( "creation-time-average-ms" ).set( metrics.creationTimeAverage().toMillis() );
                result.get( "creation-time-max-ms" ).set( metrics.creationTimeMax().toMillis() );
                result.get( "creation-time-total-ms" ).set( metrics.creationTimeTotal().toMillis() );

                context.getResult().set( result );
            }
        }
    }

    private static class StatisticsResetOperation implements OperationStepHandler {

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            if ( context.isNormalServer() ) {
                getDataSource( context ).getMetrics().reset();
            }
        }
    }
}

