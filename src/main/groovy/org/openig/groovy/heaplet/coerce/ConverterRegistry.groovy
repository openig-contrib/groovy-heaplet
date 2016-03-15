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

/**
 * Created by guillaume on 02/03/16.
 */
class ConverterRegistry {
    def converters = [:]

    def <T> void register(Class<T> type, Converter<T> converter) {
        converters[type] = converter
    }

    def unregister(Class<?> type) {
        converters.remove(type)
    }

    def Converter<?> find(Class<?> type) {
        converters[type]
    }

    def Converter<?> find(Closure<?> closure) {
        converters.find closure
    }
}
