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
package nl.basjes.sunspec.schema.generate

import nl.basjes.modbus.schema.toSchemaDevice
import nl.basjes.modbus.schema.toYaml
import nl.basjes.sunspec.schema.generate.Main.Companion.getSchema
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import kotlin.test.Test
import kotlin.test.assertEquals

class TestGeneratingSunSpecSchema{

    private val log: Logger = LogManager.getLogger()

    @Test
    fun generateSunSpecSchema() {
        val schema = getSchema(DeviceSMASunnyBoy36.device, "description")
        log.info("\n$schema")

        log.info("Creating a new SchemaDevice from the obtained schema")
        val schemaDevice = schema.toSchemaDevice()
        require(schemaDevice.initialize()) { "Unable to initialize schemaDevice" }

        schemaDevice.resolveAllImmutableFields()

        val recreatedSchema = schemaDevice.toYaml()

        assertEquals(schema, recreatedSchema)

        println(recreatedSchema)

    }
}
