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
import nl.basjes.modbus.device.api.ModbusDevice
import nl.basjes.modbus.device.api.RegisterValue
import nl.basjes.modbus.device.exception.ModbusException
import nl.basjes.modbus.schema.Block
import nl.basjes.modbus.schema.Field
import nl.basjes.modbus.schema.SchemaDevice
import nl.basjes.modbus.schema.utils.CodeGeneration.convertToCodeCompliantName
import nl.basjes.modbus.schema.utils.StringTable
import nl.basjes.sunspec.SUNSPEC_MODEL_ID_REGISTERS
import nl.basjes.sunspec.SUNSPEC_MODEL_L_REGISTERS
import nl.basjes.sunspec.SUNS_CHAIN_END_MODEL_ID
import nl.basjes.sunspec.SUNS_HEADER_MODEL_ID
import nl.basjes.sunspec.device.SunSpecDeviceModelFinder.findDeviceSunSpecModels
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
        // Ensure this new SchemaDevice is connected to the same ModbusDevice
        // DO NOT use the optimized version because some devices (i.e. mine does) will give
        // a modbus level error if you fetch a register for something it does not have.
        // Such a situation will make the entire block fail, so for starters we fetch them one by one.
        schemaDevice.connectBase(modbusDevice)

        for (deviceSunSpecModel in deviceSunSpecModels) {
            val modelId = deviceSunSpecModel.id
            val modelLength = deviceSunSpecModel.registers
            val modelAddress = deviceSunSpecModel.address

            // Special case is the header
            if (modelId == SUNS_HEADER_MODEL_ID) {
                val sunSHeaderBlock = Utils.addSunSHeaderBlock(schemaDevice, deviceSunSpecModel.address)
                setCommentOnFirstRegisterValue(sunSHeaderBlock, "SunS header")
                continue
            }

            // Special case is the end of the chain
            if (modelId == SUNS_CHAIN_END_MODEL_ID) {
                val endChainBlock = Utils.addSunSpecEndOfChainBlock(schemaDevice)
                val (endChainId, _) = Utils.addModelHeaderFields(endChainBlock, modelAddress)
                setCommentOnFirstRegisterValue(endChainId, "NO MORE MODELS")
                break // This should be the last in the chain
            }

            // Find the official SunSpec specification for this one.
            val sunSpecModel = sunSpec.getModel(modelId)

            if (sunSpecModel == null) {
                LOG.fatal("Unable to get model for ID: {}", modelId)
                continue
            }

            // Create and add a new Block for this SunSpec model.
            val modelDescription =
                if (sunSpecModel.group.description.isNullOrBlank()) {
                    "[Model " + sunSpecModel.id + "]:" + sunSpecModel.group.label
                } else {
                    "[Model " + sunSpecModel.id + "]:" + sunSpecModel.group.label + ": " + sunSpecModel.group.description
                }

            val block =
                Block
                    .builder()
                    .schemaDevice(schemaDevice)
                    .id("Model " + sunSpecModel.id)
                    .description(modelDescription)
                    .build()

            addGroup(block, "", sunSpecModel.group, modelAddress, modelId, modelLength)
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

    @Throws(ModbusException::class)
    private fun addGroup(
        block: Block,
        prefix: String,
        group: Group,
        thisModelAddress: Address,
        modelId: Int,
        modelLength: Int?,
    ) {
        // Add the Points that are directly in this model.

        var pointCounter = 0
        if (modelLength == null) {
            // I.e. a multi level deep nested group
            pointCounter = 3 // Always do the last in the switch below
        }
        for (point in group.points) {
            pointCounter++
            val field = createAndAddFieldToModel(block, thisModelAddress, point, prefix)

            when (pointCounter) {
                // 1: The ID
                1 -> {
                    getFirstRegisterValue(field)?.let {
                        it.comment =
                            "--------------------------------------\n" +
                            "Model $modelId [Header @ ${it.address.toCleanFormat()}]: ${group.label}"
                    }
                }

                // 2: The Length
                2 -> {}

                // 3: The first data register
                3 -> {
                    getFirstRegisterValue(field)?.let {
                        it.comment = "Model $modelId [Data @ ${it.address.toCleanFormat()} - ${
                            it.address.increment(modelLength!!).toCleanFormat()
                        }]: $modelLength registers"
                    }
                }

                else -> {}
            }
        }
        for (subGroup in group.groups) {
            // The "count" of a subgroup can be a number OR the NAME of the field which contains the number.
            val countString = subGroup.count
            var count = 0
            if (countString != null) {
                try {
                    count = countString.toInt()
                } catch (_: NumberFormatException) {
                    val countField =
                        block.getField(countString)
                            ?: throw ModbusException("Unable to find the count field named \"$countString\" for this group")
                    countField.update()
                    val countValue =
                        countField.longValue
                            ?: throw ModbusException("Unable to read the value of the count field named \"$countString\" for this group")
                    count = Math.toIntExact(countValue)
                }
            }

            // The older models have a fixed set of points and a repeating set.
            // The number of repeats must be calculated from the sizes of the complete model, the fixed part and repeating part.
            if (count == 0 && modelLength != null && group.groups.size == 1) {
                if (group.dataSize == 0 || subGroup.dataSize == 0) {
                    throw ModbusException("SunSpec repeat block problem")
                }

                count = (SUNSPEC_MODEL_ID_REGISTERS + SUNSPEC_MODEL_L_REGISTERS + modelLength - group.dataSize) / subGroup.dataSize

                // Check for rounding errors
                require(group.dataSize + (subGroup.dataSize * count) == modelLength) {
                    throw ModbusException(
                        "SunSpec repeat block problem: ${group.dataSize} + (${subGroup.dataSize} * $count) == $modelLength)",
                    )
                }
            }

            var subGroupPrefix = prefix
            if (prefix.isEmpty()) {
                subGroupPrefix = "R"
            }

            for (index in 0 until count) {
                addGroup(
                    block,
                    subGroupPrefix + index + "_",
                    subGroup,
                    thisModelAddress.increment(group.dataSize + (index * subGroup.dataSize)),
                    modelId,
                    null,
                )
            }
        }
    }

    // ----------------------------------------------------------------------------------------------------
    private fun mappingString(point: Point): String {
        val mappingString = StringBuilder()
        for (symbol in point.symbols) {
            mappingString.append(" ; ").append(symbol.value).append("->'").append(symbol.cleanName).append("'")
        }
        return mappingString.toString()
    }

    private fun Point.getCamelCaseName() = convertToCodeCompliantName(name, true)

    private fun Point.getCamelCaseSfName() =
        if (scalingFactor == null) {
            null
        } else {
            convertToCodeCompliantName(scalingFactor!!, true)
        }

    private fun createAndAddFieldToModel(block: Block, modelBaseAddress: Address, point: Point, prefix: String): Field {
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
                .id(prefix + point.getCamelCaseName())
                .unit(point.units)
                .immutable(point.mutable == Point.Mutable.IMMUTABLE)

        if (point.type == SUNSSF) {
            fieldBuilder.description("[${block.id}](${point.name}): Scaling factor")
        } else {
            val label =
                if (point.label.isNullOrEmpty()) {
                    "[${block.id}](${point.name}): ${point.description ?: ""}"
                } else {
                    "[${block.id}](${point.label}): ${point.description ?: ""}"
                }
            fieldBuilder.description(label)
        }

        // Some fields are considered to be system fields (i.e. not directly usable applications fields)
        if (point.type == SUNSSF ||
            point.type == PAD ||
            (point.isImmutable() && (point.name == "ID" || point.name == "L"))
        ) {
            fieldBuilder.system(true).immutable(true)
        }

        val registerAddress = modelBaseAddress.increment(point.offsetInGroup)

        var expression =
            functionName + "(" + registerAddress + (if (point.size == 1) "" else "#" + point.size) + additionalArguments + ")"

        val camelCaseSfName: String? = point.getCamelCaseSfName()
        if (!camelCaseSfName.isNullOrEmpty()) {
            expression += " * (10^$camelCaseSfName)"
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
        TYPE_MAPPINGS_NO_SYMBOLS[BITFIELD_16] =   TypeMapping("uint16",     "; 0xFFFF"                                                    )
        TYPE_MAPPINGS_NO_SYMBOLS[BITFIELD_32] =   TypeMapping("uint32",     "; 0xFFFF 0xFFFF"                                             )
        TYPE_MAPPINGS_NO_SYMBOLS[BITFIELD_64] =   TypeMapping("uint64",     "; 0xFFFF 0xFFFF 0xFFFF 0xFFFF"                               ) // TODO: NOT USED IN 2024 SUNSPEC IN ANY REAL MODEL SO NOT TESTED

        TYPE_MAPPINGS_WITH_SYMBOLS[ENUM_16] =     TypeMapping("enum",       "; 0xFFFF"                                                    )
        TYPE_MAPPINGS_WITH_SYMBOLS[ENUM_32] =     TypeMapping("enum",       "; 0xFFFF 0xFFFF"                                             ) // TODO: NOT USED IN 2024 SUNSPEC IN ANY REAL MODEL SO NOT TESTED
        TYPE_MAPPINGS_WITH_SYMBOLS[BITFIELD_16] = TypeMapping("bitset",     "; 0xFFFF"                                                    )
        TYPE_MAPPINGS_WITH_SYMBOLS[BITFIELD_32] = TypeMapping("bitset",     "; 0xFFFF 0xFFFF ; 0x8000 0xFFFF"                             ) // NOTE: The 0x8000 0xFFFF is NOT in the SPEC
        TYPE_MAPPINGS_WITH_SYMBOLS[BITFIELD_64] = TypeMapping("bitset",     "; 0xFFFF 0xFFFF 0xFFFF 0xFFFF"                               ) // TODO: NOT USED IN 2024 SUNSPEC IN ANY REAL MODEL SO NOT TESTED
    }
}
