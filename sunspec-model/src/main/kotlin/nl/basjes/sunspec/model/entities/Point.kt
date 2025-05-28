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
package nl.basjes.sunspec.model.entities

import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Suppress("unused") // Some SunSpec defined properties are unused
@Serializable
class Point (
    @Required
    @SerialName("name")         val name: String,

    @Required
    @SerialName("type")         var type: Type,
    @SerialName("value")        val value: Int? = null,
    @SerialName("count")        val count: Int? = null,

    @Required
    @SerialName("size")         val size: Int,

    /** The NAME of the scaling factor point OR a [-10;10] integer OR null */
    @SerialName("sf")           val scalingFactor: String? = null,
    @SerialName("units")        var units: String? = null,
    @SerialName("access")       val access: Access = Access.READONLY,
    @SerialName("mandatory")    val mandatory: Mandatory = Mandatory.OPTIONAL,

    /** Renamed `static` to `mutable` because 'static' is a reserved keyword in many languages and is confusing. */
    @SerialName("static")       val mutable: Mutable = Mutable.MUTABLE,
    @SerialName("label")        var label: String? = null,
    @SerialName("desc")         var description: String? = null,
    @SerialName("detail")       val detail: String? = null,
    @SerialName("notes")        val notes: String? = null,
    @SerialName("comments")     val comments: List<String> = listOf(),
    @SerialName("symbols")      val symbols: List<Symbol> = listOf(),
    @SerialName("standards")    val standards: List<String> = listOf(),
) {

    fun isImmutable() = mutable == Mutable.IMMUTABLE

    fun isStatic() = isImmutable()

    /** The SunSpec model has a problem that it treats the 2 registers that define the content (ID and L)
     * as part of the structure instead of outside of it. This flag is set to true on these 2 points.*/
    @Transient
    var isModelHeader: Boolean = false
        internal set

    /*
     * The offset in the useful data part (i.e. excluding the modelId and size).
     */
    var offsetInGroup = -1
        internal set

    override fun toString(): String {
        return "Point(name='$name', type=$type, units=$units)"
    }

    @Serializable
    enum class Access(private val value: String) {
        @SerialName("R")          READONLY("ReadOnly"),
        @SerialName("RW")         READWRITE("ReadWrite");
        override fun toString() = value
    }

    @Serializable
    enum class Mandatory(private val value: String) {
        @SerialName("M")          MANDATORY("Mandatory"),
        @SerialName("O")          OPTIONAL("Optional");
        override fun toString() = value
    }

    @Serializable
    enum class Mutable(private val value: String) {
        @SerialName("D")          MUTABLE("Dynamic"),
        @SerialName("S")          IMMUTABLE("Static");
        override fun toString() = value
    }

    @Serializable
    enum class Type(
        private val value: String,
        /** The number of 16 bit modbus registers for this value or 0 if it must be explicitly specified in the model (i.e. it is a string).  */
        val registerCount: Int
    ) {
        @SerialName("int16")      INT_16      ("int16",      1),
        @SerialName("int32")      INT_32      ("int32",      2),
        @SerialName("int64")      INT_64      ("int64",      4),
        @SerialName("raw16")      RAW_16      ("raw16",      1),
        @SerialName("uint16")     UINT_16     ("uint16",     1),
        @SerialName("uint32")     UINT_32     ("uint32",     2),
        @SerialName("uint64")     UINT_64     ("uint64",     4),
        @SerialName("acc16")      ACC_16      ("acc16",      1),
        @SerialName("acc32")      ACC_32      ("acc32",      2),
        @SerialName("acc64")      ACC_64      ("acc64",      4),
        @SerialName("bitfield16") BITFIELD_16 ("bitfield16", 1),
        @SerialName("bitfield32") BITFIELD_32 ("bitfield32", 2),
        @SerialName("bitfield64") BITFIELD_64 ("bitfield64", 4),
        @SerialName("enum16")     ENUM_16     ("enum16",     1),
        @SerialName("enum32")     ENUM_32     ("enum32",     2),
        @SerialName("float32")    FLOAT_32    ("float32",    2),
        @SerialName("float64")    FLOAT_64    ("float64",    4),
        @SerialName("string")     STRING      ("string",     0), // Need explicitly the number of registers
        @SerialName("pad")        PAD         ("pad",        1),
        @SerialName("ipaddr")     IPADDR      ("ipaddr",     2),
        @SerialName("ipv6addr")   IPV_6_ADDR  ("ipv6addr",   8),
        @SerialName("eui48")      EUI_48      ("eui48",      4),
        @SerialName("sunssf")     SUNSSF      ("sunssf",     1),
        @SerialName("count")      COUNT       ("count",      1),

        // This IS NOT part of the official SunSpec.
        // This is my proposal to have a timestamp type.
        // https://github.com/sunspec/models/issues/259
        @SerialName("timestamp")  TIMESTAMP   ("timestamp",  2);

        override fun toString() = "$value(" + if (registerCount>0) { "$registerCount Registers" } else { "?? Registers" } + ")"
    }
}
