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
import nl.basjes.sunspec.model.entities.SunSpecModel
import org.opentest4j.AssertionFailedError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class TestModelStructure {
    @Test
    fun ensureEnoughModels() {
        assertTrue(
            sunSpec.models.size > 100,
            "Unable to read enough models (Got ${sunSpec.models.size}).",
        )
        println("Got ${sunSpec.models.size} models.")
    }

    @Test
    fun checkModelId() {
        sunSpec.models
            .forEach { (id, model) -> assertEquals(id, model.id, "Index vs model id mismatch") }
    }

    // --------------------------------------------------
    @Test
    fun checkAllCodeNamesAreUniqueWithin() {
        sunSpec.models.values
            .forEach(this::checkAllCodeNamesAreUnique)
    }

    private fun checkAllCodeNamesAreUnique(model: SunSpecModel) {
        // Recursively check the subgroups (if any)
        try {
            checkAllCodeNamesAreUnique(model.group)
        } catch (afe: AssertionFailedError) {
            throw AssertionFailedError("Problem in model " + model.id, afe.expected, afe.actual, afe)
        }
    }

    private fun checkAllCodeNamesAreUnique(group: Group) {
        val points: List<Point> = group.points
        val allPointNames: List<String> =
            points
                .map { it.name }
                .filter { it.isNotBlank() } // Some Points (padding) do not return a camelcase name.
                .sorted()
        val allPointNamesUnique: List<String> = allPointNames.distinct().sorted()
        assertEquals(allPointNamesUnique, allPointNames, "Duplicate CamelCase names in model " + group.name)

        group.groups.forEach(this::checkAllCodeNamesAreUnique)
    }

    // --------------------------------------------------
    @Test
    fun checkAllScalingFactorsExist() {
        sunSpec.models.values.forEach(this::checkAllScalingFactorsExist)
    }

    private fun checkAllScalingFactorsExist(sunSpecModel: SunSpecModel) {
        val fixedBlockScalingFactorsNames = getScalingFactors(sunSpecModel.group.points)
        val modelLogName: String = String.format("Model %5d", sunSpecModel.id)

        // Check all points in the fixed model
        checkPointsScalingFactor(modelLogName, sunSpecModel.group.points, fixedBlockScalingFactorsNames)

        // Check all points in the repeating models model
        for (group in sunSpecModel.group.groups) {
            checkPointsScalingFactor(
                modelLogName,
                group.points,
                fixedBlockScalingFactorsNames + getScalingFactors(group.points),
            )
        }
    }

    private fun getScalingFactors(points: List<Point>): List<String> =
        points
            .filter { point -> point.type == Point.Type.SUNSSF }
            .map { it.name }
            .toList()

    private fun checkPointsScalingFactor(modelLogName: String, points: List<Point>, scalingFactorsNames: List<String>) {
        points
            .filter { it.type != Point.Type.SUNSSF }
            .map { it.scalingFactor }
            .filter { !it.isNullOrEmpty() }
            .forEach {
                requireNotNull(it) { "Smart cast doesn't see the filter ?!?!?" }
                try {
                    val scalingFactor: Int = it.toInt()
                    assertTrue(
                        scalingFactor >= -10 && scalingFactor <= 10,
                        "$modelLogName invalid numerical scaling factor",
                    )
                } catch (_: NumberFormatException) {
                    assertTrue(
                        scalingFactorsNames.contains(it),
                        "$modelLogName no scaling factor $it exists.",
                    )
                }
            }
    }

    // --------------------------------------------------
    @Test
    fun checkRegisterCountPerType() {
        sunSpec.models.values.forEach(this::checkRegisterCountPerType)
    }

    private fun checkRegisterCountPerType(sunSpecModel: SunSpecModel) {
        val modelLogName: String = String.format("Model %5d", sunSpecModel.id)
        checkRegisterCountPerType(modelLogName, sunSpecModel.group.points)
        for (group in sunSpecModel.group.groups) {
            checkRegisterCountPerType(modelLogName, group.points)
        }
    }

    private fun checkRegisterCountPerType(modelLogName: String, points: List<Point>) {
        for (point in points) {
            val registerCount: Int = point.type.registerCount
            if (registerCount == 0) {
                assertEquals(
                    Point.Type.STRING,
                    point.type,
                    "Only a string may have a variable number of registers",
                )
                assertTrue(point.size > 1, "Strings are expected to be a string " + point.size)
            } else {
                assertEquals(
                    registerCount,
                    point.size,
                    String.format(
                        "%s: Point with type/size mismatch: %s",
                        modelLogName,
                        point,
                    ),
                )
            }
        }
    }

    // --------------------------------------------------
    @Test
    fun checkSizesOfPointsInModel() {
        sunSpec.models.values.forEach(this::checkSizesOfPointsInModel)
    }

    private fun checkSizesOfPointsInModel(sunSpecModel: SunSpecModel) {
        val modelNr: Int = sunSpecModel.id
        val modelSize = modelSizes[modelNr]
        if (modelSize != null) {
            val registersInFixedBlock: Int = sunSpecModel.group.dataSize

            val registersInRepeatingBlock: Int = sunSpecModel.group.groups.sumOf { it.dataSize }

            assertEquals(
                modelSize.fixedBlock,
                registersInFixedBlock,
                String.format("Model %d: Invalid fixed block size", sunSpecModel.id),
            )

            assertEquals(
                modelSize.repeatingBlock,
                registersInRepeatingBlock,
                String.format("Model %d: Invalid repeating block size", sunSpecModel.id),
            )
        }
    }

    private data class ModelSize(
        val model: Int = 0,
        val fixedBlock: Int = 0,
        val repeatingBlock: Int = 0,
    )

    // --------------------------------------------------
    @Test
    fun checkValidNumberOfGroups() {
        sunSpec.models.values.forEach(this::checkValidNumberOfGroups)
    }

    private fun checkValidNumberOfGroups(sunSpecModel: SunSpecModel) {
        val groups: List<Group> = sunSpecModel.group.groups
        if (groups.isEmpty()) {
            // All is fine
            return
        }

        var groupTypeCount = 0
        var syncTypeCount = 0
        for (group in groups) {
            when (group.type) {
                Group.Type.GROUP -> groupTypeCount++
                Group.Type.SYNC -> syncTypeCount++
            }
        }

        assertTrue(groupTypeCount == 0 || syncTypeCount == 0, "Cannot mix group and sync")
    }

    companion object {
        private val sunSpec = SunSpec()

        // FIXME: Values obtained from the XML representation of the model.
        //        So not all models from the JSon are present (i.e. not the 7xx range yet)
        private val modelSizes =
            listOf(
                ModelSize(1, 66, 0),
                ModelSize(2, 14, 0),
                ModelSize(3, 58, 1),
                ModelSize(4, 60, 1),
                ModelSize(5, 88, 1),
                ModelSize(6, 90, 1),
                ModelSize(7, 10, 1),
                ModelSize(8, 2, 1),
                ModelSize(9, 92, 1),
                ModelSize(10, 4, 0),
                ModelSize(11, 13, 0),
                ModelSize(12, 98, 0),
                ModelSize(13, 174, 0),
                ModelSize(14, 52, 0),
                ModelSize(15, 24, 0),
                ModelSize(16, 52, 0),
                ModelSize(17, 12, 0),
                ModelSize(18, 22, 0),
                ModelSize(19, 30, 0),
                ModelSize(101, 50, 0),
                ModelSize(102, 50, 0),
                ModelSize(103, 50, 0),
                ModelSize(111, 60, 0),
                ModelSize(112, 60, 0),
                ModelSize(113, 60, 0),
                ModelSize(120, 26, 0),
                ModelSize(121, 30, 0),
                ModelSize(122, 44, 0),
                ModelSize(123, 24, 0),
                ModelSize(124, 24, 0),
                ModelSize(125, 8, 0),
                ModelSize(126, 10, 54),
                ModelSize(127, 10, 0),
                ModelSize(128, 14, 0),
                ModelSize(129, 10, 50),
                ModelSize(130, 10, 50),
                ModelSize(131, 10, 54),
                ModelSize(132, 10, 54),
                ModelSize(133, 6, 60),
                ModelSize(134, 10, 58),
                ModelSize(135, 10, 50),
                ModelSize(136, 10, 50),
                ModelSize(137, 10, 50),
                ModelSize(138, 10, 50),
                ModelSize(139, 10, 50),
                ModelSize(140, 10, 50),
                ModelSize(141, 10, 50),
                ModelSize(142, 10, 50),
                ModelSize(143, 10, 50),
                ModelSize(144, 10, 50),
                ModelSize(145, 8, 0),
                ModelSize(160, 8, 20),
                ModelSize(201, 105, 0),
                ModelSize(202, 105, 0),
                ModelSize(203, 105, 0),
                ModelSize(204, 105, 0),
                ModelSize(211, 124, 0),
                ModelSize(212, 124, 0),
                ModelSize(213, 124, 0),
                ModelSize(214, 124, 0),
                ModelSize(220, 42, 1),
                ModelSize(302, 0, 5),
                ModelSize(303, 0, 1),
                ModelSize(304, 0, 6),
                ModelSize(305, 36, 0),
                ModelSize(306, 4, 0),
                ModelSize(307, 11, 0),
                ModelSize(308, 4, 0),
                ModelSize(401, 14, 8),
                ModelSize(402, 20, 14),
                ModelSize(403, 16, 8),
                ModelSize(404, 25, 14),
                ModelSize(501, 31, 0),
                ModelSize(502, 28, 0),
                ModelSize(601, 26, 22),
                ModelSize(801, 1, 0),
                ModelSize(802, 62, 0),
                ModelSize(803, 26, 32),
                ModelSize(804, 46, 16),
                ModelSize(805, 42, 4),
                ModelSize(806, 1, 1),
                ModelSize(807, 34, 24),
                ModelSize(808, 1, 1),
                ModelSize(809, 1, 1),
                ModelSize(63001, 134, 18),
                ModelSize(63002, 0, 4),
                ModelSize(64001, 71, 0),
                ModelSize(64020, 30, 16),
                ModelSize(64101, 7, 0),
                ModelSize(64110, 282, 0),
                ModelSize(64111, 23, 0),
                ModelSize(64112, 64, 0),
            ).associateBy { it.model }
                .toSortedMap()
    }
}
