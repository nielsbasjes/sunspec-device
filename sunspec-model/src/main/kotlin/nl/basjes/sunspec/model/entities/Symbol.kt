/*
 * Modbus Schema Toolkit
 * Copyright (C) 2019-2025 Niels Basjes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.basjes.sunspec.model.entities

import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Suppress("unused") // Some SunSpec defined properties are unused
@Serializable
class Symbol(
    @Required
    @SerialName("name")     val name: String,

    @Required
    @SerialName("value")    val value: Int,

    @SerialName("label")    val label: String? = null,
    @SerialName("desc")     val description: String? = null,
    @SerialName("detail")   val detail: String? = null,
    @SerialName("notes")    val notes: String? = null,
    @SerialName("comments") val comments: List<String> = ArrayList(),
) {
    val cleanName = name.trim { it <= ' ' }.replace('-', '_').replace(' ', '_')
}
