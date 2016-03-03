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

import org.forgerock.http.Handler
import org.forgerock.http.protocol.Response
import org.forgerock.http.protocol.Status
import org.forgerock.openig.heap.HeapException
import org.forgerock.openig.heap.HeapImpl
import org.forgerock.openig.heap.Keys
import org.forgerock.openig.heap.Name
import org.forgerock.openig.io.TemporaryStorage
import org.forgerock.openig.log.NullLogSink
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import static org.forgerock.http.protocol.Response.newResponsePromise
import static org.forgerock.json.JsonValue.json

/**
 * Created by guillaume on 02/03/16.
 */
class GHeapletSpec extends Specification {

    @Shared Name name = Name.of("this")
    @Shared HeapImpl heap = new HeapImpl(Name.of("heap"))

    void setup() {
        heap.put(Keys.LOGSINK_HEAP_KEY, new NullLogSink())
        heap.put(Keys.TEMPORARY_STORAGE_HEAP_KEY, new TemporaryStorage())
    }

    def "should throw exception on missing required attribute"() {
        given:
        def heaplet = new GHeaplet(RequiredAttribute)

        when:
        heaplet.create(name, json([:]), heap)

        then:
        thrown(HeapException)
    }

    def "Should inject named reference"() {
        given:
        def heaplet = new GHeaplet(ReferenceSupport)
        heap.put("new-handler", {context, request -> newResponsePromise(new Response(Status.OK))} as Handler)

        when:
        def object = heaplet.create(name, json([handler: "new-handler"]), heap)

        then:
        object.handler
    }

    def "Should accept missing optional reference"() {
        given:
        def heaplet = new GHeaplet(OptionalReferenceSupport)

        when:
        def object = heaplet.create(name, json([:]), heap)

        then:
        !object.handler
    }

    def "Should inject inlined reference"() {
        given:
        def heaplet = new GHeaplet(ReferenceSupport)

        when:
        def object = heaplet.create(name, json([handler:[ type: 'ClientHandler' ]]), heap)

        then:
        object.handler
    }

    @Unroll
    def "#type.simpleName message should be equal to #result"() {
        given:
        def heaplet = new GHeaplet(type)

        when:
        def object = heaplet.create(name, json(config), heap)

        then:
        object.message == result

        where:
        type                      | config                 || result
        RequiredAttribute         | [ "message": "Hello" ] || "Hello"
        RequiredNamedAttribute    | [ "msg": "Hello" ]     || "Hello"
        RequiredImplicitAttribute | [ "message": "Hello" ] || "Hello"
        OptionalAttribute         | [ "message": "Hello" ] || "Hello"
        OptionalAttribute         | [:]                    || null
        TransformSupport          | [ "message": "Hello" ] || "Hello World"
    }

    def "should inject primitive types"() {
        given:
        def heaplet = new GHeaplet(PrimitiveTypes)

        when:
        def config = [
                aBoolean: true,
                aShort: 42,
                anInt: 312,
                aLong: 123456789,
                aFloat: 12.5,
                aDouble: 125134644,
                aChar: 2,
                aChar2: "a"
        ]
        def object = heaplet.create(name, json(config), heap)

        then:
        object.aBoolean
        object.aShort == 42
        object.anInt == 312
        object.aLong == 123456789
        object.aFloat == 12.5
        object.aDouble == 125134644
        object.aChar == 2
        object.aChar2 == 'a'
    }

    public static class RequiredImplicitAttribute {
        String message
    }

    public static class RequiredAttribute {
        @Attribute
        String message
    }

    public static class OptionalAttribute {
        @Optional
        @Attribute
        String message
    }

    public static class RequiredNamedAttribute {
        @Attribute("msg")
        String message
    }

    public static class ReferenceSupport {
        @Attribute
        @Reference
        Handler handler
    }

    public static class OptionalReferenceSupport {
        @Attribute
        @Optional
        @Reference
        Handler handler
    }

    public static class TransformSupport {
        @Attribute
        @Transform({"${it.asString()} World"})
        String message
    }

    public static class PrimitiveTypes {
        @Attribute
        boolean aBoolean

        @Attribute
        short aShort

        @Attribute
        int anInt

        @Attribute
        long aLong

        @Attribute
        float aFloat

        @Attribute
        double aDouble

        @Attribute
        char aChar

        @Attribute
        char aChar2
    }


}
