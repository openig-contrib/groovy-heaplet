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
import org.forgerock.json.JsonValue
import org.forgerock.openig.heap.*
import org.forgerock.openig.io.TemporaryStorage
import org.forgerock.openig.log.NullLogSink
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.CopyOnWriteArrayList

import static org.forgerock.http.protocol.Response.newResponsePromise
import static org.forgerock.json.JsonValue.json
import static org.hamcrest.CoreMatchers.hasItems
import static spock.util.matcher.HamcrestSupport.expect

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

    @Unroll
    def "Should not inject static or final attributes in #type.simpleName"() {
        given:
        def heaplet = new GHeaplet(type)

        when:
        heaplet.create(name, json(config), heap)

        then:
        thrown(HeapException)

        where:
        type              | config
        StaticAttribute   | [ "message": "Hello" ]
        FinalAttribute    | [ "message": "Hello" ]
    }

    def "Should inject heap, config and name"() {
        given:
        def heaplet = new GHeaplet(ContextSupport)
        def config = json([:])

        when:
        def object = heaplet.create(name, config, heap)

        then:
        object.name == name
        object.config == config
        object.heap == heap
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

    def "Should support List injection"() {
        given:
        def heaplet = new GHeaplet(RequiredListAttribute)

        when:
        def RequiredListAttribute object = heaplet.create(name, json([messages: [ "one", "two", "three" ]]), heap)

        then:
        expect object.list, hasItems("one", "two", "three")
        expect object.arrayList, hasItems("one", "two", "three")
        expect object.linkedList, hasItems("one", "two", "three")
        expect object.vector, hasItems("one", "two", "three")
        expect object.copyOnWriteArrayList, hasItems("one", "two", "three")
    }

    def "Should support Set injection"() {
        given:
        def heaplet = new GHeaplet(RequiredSetAttribute)

        when:
        def RequiredSetAttribute object = heaplet.create(name, json([messages: [ "one", "two", "three" ]]), heap)

        then:
        expect object.set, hasItems("one", "two", "three")
        expect object.treeSet, hasItems("one", "two", "three")
        expect object.linkedHashSet, hasItems("one", "two", "three")
        expect object.hashSet, hasItems("one", "two", "three")
        expect object.concurrentSkipListSet, hasItems("one", "two", "three")
    }

    def "Should support Collection injection"() {
        given:
        def heaplet = new GHeaplet(RequiredCollectionAttribute)

        when:
        def RequiredCollectionAttribute object = heaplet.create(name, json([messages: [ "one", "two", "three" ]]), heap)

        then:
        expect object.collection, hasItems("one", "two", "three")
    }

    def "Should support transformation on collections"() {
        given:
        def heaplet = new GHeaplet(TransformedAttributes)

        when:
        def TransformedAttributes object = heaplet.create(name, json([messages: [ "one", "two", "three" ]]), heap)

        then:
        expect object.list, hasItems("one world", "two world", "three world")
        expect object.set, hasItems("one world", "two world", "three world")
        expect object.collection, hasItems("one world", "two world", "three world")
    }

    static class StaticAttribute {
        static String message
    }

    static class FinalAttribute {
        final String message = "a"
    }

    static class RequiredImplicitAttribute {
        String message
    }

    static class RequiredAttribute {
        @Attribute
        String message
    }

    static class OptionalAttribute {
        @Optional
        @Attribute
        String message
    }

    static class RequiredNamedAttribute {
        @Attribute("msg")
        String message
    }

    static class ReferenceSupport {
        @Attribute
        @Reference
        Handler handler
    }

    static class OptionalReferenceSupport {
        @Attribute
        @Optional
        @Reference
        Handler handler
    }

    static class TransformSupport {
        @Attribute
        @Transform({"${it.asString()} World"})
        String message
    }

    static class PrimitiveTypes {
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

    static class ContextSupport {
        @Context
        Heap heap

        @Context
        JsonValue config

        @Context
        Name name
    }

    static class RequiredListAttribute {
        @Attribute('messages')
        List<String> list

        @Attribute('messages')
        LinkedList<String> linkedList

        @Attribute('messages')
        ArrayList<String> arrayList

        @Attribute('messages')
        Vector<String> vector

        @Attribute('messages')
        CopyOnWriteArrayList<String> copyOnWriteArrayList
    }

    static class RequiredSetAttribute {
        @Attribute('messages')
        Set<String> set

        @Attribute('messages')
        HashSet<String> hashSet

        @Attribute('messages')
        LinkedHashSet<String> linkedHashSet

        @Attribute('messages')
        TreeSet<String> treeSet

        @Attribute('messages')
        ConcurrentSkipListSet<String> concurrentSkipListSet
    }

    static class RequiredCollectionAttribute {
        @Attribute('messages')
        Collection<String> collection
    }

    static class TransformedAttributes {
        @Attribute('messages')
        @Transform({"${it.asString()} world" as String})
        Collection<String> collection

        @Attribute('messages')
        @Transform({"${it.asString()} world" as String})
        List<String> list

        @Attribute('messages')
        @Transform({"${it.asString()} world" as String})
        Set<String> set
    }
}
