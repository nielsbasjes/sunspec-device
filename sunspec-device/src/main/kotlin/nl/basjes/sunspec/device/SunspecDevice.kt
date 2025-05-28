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
import nl.basjes.modbus.device.api.MODBUS_MAX_REGISTERS_PER_REQUEST
import nl.basjes.modbus.device.api.ModbusDevice
import nl.basjes.modbus.device.api.RegisterValue
import nl.basjes.modbus.device.exception.ModbusException
import nl.basjes.modbus.schema.Block
import nl.basjes.modbus.schema.Field
import nl.basjes.modbus.schema.SchemaDevice
import nl.basjes.modbus.schema.exceptions.ModbusSchemaParseException
import nl.basjes.modbus.schema.get
import nl.basjes.modbus.schema.utils.StringTable
import nl.basjes.sunspec.SUNSPEC_MODEL_ID_REGISTERS
import nl.basjes.sunspec.SUNSPEC_MODEL_L_REGISTERS
import nl.basjes.sunspec.SUNS_CHAIN_END_MODEL_ID
import nl.basjes.sunspec.SUNS_HEADER_MODEL_ID
import nl.basjes.sunspec.device.SunSpecDeviceModelFinder.findDeviceSunSpecModels
import nl.basjes.sunspec.device.Utils.addModelHeaderFields
import nl.basjes.sunspec.model.SunSpec
import nl.basjes.sunspec.model.entities.Group
import nl.basjes.sunspec.model.entities.Point
import nl.basjes.sunspec.model.entities.Point.Type.ACC_16
import nl.basjes.sunspec.model.entities.Point.Type.ACC_32
import nl.basjes.sunspec.model.entities.Point.Type.ACC_64
import nl.basjes.sunspec.model.entities.Point.Type.BITFIELD_16
import nl.basjes.sunspec.model.entities.Point.Type.BITFIELD_32
import nl.basjes.sunspec.model.entities.Point.Type.BITFIELD_64
import nl.basjes.sunspec.model.entities.Point.Type.COUNT
import nl.basjes.sunspec.model.entities.Point.Type.ENUM_16
import nl.basjes.sunspec.model.entities.Point.Type.ENUM_32
import nl.basjes.sunspec.model.entities.Point.Type.EUI_48
import nl.basjes.sunspec.model.entities.Point.Type.FLOAT_32
import nl.basjes.sunspec.model.entities.Point.Type.FLOAT_64
import nl.basjes.sunspec.model.entities.Point.Type.INT_16
import nl.basjes.sunspec.model.entities.Point.Type.INT_32
import nl.basjes.sunspec.model.entities.Point.Type.INT_64
import nl.basjes.sunspec.model.entities.Point.Type.IPADDR
import nl.basjes.sunspec.model.entities.Point.Type.IPV_6_ADDR
import nl.basjes.sunspec.model.entities.Point.Type.PAD
import nl.basjes.sunspec.model.entities.Point.Type.RAW_16
import nl.basjes.sunspec.model.entities.Point.Type.STRING
import nl.basjes.sunspec.model.entities.Point.Type.SUNSSF
import nl.basjes.sunspec.model.entities.Point.Type.TIMESTAMP
import nl.basjes.sunspec.model.entities.Point.Type.UINT_16
import nl.basjes.sunspec.model.entities.Point.Type.UINT_32
import nl.basjes.sunspec.model.entities.Point.Type.UINT_64
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.time.Instant
import java.util.Locale
import java.util.TreeMap

@Suppress("ktlint:standard:comment-wrapping", "ktlint:standard:max-line-length", "ktlint:standard:paren-spacing")
object SunspecDevice {
    private val LOG: Logger = LogManager.getLogger()

    private fun getFirstRegisterValue(field: Field): RegisterValue? {
        field.initialize()
        val parsedExpression = field.parsedExpression ?: return null  // Nothing to do
        val registerValues = parsedExpression.getRegisterValues(field.block.schemaDevice)
        if (registerValues.isEmpty()) {
            return null // Nothing to do
        }
        return registerValues[0]
    }

    private fun setCommentOnFirstRegisterValue(field: Field, comment: String) {
        getFirstRegisterValue(field)?.comment = comment
    }

