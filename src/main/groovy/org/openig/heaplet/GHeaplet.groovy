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

package org.openig.heaplet

import org.forgerock.json.JsonValue
import org.forgerock.openig.heap.GenericHeaplet
import org.forgerock.openig.heap.Heap
import org.forgerock.openig.heap.HeapException
import org.forgerock.openig.heap.Name
import org.openig.heaplet.coerce.Converters

import java.lang.reflect.Field

/**
 * Created by guillaume on 01/03/16.
 */
public class GHeaplet extends GenericHeaplet {
    final Class type
    final Converters converters = new Converters()

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
            Field field = property.field.field

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

        if (reference) {
            def opt = optional != null
            return heap.resolve(node, field.type, opt)
        }

        if (transform) {
            Closure converter = (Closure) transform.value().newInstance(null, null)
            return converter(node)
        }

        def converter = converters.find(field.type)
        if (!converter) {
            throw new HeapException("${field.type.name} is not supported yet")
        }
        return converter.apply(node)
    }
}
