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

object Utils {
    /**
     * Various fixes to make the SunSpec usable in code.
     *
     * @param model The SunSpecModel that needs to be fixed.
     */
    fun fixProblemsInStandardSunSpec(model: SunSpecModel) {
        fixProblemsInStandardSunSpec(model.group)
        fixUnclearLabelsInStandardSunSpec(model)
        markTimestampFields(model)
    }

    private fun fixProblemsInStandardSunSpec(group: Group) {
        for (subGroup in group.groups) {
            fixProblemsInStandardSunSpec(subGroup)
        }

        for (point in group.points) {

            val label =  point.label
            if (label != null) {
                // Typo as reported here https://github.com/sunspec/models/issues/258
                point.label = label.replace("Sting", "String")
            }

            var description = point.description
            if (description != null) {
                description = description
                    .replace(Regex("^Bit Mask indicating (.*)$"), "$1 (Bitmask)")
                    .replace("Bit flags.", "")
                    .replace("Bitfield value.", "")
                    .replace("Bitmask value.", "")
                    .replace("Bitmask values.", "")
                    .replace("Bitmask value", "")
                    .replace("Bitmask values", "")
                    .replace("Enumerated value indicates if curve is ", "Curve is ")
                    .replace("Enumerated value.", "")
                    .replace("Enumerated valued.", "")
                    .replace("Enumerated value", "")
                    .replaceFirstChar { it.uppercase() }

                    .trim { it <= ' ' }
                description = "$description..."
                description = description
                    .replace("\\.+$".toRegex(), ".")
                    .replace(" +".toRegex(), " ")
                point.description = description
            }
        }
    }

    private fun fixUnclearLabelsInStandardSunSpec(model: SunSpecModel) {
        if (model.id == 702) {
            for (point in model.group.points) {
                when (point.name) {
                    // Fix some unclear labels
                    "IntIslandCatRtg" -> point.label = "Intentional Island Categories Rating"
                    "WUndExtRtgPF" -> point.label = "Specified Under-Excited Rating Power Factor"
                    "WOvrExtRtgPF" -> point.label = "Specified Over-Excited Rating Power Factor"
                    else -> {}
                }
            }
        }
    }

    private fun markPointAsTimestamp(point: Point) {
        // Letting it fail hard here to ensure it is always valid
        require(point.type == Point.Type.UINT_32) { "Any type that should be a Timestamp MUST be in the schema as a uint32" }
        point.type = Point.Type.TIMESTAMP
        point.description += " Converted to standard UNIX Epoch in Milliseconds."
    }

    private fun markTimestampFields(model: SunSpecModel) {
        when (model.id) {
            3,4,5,6,7,9,220
                -> model.group.points
                    .find { it.name == "Ts" }
                    ?.let { markPointAsTimestamp(it) }
            122,501,502
                -> model.group.points
                    .find { it.name == "Tms" }
                    ?.let { markPointAsTimestamp(it) }
            133 -> model.group.groups
                    .flatMap { group -> group.points }
                    .find { it.name == "StrTms" }
                    ?.let { markPointAsTimestamp(it) }
        }
    }

}