    /**
     * Determine the Sunspec Schema for the provided device
     * @param modbusDevice The ModbusDevice for which the Schema is needed
     * @return The SunSpec schema device for this specific device.
     */
    @JvmStatic
    @Throws(ModbusException::class)
    fun generate(
        modbusDevice: ModbusDevice,
        /**
         * A human-readable description of this schema device.
         */
        description: String = "",
        /**
         * Some devices have non-standard models in their SunSpec.
         * With this you can skip them in the generated schema device.
         */
        skipUnknownModels: Boolean = false,
    ): SchemaDevice? {
        // We need 2 things:
        // - The official SunSpec schemas
        // - The actual device to know what it supports.
        val sunSpec = SunSpec()

        // Get the list of all SunSpec model actually present in this device
        val deviceSunSpecModels = findDeviceSunSpecModels(modbusDevice)

        if (deviceSunSpecModels.isEmpty()) {
            LOG.error("Unable to find any SunSpec models")
            return null
        }

        val table = StringTable()
        table.withHeaders(
            "Model",
            "Address",
            "Length",
            "Label",
        )
        for (deviceSunSpecModel in deviceSunSpecModels) {
            val model = sunSpec.getModel(deviceSunSpecModel.id)
            if (model == null) {
                table.addRow(
                    deviceSunSpecModel.id.toString(),
                    deviceSunSpecModel.address.toCleanFormat(),
                    deviceSunSpecModel.registers.toString(),
                    "Non existent model",
                )
            } else {
                table.addRow(
                    deviceSunSpecModel.id.toString(),
                    deviceSunSpecModel.address.toCleanFormat(),
                    deviceSunSpecModel.registers.toString(),
                    model.cleanLabel ?: "",
                )
            }
        }
        LOG.info("Detected models: \n{}", table)

        val schemaDevice = SchemaDevice(description)
        schemaDevice.connect(modbusDevice)

        for (deviceSunSpecModel in deviceSunSpecModels) {
            val modelId = deviceSunSpecModel.id
            val modelLength = deviceSunSpecModel.registers
            val modelAddress = deviceSunSpecModel.address

            // Special case is the header
            if (modelId == SUNS_HEADER_MODEL_ID) {
                val sunSHeaderBlock = Utils.addSunSHeaderBlock(schemaDevice, deviceSunSpecModel.address)
                setCommentOnFirstRegisterValue(sunSHeaderBlock, "--------------------------------------\nSunS header")
                continue
            }

            // Special case is the end of the chain
            if (modelId == SUNS_CHAIN_END_MODEL_ID) {
                val endChainBlock = Utils.addSunSpecEndOfChainBlock(schemaDevice)
                val (endChainId, _) = Utils.addModelHeaderFields(endChainBlock, modelAddress)
                setCommentOnFirstRegisterValue(endChainId, "--------------------------------------\nNO MORE MODELS")
                break // This should be the last in the chain
            }

            // Find the official SunSpec specification for this one.
            val sunSpecModel = sunSpec.getModel(modelId)

            if (sunSpecModel == null) {
                LOG.fatal("Unable to get model for ID: {}", modelId)
                if (!skipUnknownModels) {
                    createBlockForNonExistentModel(schemaDevice, modelId, modelLength, modelAddress)
                }
                continue
            }

            // Create and add a new Block for this SunSpec model.
            val modelDescription =
                if (sunSpecModel.group.description.isNullOrBlank()) {
                    "[Model " + sunSpecModel.id + "]:" + sunSpecModel.group.label
                } else {
//                    "[Model " + sunSpecModel.id + "]:" +
                    sunSpecModel.group.label + ": " + sunSpecModel.group.description
                }

            val block =
                Block
                    .builder()
                    .schemaDevice(schemaDevice)
                    .id("Model " + sunSpecModel.id)
                    .description(modelDescription)
                    .build()

            // This is the root of an old style model with a repeating group
            val subGroupCount = if (sunSpecModel.group.groups.size == 1) { sunSpecModel.group.groups[0].count } else { "Computer says no" }
            if (subGroupCount == null || subGroupCount == "0") {
                val subGroup = sunSpecModel.group.groups[0]
                if (subGroup.dataSize == 0) {
                    throw ModbusException("SunSpec repeat block problem")
                }

                val count = (modelLength - sunSpecModel.group.dataSize) / subGroup.dataSize

                // Check for rounding errors
                require(sunSpecModel.group.dataSize + (subGroup.dataSize * count) == modelLength) {
                    throw ModbusException(
                        "SunSpec repeat block problem: ${sunSpecModel.group.dataSize} + (${subGroup.dataSize} * $count) == $modelLength)",
                    )
                }

                // Add the Points that are directly in this model.
                var nextFieldAddress = addGroupPoints(sunSpecModel.group, modelAddress, block, modelId, modelLength, "", true)
                for (subGroupIndex in 0 until count) {
                    nextFieldAddress = addGroup(
                        block,
                        "${subGroup.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}_${subGroupIndex}_",
                        subGroup,
                        nextFieldAddress,
                        modelId,
                        null,
                        false,
                    )
                }
            } else {
                addGroup(block, "", sunSpecModel.group, modelAddress, modelId, modelLength, true)
            }
        }

        if (!schemaDevice.initialize()) {
            throw ModbusException("Unable to initialize the schema device")
        }

        // Determine a better description if none was set
        if (description.isBlank()) {
            schemaDevice["Model 1"]?.let {
                val manufacturer = it["Mn"]
                val model        = it["Md"]
                val serialNr     = it["SN"]
                val version      = it["Vr"]
                requireNotNull(manufacturer)
                requireNotNull(model)
                requireNotNull(serialNr)
                requireNotNull(version)

                manufacturer.need()
                model.need()
                serialNr.need()
                version.need()
                schemaDevice.update(1000)
                schemaDevice.description =
                    "A schema specifically for the SunSpec device made by ${manufacturer.stringValue} model ${model.stringValue} using version ${version.stringValue} (SN: ${serialNr.stringValue})"
            }
        }

        return schemaDevice
    }

