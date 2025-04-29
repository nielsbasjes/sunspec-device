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

import io.github.optimumcode.json.schema.JsonSchema
import io.github.optimumcode.json.schema.ValidationError
import kotlinx.serialization.json.Json
import nl.basjes.sunspec.SUNSPEC_MODEL_FILENAMES
import nl.basjes.sunspec.SUNSPEC_SCHEMA_FILENAME
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertNotNull

internal class TestJsonSchema {

    @Test
    fun testModelsAgainstSchema() {
        // Load the schema
        val modelResource = this.javaClass.classLoader.getResource(SUNSPEC_SCHEMA_FILENAME)
            ?: throw IllegalStateException("Unable to locate $SUNSPEC_SCHEMA_FILENAME ... this should never happen")
        val sunSpecSchema = JsonSchema.fromDefinition(modelResource.readText())
        assertNotNull(sunSpecSchema, "Unable to parse schema.json")

        for (modelFileName in SUNSPEC_MODEL_FILENAMES) {
            val sunSpecModelJson =
                try {
                    this.javaClass.classLoader.getResource(modelFileName)?.readText()
                        ?: throw IllegalStateException("Unable to locate $modelFileName ... this should never happen")
                } catch (e: IOException) {
                    throw RuntimeException(e)
                }
            val errors = mutableListOf<ValidationError>()
            val valid = sunSpecSchema.validate(Json.parseToJsonElement(sunSpecModelJson), errors::add)

            errors.forEach {
                println("${it.message} - ${it.schemaPath}")
            }
            require(valid)
        }
    }
}
