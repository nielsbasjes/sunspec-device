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
package nl.basjes.sunspec.model

import nl.basjes.sunspec.model.entities.Group
import nl.basjes.sunspec.model.entities.Point
import nl.basjes.sunspec.model.entities.Point.Type
import nl.basjes.sunspec.model.entities.SunSpecModel
import org.opentest4j.AssertionFailedError
import java.util.Locale
import kotlin.test.Test
import kotlin.text.contains
import kotlin.text.lowercase

class TestTimestampFix {

    // --------------------------------------------------
    @Test
    fun checkIfNoTimestampFieldsAreMissed() {
        sunSpec.models.values
            .forEach(this::checkIfNoTimestampFieldsAreMissed)
    }

    private fun checkIfNoTimestampFieldsAreMissed(model: SunSpecModel) {
        // Recursively check the subgroups (if any)
        try {
            checkIfNoTimestampFieldsAreMissed(model.group)
        } catch (afe: AssertionFailedError) {
            throw AssertionFailedError("Problem in model " + model.id, afe.expected, afe.actual, afe)
        }
    }

    /**
     * @return true if this point is a number and means the number of seconds since 2000-01-01 00:00:00 UTC
     */
    fun isTimeInstant(point: Point): Boolean {
            val desc = point.description?.lowercase(Locale.getDefault())
            if (point.type == Type.UINT_32 && desc != null) {
                return (desc.contains("2000") && desc.contains("seconds"))
            }
            return false
        }

    private fun checkIfNoTimestampFieldsAreMissed(group: Group) {
        group.points.forEach { require(!isTimeInstant(it)) { "We have point $it in group ${group.name} is a timestamp" } }
        group.groups.forEach { checkIfNoTimestampFieldsAreMissed(it) }
    }

    companion object {
        private val sunSpec = SunSpec()
    }
}