    private fun addGroupPoints(group: Group, startFieldAddress: Address, block: Block, modelId: Int, modelLength: Int?, prefix: String, addComments: Boolean): Address {
        var nextFieldAddress = startFieldAddress
        var pointNrInModel = 0 // Needed for putting the right comment on the registers
        var didFirstDataComment = false

        val forceFetchGroup =
            if (group.type == Group.Type.SYNC) {
                // This means ALL points in this group must be fetched in a single modbus call
                if (group.groups.isNotEmpty()) {
                    throw ModbusSchemaParseException("It is impossible to do a SunSpec SYNC if there are sub groups")
                }
                true
            } else {
                false
            }

        val fetchGroupId = "<<Group SYNC for ${group.name} at ${nextFieldAddress.toCleanFormat()}>>"

        for (point in group.points) {
            pointNrInModel++
            val field = createAndAddFieldToModel(block, nextFieldAddress, point, prefix)
            if (forceFetchGroup) {
                field.fetchGroup = fetchGroupId
            }
            nextFieldAddress = nextFieldAddress.increment(point.size)
            if (addComments) {
                if (point.isModelHeader) {
                    if (point.name == "ID") {
                        getFirstRegisterValue(field)?.let {
                            it.comment =
                                "--------------------------------------\n" +
                                    "Model $modelId [Header @ ${it.address.toCleanFormat()}]: ${group.label}"
                        }
                    }
                } else {
                    if (!didFirstDataComment) {
                        didFirstDataComment = true
                        getFirstRegisterValue(field)?.let {
                            it.comment = "Model $modelId [Data @ ${it.address.toCleanFormat()} - ${
                                it.address.increment((modelLength ?: 1)-1).toCleanFormat()
                            }]: $modelLength registers"
                        }
                    }
                }
            }

// This will comment each field
//            getFirstRegisterValue(field)?.let {
//                if (it.comment == null) {
//                    it.comment = ""
//                } else {
//                    if (!it.comment.isNullOrBlank()) {
//                        it.comment += "\n"
//                    }
//                }
//                val endAddress = if (point.size>1) " - ${it.address.increment(point.size-1).toCleanFormat()}" else ""
//                it.comment += "Field \"${field.id}\" [@ ${it.address.toCleanFormat()}${endAddress}]: ${point.size} registers : ${field.parsedExpression}"
//            }
        }
        return nextFieldAddress
    }

    private fun determineCount(block: Block, countString: String?): Int {
        // The "count" of a subgroup can be a number OR the NAME of the field which contains the number.
        if (countString.isNullOrBlank()) {
            return 1
        }
        try {
            return countString.toInt()
        } catch (_: NumberFormatException) {
            val countField =
                block.getField(countString)
                    ?: throw ModbusException("Unable to find the count field named \"$countString\" for this group")
            countField.initialize()
            countField.update()
            val countValue =
                countField.longValue
                    ?: throw ModbusException("Unable to read the value of the count field named \"$countString\" for this group")
            return Math.toIntExact(countValue)
        }
    }

    @Throws(ModbusException::class)
    private fun addGroup(
        block: Block,
        prefix: String,
        group: Group,
        startFieldAddress: Address,
        modelId: Int,
        modelLength: Int?,
        addComments: Boolean,
    ): Address {
        // Add the Points that are directly in this model.
        var nextFieldAddress = addGroupPoints(group, startFieldAddress, block, modelId, modelLength, prefix, addComments)

        // Any repeating subgroups?
        if (group.groups.isEmpty()) {
            return nextFieldAddress
        }

        var subGroupIndex = 0
        for (subGroup in group.groups) {
            // The "count" of a subgroup can be a number OR the NAME of the field which contains the number.
            val count = determineCount(block, subGroup.count)

            val prefix = "${prefix}${subGroup.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}"

            for (index in 0 until count) {
                nextFieldAddress = addGroup(
                    block,
                    if (subGroup.count == null) { "${prefix}_" } else { "${prefix}_${index}_" },
                    subGroup,
                    nextFieldAddress,
                    modelId,
                    null,
                    false,
                )
            }
            subGroupIndex++
        }
        return nextFieldAddress
    }

    private fun createBlockForNonExistentModel(schemaDevice: SchemaDevice, modelId: Int, modelLength: Int, modelAddress: Address) {
        val block =
            Block
                .builder()
                .schemaDevice(schemaDevice)
                .id("Model $modelId")
                .description("[Model $modelId]: Unknown (vendor specific?) model. No fields available.")
                .build()
        val (modelIdField, _) = addModelHeaderFields(block, modelAddress)

        getFirstRegisterValue(modelIdField)?.let {
            it.comment =
                "--------------------------------------\n" +
                    "Model $modelId [Header @ ${it.address.toCleanFormat()}]: Unknown (vendor specific?) model. No fields available."
        }


        var unknownAddress = modelAddress.increment(SUNSPEC_MODEL_ID_REGISTERS + SUNSPEC_MODEL_L_REGISTERS)
        block.schemaDevice.getRegisterBlock(unknownAddress.addressClass)[unknownAddress].let {
            it.comment = "Model $modelId [Data @ ${it.address.toCleanFormat()} - ${
                it.address.increment(modelLength-1).toCleanFormat()
            }]: $modelLength registers"
        }

        var remainingRegisters = modelLength
        var index = 0
        do {
            val registersForField =
                if (remainingRegisters > MODBUS_MAX_REGISTERS_PER_REQUEST) {
                    MODBUS_MAX_REGISTERS_PER_REQUEST
                } else {
                    remainingRegisters
                }
            Field
                .builder()
                .block(block)
                .id("Unknown_$index")
                .description("Unknown block of registers #$index")
                .expression("hexstring(${unknownAddress.toCleanFormat()}#$registersForField)")
                .build()
            index++
            unknownAddress = unknownAddress.increment(registersForField)
            remainingRegisters -= registersForField
        } while (remainingRegisters > MODBUS_MAX_REGISTERS_PER_REQUEST)
    }

    // ----------------------------------------------------------------------------------------------------
    private fun mappingString(point: Point): String {
        val mappingString = StringBuilder()
        for (symbol in point.symbols) {
            mappingString.append(" ; ${symbol.value}->'${symbol.cleanName}'")
        }
        return mappingString.toString()
    }

    private fun createAndAddFieldToModel(block: Block, registerAddress: Address, point: Point, prefix: String): Field {
        val functionName: String
        val additionalArguments: String

        val typeLoadingParameters: TypeMapping
        when (point.type) {
            PAD,
            SUNSSF,
            INT_16,
            INT_32,
            INT_64,
            RAW_16,
            UINT_16,
            UINT_32,
            UINT_64,
            COUNT,
            ACC_16,
            ACC_32,
            ACC_64,
            FLOAT_32,
            FLOAT_64,
            STRING,
            IPADDR,
            IPV_6_ADDR,
            EUI_48,
            TIMESTAMP,
            -> {
                typeLoadingParameters = TYPE_MAPPINGS_NO_SYMBOLS[point.type]!!
                functionName = typeLoadingParameters.functionName
                additionalArguments = typeLoadingParameters.notImplemented
            }

            BITFIELD_16,
            BITFIELD_32,
            BITFIELD_64,
            ENUM_16,
            ENUM_32,
            -> {
                if (point.symbols.isEmpty()) {
                    typeLoadingParameters = TYPE_MAPPINGS_NO_SYMBOLS[point.type]!!
                    functionName = typeLoadingParameters.functionName
                    additionalArguments = typeLoadingParameters.notImplemented
                } else {
                    typeLoadingParameters = TYPE_MAPPINGS_WITH_SYMBOLS[point.type]!!
                    functionName = typeLoadingParameters.functionName
                    additionalArguments = typeLoadingParameters.notImplemented + mappingString(point)
                }
            }
        }

        val fieldBuilder =
            Field
                .builder()
                .block(block)
                .id(prefix + point.name)
                .unit(point.units)
                .immutable(point.mutable == Point.Mutable.IMMUTABLE)

        if (point.type == SUNSSF) {
//            fieldBuilder.description("[${block.id}](${point.name}): Scaling factor")
            fieldBuilder.description("Scaling factor")
        } else {
            val label = point.description ?: ""
//                if (point.label.isNullOrEmpty()) {
//                    "[${block.id}](${point.name}): ${point.description ?: ""}"
//                } else {
//                    "[${block.id}](${point.label}): ${point.description ?: ""}"
//                }
            fieldBuilder.description(label)
        }

        // Some fields are considered to be system fields (i.e. not directly usable applications fields)
        if (point.type == SUNSSF ||
            point.type == PAD ||
            point.isModelHeader
        ) {
            fieldBuilder.system(true).immutable(true)
        }

        var expression =
            functionName + "(" + registerAddress + (if (point.size == 1) "" else "#${point.size}" ) + additionalArguments + ")"

        if (!point.scalingFactor.isNullOrEmpty()) {
            expression += " * (10^${point.scalingFactor})"
        }

        if (point.type == TIMESTAMP) {
            val y2kEpochOffset = Instant.parse("2000-01-01T00:00:00.000Z").toEpochMilli()
            expression = "($expression*1000+$y2kEpochOffset)"
        }

        return fieldBuilder
            .expression(expression)
            .build()
    }

    data class TypeMapping(
        val functionName: String,
        val notImplemented: String = "",
    )

    // Maps Point.Type to  LoadFunction, notImplemented when NO symbols are present
    private val TYPE_MAPPINGS_NO_SYMBOLS: MutableMap<Point.Type, TypeMapping> = TreeMap()

    // Maps Point.Type to  LoadFunction, notImplemented when there ARE symbols present
    private val TYPE_MAPPINGS_WITH_SYMBOLS: MutableMap<Point.Type, TypeMapping> = TreeMap()

    init {
        // PAD is for applications a useless value because it is always 0x8000
        TYPE_MAPPINGS_NO_SYMBOLS[PAD] =           TypeMapping("int16",      ";0x8000"                                                     )
        TYPE_MAPPINGS_NO_SYMBOLS[SUNSSF] =        TypeMapping("int16",      ";0x8000"                                                     )
        TYPE_MAPPINGS_NO_SYMBOLS[INT_16] =        TypeMapping("int16",      ";0x8000"                                                     )
        TYPE_MAPPINGS_NO_SYMBOLS[INT_32] =        TypeMapping("int32",      ";0x8000 0x0000"                                              )
        TYPE_MAPPINGS_NO_SYMBOLS[INT_64] =        TypeMapping("int64",      ";0x8000 0x0000 0x0000 0x0000"                                )
        TYPE_MAPPINGS_NO_SYMBOLS[RAW_16] =        TypeMapping("hexstring"                                                                 )
        TYPE_MAPPINGS_NO_SYMBOLS[UINT_16] =       TypeMapping("uint16",     "; 0xFFFF ;0x8000"                                            ) // NOTE: The 0x8000... is NOT in the SPEC
        TYPE_MAPPINGS_NO_SYMBOLS[UINT_32] =       TypeMapping("uint32",     "; 0xFFFF 0xFFFF ;0x8000 0x0000"                              ) // NOTE: The 0x8000... is NOT in the SPEC
        TYPE_MAPPINGS_NO_SYMBOLS[UINT_64] =       TypeMapping("uint64",     "; 0xFFFF 0xFFFF 0xFFFF 0xFFFF ;0x8000 0x0000 0x0000 0x0000"  ) // NOTE: The 0x8000... is NOT in the SPEC
        TYPE_MAPPINGS_NO_SYMBOLS[TIMESTAMP] =     TypeMapping("uint32",     "; 0xFFFF 0xFFFF ;0x8000 0x0000"                              ) // NOTE: "Timestamp" was extra introduced by Niels Basjes
        TYPE_MAPPINGS_NO_SYMBOLS[COUNT] =         TypeMapping("uint16",     "; 0x0000"                                                    )
        TYPE_MAPPINGS_NO_SYMBOLS[ACC_16] =        TypeMapping("uint16",     "; 0x0000"                                                    )
        TYPE_MAPPINGS_NO_SYMBOLS[ACC_32] =        TypeMapping("uint32",     "; 0x0000 0x0000"                                             )
        TYPE_MAPPINGS_NO_SYMBOLS[ACC_64] =        TypeMapping("uint64",     "; 0x0000 0x0000 0x0000 0x0000"                               )
        TYPE_MAPPINGS_NO_SYMBOLS[FLOAT_32] =      TypeMapping("ieee754_32", "; 0x7FC0 0x0000"               /* IEEE 754 bits for NaN */   )
        TYPE_MAPPINGS_NO_SYMBOLS[FLOAT_64] =      TypeMapping("ieee754_64", "; 0x7FF8 0x0000 0x0000 0x0000" /* IEEE 754 bits for NaN */   ) // TODO: NOT USED IN 2024 SUNSPEC IN ANY REAL MODEL SO NOT TESTED
        TYPE_MAPPINGS_NO_SYMBOLS[STRING] =        TypeMapping("utf8"                                                                      )
        TYPE_MAPPINGS_NO_SYMBOLS[IPADDR] =        TypeMapping("ipv4addr",   "; 0x0000 0x0000"                                             ) // TODO: NOT USED IN 2024 SUNSPEC IN ANY REAL MODEL SO NOT TESTED
        TYPE_MAPPINGS_NO_SYMBOLS[IPV_6_ADDR] =    TypeMapping("ipv6addr",   "; 0x0000 0x0000 0x0000 0x0000"                               ) // TODO: NOT USED IN 2024 SUNSPEC IN ANY REAL MODEL SO NOT TESTED
        TYPE_MAPPINGS_NO_SYMBOLS[EUI_48] =        TypeMapping("eui48",      "; 0x0000 0x0000 0x0000 0x0000"                               )

        // These would normally have symbols but do not always have any because some are vendor specific
        TYPE_MAPPINGS_NO_SYMBOLS[ENUM_16] =       TypeMapping("uint16",     "; 0xFFFF"                                                    )
        TYPE_MAPPINGS_NO_SYMBOLS[ENUM_32] =       TypeMapping("uint32",     "; 0xFFFF 0xFFFF"                                             )
        TYPE_MAPPINGS_NO_SYMBOLS[BITFIELD_16] =   TypeMapping("bitset",     "; 0xFFFF"                                                    )
        TYPE_MAPPINGS_NO_SYMBOLS[BITFIELD_32] =   TypeMapping("bitset",     "; 0xFFFF 0xFFFF"                                             )
        TYPE_MAPPINGS_NO_SYMBOLS[BITFIELD_64] =   TypeMapping("bitset",     "; 0xFFFF 0xFFFF 0xFFFF 0xFFFF"                               ) // TODO: NOT USED IN 2024 SUNSPEC IN ANY REAL MODEL SO NOT TESTED

        TYPE_MAPPINGS_WITH_SYMBOLS[ENUM_16] =     TypeMapping("enum",       "; 0xFFFF"                                                    )
        TYPE_MAPPINGS_WITH_SYMBOLS[ENUM_32] =     TypeMapping("enum",       "; 0xFFFF 0xFFFF"                                             ) // TODO: NOT USED IN 2024 SUNSPEC IN ANY REAL MODEL SO NOT TESTED
        TYPE_MAPPINGS_WITH_SYMBOLS[BITFIELD_16] = TypeMapping("bitset",     "; 0xFFFF"                                                    )
        TYPE_MAPPINGS_WITH_SYMBOLS[BITFIELD_32] = TypeMapping("bitset",     "; 0xFFFF 0xFFFF ; 0x8000 0xFFFF"                             ) // NOTE: The 0x8000 0xFFFF is NOT in the SPEC
        TYPE_MAPPINGS_WITH_SYMBOLS[BITFIELD_64] = TypeMapping("bitset",     "; 0xFFFF 0xFFFF 0xFFFF 0xFFFF"                               ) // TODO: NOT USED IN 2024 SUNSPEC IN ANY REAL MODEL SO NOT TESTED
    }
}
