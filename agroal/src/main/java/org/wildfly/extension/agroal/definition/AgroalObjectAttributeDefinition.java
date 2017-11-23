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

import org.jboss.as.controller.AbstractAttributeDefinitionBuilder;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeParser;
import org.jboss.as.controller.AttributeParsers;
import org.jboss.as.controller.DefaultAttributeMarshaller;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.operations.validation.ObjectTypeValidator;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.staxmapper.XMLExtendedStreamReader;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Arrays.stream;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

/**
 * Definition of a ObjectType attribute that supports attribute groups
 * It's a fix for the fact that ObjectTypeAttributeDefinition does not handle attribute groups
 *
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 */
public class AgroalObjectAttributeDefinition extends ObjectTypeAttributeDefinition {

    /**
     * Ideally ObjectTypeAttributeDefinition would expose this field, in which case it would be just a matter of setting a custom parser / marshal
     */
    private final AttributeDefinition[] valueTypes;

    protected AgroalObjectAttributeDefinition(Builder builder) {
        this( builder, builder.valueTypes );
    }

    protected AgroalObjectAttributeDefinition(AbstractAttributeDefinitionBuilder<?, ? extends AgroalObjectAttributeDefinition> builder, AttributeDefinition[] valueTypes) {
        super( builder, null, valueTypes );
        this.valueTypes = valueTypes;
    }

    public static Builder groupSupport(String name, AttributeDefinition... valueTypes) {
        return new Builder( name, valueTypes );
    }

    private static Map<String, LinkedHashMap<String, AttributeDefinition>> attributesByGroup(AttributeDefinition[] valueTypes) {
        Map<String, LinkedHashMap<String, AttributeDefinition>> attributesByGroup = new LinkedHashMap<>();
        stream( valueTypes ).filter( ad -> ad.getAttributeGroup() != null && !ad.getAttributeGroup().isEmpty() ).forEach( ad -> {
            LinkedHashMap<String, AttributeDefinition> forGroup = attributesByGroup.computeIfAbsent( ad.getAttributeGroup(), k -> new LinkedHashMap<>() );
            forGroup.put( ad.getXmlName(), ad );
        } );
        return attributesByGroup;
    }

    // --- //

    public static final class Builder extends AbstractAttributeDefinitionBuilder<AgroalObjectAttributeDefinition.Builder, AgroalObjectAttributeDefinition> {
        private final AttributeDefinition[] valueTypes;

        public Builder(final String name, final AttributeDefinition... valueTypes) {
            super( name, ModelType.OBJECT, true );
            this.valueTypes = valueTypes;
            setAttributeParser( AgroalObjectParser.INSTANCE );
            setAttributeMarshaller( AgroalObjectMarshaller.INSTANCE );
        }

        public AgroalObjectAttributeDefinition build() {
            if ( validator == null ) {
                validator = new ObjectTypeValidator( allowNull, valueTypes );
            }
            return new AgroalObjectAttributeDefinition( this );
        }
    }

    // --- //

    /**
     * Copy of AttributeParsers.ObjectParser, adding support for attribute groups (as a child element)
     */
    private static class AgroalObjectParser extends AttributeParsers.ObjectParser {

        private static final AgroalObjectParser INSTANCE = new AgroalObjectParser();

        private static final Predicate<AttributeDefinition> HAS_NO_GROUP = ad -> ad.getAttributeGroup() == null || ad.getAttributeGroup().isEmpty();
        private static final Predicate<AttributeDefinition> PARSE_AS_ELEMENT = ad -> ad.getParser().isParseAsElement();
        private static final Function<AttributeDefinition, String> XML_NAME = ad -> ad.getParser().getXmlName( ad );

        private static void parseEmbeddedElement(AgroalObjectAttributeDefinition attribute, XMLExtendedStreamReader reader, ModelNode op, String... additionalExpectedAttributes) throws XMLStreamException {
            AttributeDefinition[] valueTypes = attribute.valueTypes;

            Map<String, AttributeDefinition> attributes = stream( valueTypes ).filter( HAS_NO_GROUP ).collect( toMap( AttributeDefinition::getXmlName, identity() ) );
            Map<String, AttributeDefinition> attributeElements = stream( valueTypes ).filter( PARSE_AS_ELEMENT ).collect( toMap( XML_NAME, identity() ) );

            // Prepare attribute group data structures

            for ( int i = 0; i < reader.getAttributeCount(); i++ ) {
                String attributeName = reader.getAttributeLocalName( i );
                String value = reader.getAttributeValue( i );
                if ( attributes.containsKey( attributeName ) ) {
                    AttributeDefinition def = attributes.get( attributeName );
                    AttributeParser parser = def.getParser();
                    assert parser != null;
                    parser.parseAndSetParameter( def, value, op, reader );
                } else if ( Arrays.binarySearch( additionalExpectedAttributes, attributeName ) < 0 ) {
                    throw ParseUtils.unexpectedAttribute( reader, i, attributes.keySet() );
                }
            }

            // Check if there are also element attributes inside a group
            if ( reader.isStartElement() ) {
                String originalStartElement = reader.getLocalName();
                if ( reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT ) {
                    Map<String, LinkedHashMap<String, AttributeDefinition>> attributesByGroup = attributesByGroup( valueTypes );
                    do {
                        String attrName = reader.getLocalName();
                        if ( attributeElements.containsKey( attrName ) ) {
                            AttributeDefinition ad = attributeElements.get( reader.getLocalName() );
                            ad.getParser().parseElement( ad, reader, op );
                        } else if ( attributesByGroup.containsKey( attrName ) ) {
                            LinkedHashMap<String, AttributeDefinition> forGroup = attributesByGroup.get( attrName );
                            for ( int i = 0; i < reader.getAttributeCount(); i++ ) {
                                String attributeName = reader.getAttributeLocalName( i );
                                String value = reader.getAttributeValue( i );
                                if ( forGroup.containsKey( attributeName ) ) {
                                    AttributeDefinition def = forGroup.get( attributeName );
                                    AttributeParser parser = def.getParser();
                                    assert parser != null;
                                    parser.parseAndSetParameter( def, value, op, reader );
                                } else if ( Arrays.binarySearch( additionalExpectedAttributes, attributeName ) < 0 ) {
                                    throw ParseUtils.unexpectedAttribute( reader, i, attributes.keySet() );
                                }
                            }
                            ParseUtils.requireNoContent( reader );
                        } else {
                            throw ParseUtils.unexpectedElement( reader );
                        }
                    }
                    while ( !reader.getLocalName().equals( originalStartElement ) && reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT );
                }
            }
        }

