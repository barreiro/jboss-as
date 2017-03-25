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
package org.wildfly.extension.agroal.parser;

import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentResourceXMLParser;
import org.wildfly.extension.agroal.Namespace;
import org.wildfly.extension.agroal.definition.AgroalSubsystemDefinition;
import org.wildfly.extension.agroal.definition.DatasourceDefinition;
import org.wildfly.extension.agroal.definition.DriverDefinition;
import org.wildfly.extension.agroal.definition.XaDatasourceDefinition;

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;

/**
 * The subsystem parser, which uses STAX to read and write to and from xml
 *
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 */
public class AgroalSubsystemParser_1_0 extends PersistentResourceXMLParser {

    public static final AgroalSubsystemParser_1_0 INSTANCE = new AgroalSubsystemParser_1_0();

    private static final PersistentResourceXMLDescription XML_DESCRIPTION;

    static {
        XML_DESCRIPTION = builder( AgroalSubsystemDefinition.INSTANCE.getPathElement(), Namespace.AGROAL_1_0.getUriString() )
                .addChild( builder( DatasourceDefinition.INSTANCE.getPathElement() )
                        .addAttribute( DatasourceDefinition.JNDI_NAME_ATTRIBUTE )
                        .addAttribute( DatasourceDefinition.DRIVER_ATTRIBUTE )
                        .addAttribute( DatasourceDefinition.STATISTICS_ENABLED_ATTRIBUTE )
                        .addAttribute( DatasourceDefinition.JTA_ATTRIBUTE )
                        .addAttribute( DatasourceDefinition.CONNECTABLE_ATTRIBUTE )
                )
                .addChild( builder( XaDatasourceDefinition.INSTANCE.getPathElement() )
                        .addAttribute( XaDatasourceDefinition.JNDI_NAME_ATTRIBUTE )
                        .addAttribute( XaDatasourceDefinition.DRIVER_ATTRIBUTE )
                        .addAttribute( XaDatasourceDefinition.STATISTICS_ENABLED_ATTRIBUTE )
                )
                .addChild( builder( DriverDefinition.INSTANCE.getPathElement() )
                        .setXmlWrapperElement( DriverDefinition.DRIVERS_ELEMENT_NAME )
                        .addAttribute( DriverDefinition.DRIVER_CLASS_ATTRIBUTE )
                        .addAttribute( DriverDefinition.MODULE_ATTRIBUTE )
                        .addAttribute( DriverDefinition.SLOT_ATTRIBUTE )
                ).build();
    }

    private AgroalSubsystemParser_1_0() {
    }

    @Override
    public PersistentResourceXMLDescription getParserDescription() {
        return XML_DESCRIPTION;
    }
}
