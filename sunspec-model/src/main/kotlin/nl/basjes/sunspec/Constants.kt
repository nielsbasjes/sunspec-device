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
package nl.basjes.sunspec

/** The standard Modbus unit id where a SunSpec device is listening on. */
const val SUNSPEC_STANDARD_UNITID: Int = 126

/** The list of starting addresses where the SunSpec Model chain can begin. */
val SUNSPEC_STANDARD_START_PHYSICAL_ADDRESS = arrayOf(0, 40000, 50000)

/** The list of SunSpec models starts with 2 registers that hold this ASCII/UTF-8 value. */
const val SUNSPEC_HEADER: String = "SunS"

/**
 * If a fake model id is needed for the SunS header then use this value.
 * Note that this value IS sometimes incorrectly used by some devices as their CLOSING marker.
 */
const val SUNS_HEADER_MODEL_ID:       Int = 0

/**
 * The model id of the terminating model. This model MUST be 0 registers in size!
 */
const val SUNS_CHAIN_END_MODEL_ID:    Int = 0XFFFF

/**
 * The number of registers used for the model id of a model.
 */
const val SUNSPEC_MODEL_ID_REGISTERS: Int = 1

/**
 * The number of registers used for the model length of a model.
 * This value does NOT include the ID and L registers.
 */
const val SUNSPEC_MODEL_L_REGISTERS:  Int = 1
