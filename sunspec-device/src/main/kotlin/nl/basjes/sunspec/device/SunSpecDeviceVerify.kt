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
