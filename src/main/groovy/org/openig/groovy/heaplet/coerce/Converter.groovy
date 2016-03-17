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

import org.forgerock.json.JsonValue
import org.forgerock.openig.heap.HeapException
import org.forgerock.util.Function

/**
 * Generic {@link Function} ({@literal Function<JsonValue, T, HeapException>}) to transform
 * a given {@link JsonValue} into a given {@literal T} type.
 *
 * @param T Converted type (output)
 */
interface Converter<T> extends Function<JsonValue, T, HeapException> {}