        @Override
        public void parseElement(AttributeDefinition attribute, XMLExtendedStreamReader reader, ModelNode operation) throws
                XMLStreamException {
            assert attribute instanceof AgroalObjectAttributeDefinition;

            if ( attribute.getXmlName().equals( reader.getLocalName() ) ) {
                AgroalObjectAttributeDefinition objectType = ( (AgroalObjectAttributeDefinition) attribute );
                ModelNode op = operation.get( attribute.getName() );
                op.setEmptyObject();
                parseEmbeddedElement( objectType, reader, op );
            } else {
                throw ParseUtils.unexpectedElement( reader, Collections.singleton( attribute.getXmlName() ) );
            }
        }
    }

    /**
     * Copy of AttributeMarshaller.ATTRIBUTE_OBJECT, adding support for attribute groups (as child elements)
     */
    private static class AgroalObjectMarshaller extends DefaultAttributeMarshaller {

        private static final AgroalObjectMarshaller INSTANCE = new AgroalObjectMarshaller();

        private static Set<AttributeDefinition> sortAttributes(AttributeDefinition[] attributes) {
            Set<AttributeDefinition> sortedAttrs = new LinkedHashSet<>( attributes.length );
            List<AttributeDefinition> elementAds = null;
            for ( AttributeDefinition ad : attributes ) {
                if ( ad.getParser().isParseAsElement() ) {
                    if ( elementAds == null ) {
                        elementAds = new ArrayList<>();
                    }
                    elementAds.add( ad );
                } else {
                    sortedAttrs.add( ad );
                }
            }
            if ( elementAds != null ) {
                sortedAttrs.addAll( elementAds );
            }
            return sortedAttrs;
        }

        @Override
        public boolean isMarshallableAsElement() {
            return true;
        }

        @Override
        public void marshallAsElement(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
            assert attribute instanceof AgroalObjectAttributeDefinition;
            if ( resourceModel.hasDefined( attribute.getName() ) ) {
                AttributeDefinition[] valueTypes = ( (AgroalObjectAttributeDefinition) attribute ).valueTypes;
                Set<AttributeDefinition> sortedAttrs = sortAttributes( valueTypes );
                writer.writeStartElement( attribute.getXmlName() );
                for ( AttributeDefinition valueType : sortedAttrs ) {
                    if ( valueType.getAttributeGroup() == null && resourceModel.hasDefined( attribute.getName(), valueType.getName() ) ) {
                        ModelNode handler = resourceModel.get( attribute.getName() );
                        valueType.getMarshaller().marshall( valueType, handler, marshallDefault, writer );
                    }
                }

                // now we deal with attribute-groups
                Map<String, LinkedHashMap<String, AttributeDefinition>> attributesByGroup = attributesByGroup( valueTypes );
                Predicate<AttributeDefinition> ATTRIBUTE_DEFINED = ad -> resourceModel.hasDefined( attribute.getName(), ad.getName() );

                for ( Map.Entry<String, LinkedHashMap<String, AttributeDefinition>> entry : attributesByGroup.entrySet() ) {
                    if ( entry.getKey() == null || entry.getValue().values().stream().noneMatch( ATTRIBUTE_DEFINED ) ) {
                        continue;
                    }

                    writer.writeStartElement( entry.getKey() );
                    ModelNode handler = resourceModel.get( attribute.getName() );
                    for ( AttributeDefinition valueType : entry.getValue().values() ) {
                        if ( ATTRIBUTE_DEFINED.test( valueType ) ) {
                            valueType.getMarshaller().marshall( valueType, handler, marshallDefault, writer );
                        }
                    }
                    writer.writeEndElement();
                }
                writer.writeEndElement();
            }
        }
    }
}
