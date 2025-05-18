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
package nl.basjes.sunspec.device

import nl.basjes.modbus.device.api.Address
import nl.basjes.modbus.schema.Block
import nl.basjes.modbus.schema.Field
import nl.basjes.modbus.schema.SchemaDevice

object Utils {
    fun addSunSHeaderBlock(schemaDevice: SchemaDevice?, sunSpecStartAddress: Address): Field {
        // Create the root block with only the SunS header field.
        val sunSHeaderBlock =
            Block
                .builder()
                .schemaDevice(schemaDevice!!)
                .id("SunSpecHeader")
                .description("The header that starts the SunSpec model list")
                .build()

        return Field
            .builder()
            .block(sunSHeaderBlock)
            .id("SunS")
            .description("The SunS header")
            .immutable(true)
            .system(true)
            .expression("utf8($sunSpecStartAddress#2)")
            .build()
    }

    fun addSunSpecEndOfChainBlock(schemaDevice: SchemaDevice?): Block {
        // Create the closing block without any fields.
        return Block
            .builder()
            .schemaDevice(schemaDevice!!)
            .id("EndOfModelChain")
            .description("The final marker that closes the SunSpec model list")
            .build()
    }

    fun addModelHeaderFields(block: Block, thisModelAddress: Address): Pair<Field, Field> {
        // ALL Models start with an ID and a Length field that is the same in all cases
        val modelIdField =
            Field
                .builder()
                .block(block)
                .id("ID")
                .description("Model identifier")
                .immutable(true)
                .expression("uint16($thisModelAddress)")
                .system(true)
                .build()

        val modelLengthField =
            Field
                .builder()
                .block(block)
                .id("L")
                .description("Model length")
                .immutable(true)
                .expression("uint16(" + thisModelAddress.increment() + ")")
                .system(true)
                .build()
        return Pair(modelIdField, modelLengthField)
    }
}
