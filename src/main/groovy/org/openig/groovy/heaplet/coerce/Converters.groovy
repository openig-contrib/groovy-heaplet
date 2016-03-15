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

package org.openig.groovy.heaplet.coerce

import org.forgerock.json.JsonPointer
import org.forgerock.openig.heap.HeapException

import java.nio.charset.Charset
import java.util.regex.Pattern

/**
 * Created by guillaume on 02/03/16.
 */
class Converters extends ConverterRegistry {

    static final Converter<Character> CHAR_CONVERTER = {
        if (it.isString()) {
            if (it.asString().size() == 1) {
                return it.asString()[0]
            } else {
                throw new HeapException("${it.pointer} cannot be converter as char")
            }
        } else if (it.isNumber()) {
            return (char) it.asNumber().intValue()
        }
    }

    Converters() {
        // Built-in JsonValue transformer
        register(String) { it.asString() }
        register(URI) { it.asURI() }
        register(URL) { it.asURL() }
        register(UUID) { it.asUUID() }
        register(Charset) { it.asCharset() }
        register(File) { it.asFile() }
        register(Pattern) { it.asPattern() }
        register(JsonPointer) { it.asPointer() }

        // Primitive types
        register(Boolean.TYPE) { it.asBoolean() }
        register(Short.TYPE) { it.asNumber().shortValue() }
        register(Float.TYPE) { it.asNumber().floatValue() }
        register(Double.TYPE) { it.asNumber().doubleValue() }
        register(Long.TYPE) { it.asNumber().longValue() }
        register(Integer.TYPE) { it.asNumber().intValue() }
        register(Byte.TYPE) { it.asNumber().byteValue() }
        register(Character.TYPE, CHAR_CONVERTER)

        // Wrapper of primitive types
        register(Boolean) { it.asBoolean() }
        register(Short) { it.asNumber().shortValue() }
        register(Float) { it.asNumber().floatValue() }
        register(Double) { it.asNumber().doubleValue() }
        register(Long) { it.asNumber().longValue() }
        register(Integer) { it.asNumber().intValue() }
        register(Byte) { it.asNumber().byteValue() }
        register(Character, CHAR_CONVERTER)

        register(Closure) {
            def script = it.asString()
            def shell = new GroovyShell()
            def object = shell.evaluate(script)
            if (!(object instanceof Closure)) {
                throw new HeapException("Can't transform '${script}' in a Closure, got '$object'")
            }
            return object
        }
    }
}
