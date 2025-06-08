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

/**
 * JSON Schema for SunSpec information model definition
 */
@Serializable
class SunSpecModel (
    @Required
    @SerialName(value = "id")       val id: Int,

    @Required
    @SerialName(value = "group")    val group: Group,

    @SerialName("label")            val label:       String?       = null,
    @SerialName("desc")             val description: String?       = null,
    @SerialName("detail")           val detail:      String?       = null,
    @SerialName("notes")            val notes:       String?       = null,
    @SerialName("comments")         val comments:    List<String>? = null,
) {
    val cleanLabel        = label       ?: group.label
    val cleanDescription  = description ?: group.description
    val cleanDetail       = detail      ?: group.detail
    val cleanNotes        = notes       ?: group.notes
    val cleanComments     = comments    ?: group.comments

    fun init() {
        FixesAndImprovements.fixProblemsInStandardSunSpec(this)

        // Mark the 2 model header points as such.
        if (group.points.size >= 2) {
            if (group.points[0].name == "ID" &&
                group.points[1].name == "L") {
                group.points[0].isModelHeader = true
                group.points[1].isModelHeader = true
            }
        }
        group.init()
    }

    override fun toString() = "SunSpecModel(id=$id, label=$cleanLabel)"
}
