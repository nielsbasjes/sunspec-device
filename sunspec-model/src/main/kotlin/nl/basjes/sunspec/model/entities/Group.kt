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

@Serializable
class Group(
    @Required
    @SerialName(value = "name")     var name:           String,

    @Required
    @SerialName(value = "type")     var type:           Type,

    /** The Integer OR the NAME of the count point OR null */
    @SerialName("count")            var count:          String?      = null,
    @SerialName("points")           val points:         List<Point>  = listOf(),
    @SerialName("groups")           val groups:         List<Group>  = listOf(),
    @SerialName("label")            var label:          String?      = null,
    @SerialName("desc")             var description:    String?      = null,
    @SerialName("detail")           val detail:         String?      = null,
    @SerialName("notes")            val notes:          String?      = null,
    @SerialName("comments")         val comments:       List<String> = listOf(),
    @SerialName("standards")        val standards:      List<String> = listOf(),
) {

    fun init() {
        // Set the right offset values in each of the direct points in this group
        // and calculate the total size of the direct points.
        var totalSize = 0
        headerSize = 0
        dataSize = 0
        points
            .forEach {
                it.offsetInGroup = totalSize
                totalSize += it.size
                if (it.isModelHeader) {
                    headerSize += it.size
                } else {
                    dataSize += it.size
                }
            }
        groups.forEach { it.init() }
    }

    override fun toString(): String {
        return "Group(name='$name', type=$type, label=$label)"
    }

    /** The number of registers in the SunSpec Model header (i.e. the ID and L if present). */
    var headerSize = 0
        private set

    /** The number of registers in the real data registers (excluding the ID and L if present) that are in the Points DIRECTLY in this Group. */
    var dataSize = 0
        private set

    @Serializable
    enum class Type(private val value: String) {
        @SerialName("group")        GROUP("group"),
        @SerialName("sync")         SYNC("sync");
    }
}
