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

import kotlinx.serialization.json.Json
import nl.basjes.sunspec.SUNSPEC_MODEL_FILENAMES
import nl.basjes.sunspec.model.entities.Group
import nl.basjes.sunspec.model.entities.SunSpecModel
import java.io.IOException

class SunSpec {
    val models: Map<Int, SunSpecModel>

    /**
     * @param modelNr The number of the model for which we need the definition
     * @return The instance of SunSpecModel for the requested model, or null if non-existent.
     */
    fun getModel(modelNr: Int): SunSpecModel? {
        return models[modelNr]
    }

    init {
        // isLenient is needed because sometimes count is an Integer and sometimes a String.
        val json = Json { isLenient = true }

        val sunSpecModels = SUNSPEC_MODEL_FILENAMES
            .map { modelFileName: String ->
                try {
                    val modelResource = this.javaClass.classLoader.getResource(modelFileName)
                        ?: throw IllegalStateException("Unable to locate $modelFileName ... this should never happen")
                    json.decodeFromString<SunSpecModel>(modelResource.readText())
                } catch (e: IOException) {
                    throw RuntimeException(e)
                }
            }
            .associateBy { it.id }
            .toMutableMap()

        val sunSHeaderGroup = Group(
                name        = "SunSHeader",
                type        = Group.Type.GROUP,
                label       = "Empty Group",
                description = "The SunS Header Model has no Points")
        val sunSHeaderModel = SunSpecModel(
                id          = 0,
                group       = sunSHeaderGroup,
                label       = "Start of the SunSpec Model chain",
                description = "The starting value of the chain of SunSpec models of a device")
        sunSpecModels[0] = sunSHeaderModel

        val endOfChainGroup = Group(
                name        = "EndOfChain",
                type        = Group.Type.GROUP,
                label       = "Empty Group",
                description = "The End of Chain Model has no Points")
        val endOfChainModelFFFF = SunSpecModel(
                id          = 0xFFFF,
                group       = endOfChainGroup,
                label       = "End of the SunSpec Model chain",
                description = "The closing empty model of chain of SunSpec models of a device")
        sunSpecModels[0XFFFF] = endOfChainModelFFFF

        sunSpecModels.values.forEach { obj: SunSpecModel -> obj.init() }

        models = sunSpecModels.toSortedMap()
    }
}
