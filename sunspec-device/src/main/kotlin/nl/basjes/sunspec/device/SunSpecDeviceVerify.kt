/*
 * Modbus Schema Toolkit
 * Copyright (C) 2019-2026 Niels Basjes
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

import nl.basjes.modbus.schema.Block
import nl.basjes.modbus.schema.SchemaDevice
import nl.basjes.modbus.schema.utils.StringTable

/**
 * Verify if the specified device models are valid
 */
fun verify(schemaDevice: SchemaDevice) {
    verifyBlockOverlap(schemaDevice)
}


fun verifyBlockOverlap(schemaDevice: SchemaDevice) {
    val table = StringTable().withHeaders("Field 1", "Field 2", "Overlapping Address")
    var hasOverlap = false
    for (blockPair in schemaDevice.blocks.zipWithNext()) {
        hasOverlap = hasOverlap || verifyBlockOverlap(table, blockPair.first, blockPair.second)
    }
    if (hasOverlap) {
        println("Device has overlapping models (i.e. bad SunSpec implementation):\n$table")
    }
}

private fun verifyBlockOverlap(table: StringTable, firstBlock: Block, secondBlock: Block): Boolean {
    var haveOverlap = false
    val secondBlockAddresses = secondBlock.fields.flatMap { field -> field.requiredAddresses }.sorted()
    for (field in firstBlock.fields) {
        for (address in field.requiredAddresses) {
            if (secondBlockAddresses.contains(address)) {
                // Found it
                val secondFields = secondBlock.fields.filter { field -> field.requiredAddresses.contains(address) }
                for (secondField in secondFields) {
                    table.addRow("${firstBlock.id}: ${field.id}", "${secondField.block.id}: ${secondField.id}", "$address")
                    haveOverlap = true
                }
            }
        }
    }
    return haveOverlap
}
