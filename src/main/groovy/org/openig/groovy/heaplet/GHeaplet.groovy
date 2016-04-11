/*
 * Copyright 2016 ForgeRock AS.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openig.groovy.heaplet

import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType

import org.forgerock.json.JsonValue
import org.forgerock.openig.heap.GenericHeapObject
import org.forgerock.openig.heap.GenericHeaplet
import org.forgerock.openig.heap.Heap
import org.forgerock.openig.heap.HeapException
import org.forgerock.openig.heap.Name
import org.openig.groovy.heaplet.coerce.ConverterRegistry
import org.openig.groovy.heaplet.coerce.Converters

/**
 * Base class that supports a declarative heap object programming model.
 * It is responsible for performing value type conversion, injection and validation.
 *
 * @see Heaplet
 * @see org.openig.groovy.heaplet.ast.HeapletAstTransformation
 */
public class GHeaplet extends GenericHeaplet {

    /**
     * Managed type.
     */
    final Class type

    /**
     * Converter registry.
     */
    final ConverterRegistry converters = new Converters()

    /**
     * Construct a new Heaplet for the given managed type.
     *
     * @param type the managed type out of it we'll create and configure instances.
     */
    public GHeaplet(final Class type) {
        this.type = type;
    }

    @Override
    public Object create() throws HeapException {
        Object o = type.newInstance();
        type.metaClass.properties.each { property ->

            String pointer = property.name
            // Ignore properties that have no associated field
            if (!property.field) {
                return
            }

            // Exclude static / final fields
            if (Modifier.isStatic(property.modifiers) || Modifier.isFinal(property.modifiers)) {
                throw new HeapException("Cannot inject in static or final field " + property.name)
            }

            Field field = property.field.field

            // Ignore properties from GenericHeapObject
            if (field.declaringClass == GenericHeapObject) {
                return
            }

            if (field.isAnnotationPresent(Attribute)) {
                def name = field.getAnnotation(Attribute).value()
                if (!"".equals(name)) {
                    pointer = name
                }
            }

            def value
            Context context = field.getAnnotation(Context)
            if (context) {
                // Only supported: Heap, JsonValue and Name
                switch (property.type) {
                case Heap:
                    value = heap
                    break
                case JsonValue:
                    value = config
                    break
                case Name:
                    value = qualified
                    break
                default:
                    throw new HeapException("@Context is only supported on Heap, JsonValue and Name")
                }
            } else {
                Optional optional = field.getAnnotation(Optional)
                Reference reference = field.getAnnotation(Reference)
                Transform transform = field.getAnnotation(Transform)

                value = convert(config.get(pointer), field, optional, reference, transform)

                // Keep the old value if there was none found and the field was optional
                if (!value && optional) {
                    value = property.getProperty(o)
                }
            }

            property.setProperty(o, value)
        }
        return o;
    }

    Object convert(final JsonValue node,
                   final Field field,
                   final Optional optional,
                   final Reference reference,
                   final Transform transform) {
        // optional attribute
        if (node.isNull()) {
            if (optional) {
                return null
            }
            throw new HeapException("${node.pointer} is required")
        }

        if (List.isAssignableFrom(field.type)) {
            // Get generic type info
            ParameterizedType pt = field.genericType
            Class clazz = pt.getActualTypeArguments()[ 0 ] as Class
            return node.collect {
                doConvert(it, clazz, reference, optional, transform)
            }
        }

        if (Set.isAssignableFrom(field.type)) {
            // Get generic type info
            ParameterizedType pt = field.genericType
            Class clazz = pt.getActualTypeArguments()[ 0 ] as Class
            return node.collect(new LinkedHashSet()) {
                doConvert(it, clazz, reference, optional, transform)
            }
        }

        if (Collection.isAssignableFrom(field.type)) {
            // Get generic type info
            ParameterizedType pt = field.genericType
            Class clazz = pt.getActualTypeArguments()[ 0 ] as Class
            return node.collect {
                doConvert(it, clazz, reference, optional, transform)
            }
        }

        return doConvert(node, field.type, reference, optional, transform)
    }

    Object doConvert(JsonValue node, Class type, Reference reference, Optional optional, Transform transform) {
        if (reference) {
            def opt = optional != null
            return heap.resolve(node, type, opt)
        }

        if (transform) {
            Closure converter = (Closure) transform.value().newInstance(null, null)
            return converter(node)
        }

        def converter = converters.find(type)
        if (!converter) {
            throw new HeapException("${type.name} is not supported yet")
        }
        return converter.apply(node)
    }
}
