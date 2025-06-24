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

object FixesAndImprovements {
    /**
     * Various fixes to make the SunSpec usable in code.
     *
     * @param model The SunSpecModel that needs to be fixed.
     */
    fun fixProblemsInStandardSunSpec(model: SunSpecModel) {
        fixProblemsInStandardSunSpec(model.group)
        fixUnclearLabelsInStandardSunSpec(model)
        markTimestampFields(model)
    }

    private fun fixProblemsInStandardSunSpec(group: Group) {
        for (subGroup in group.groups) {
            fixProblemsInStandardSunSpec(subGroup)
        }

        for (point in group.points) {

            val label =  point.label
            if (label != null) {
                // Typo as reported here https://github.com/sunspec/models/issues/258
                point.label = label.replace("Sting", "String")
            }

            var description = point.description
            if (description != null) {
                description = description
                    .replace(Regex("^Bit Mask indicating (.*)$"), "$1 (Bitmask)")
                    .replace("Bit flags.", "")
                    .replace("Bitfield value.", "")
                    .replace("Bitmask value.", "")
                    .replace("Bitmask values.", "")
                    .replace("Bitmask value", "")
                    .replace("Bitmask values", "")
                    .replace("Enumerated value indicates if curve is ", "Curve is ")
                    .replace("Enumerated value.", "")
                    .replace("Enumerated valued.", "")
                    .replace("Enumerated value", "")
                    .replaceFirstChar { it.uppercase() }

                    .trim { it <= ' ' }
                description = "$description..."
                description = description
                    .replace("\\.+$".toRegex(), ".")
                    .replace(" +".toRegex(), " ")
                point.description = description
            }

            // Rewrite some of the provided units.
            point.units = when (point.units){
                "C"   -> "°C"
//                "Pct" -> "%"
                else -> point.units
            }
        }
    }

    private fun fixUnclearLabelsInStandardSunSpec(model: SunSpecModel) {
        model.group.fixUnclearGroupNames()
        val mapping = pointNameToLabel[model.id] ?: return
        model.group.fixUnclearLabelsInStandardSunSpec(mapping)
    }

    private fun Group.fixUnclearLabelsInStandardSunSpec(mapping: Map<String, String>) {
        name = groupNameToName[name]?: name
        points.forEach { point ->
            point.label = mapping[point.name] ?: return@forEach
        }
        groups.forEach { it.fixUnclearLabelsInStandardSunSpec(mapping) }
    }

    private fun Group.fixUnclearGroupNames() {
        name = groupNameToName[name]?: name
        groups.forEach { it.fixUnclearGroupNames() }
    }

    val groupNameToName = mapOf(
        "Ctl"   to   "Control",
        "Crv"   to   "Curve",
        "Pt"    to   "Point",
        "Prt"   to   "Port",
    )

    private fun markPointAsTimestamp(point: Point) {
        // Letting it fail hard here to ensure it is always valid
        require(point.type == Point.Type.UINT_32) { "Any type that should be a Timestamp MUST be in the schema as a uint32" }
        point.type = Point.Type.TIMESTAMP
    }

    private fun markTimestampFields(model: SunSpecModel) {
        when (model.id) {
            3,4,5,6,7,9,220
                -> model.group.points
                    .find { it.name == "Ts" }
                    ?.let { markPointAsTimestamp(it) }
            122,501,502
                -> model.group.points
                    .find { it.name == "Tms" }
                    ?.let { markPointAsTimestamp(it) }
            133 -> model.group.groups
                    .flatMap { group -> group.points }
                    .find { it.name == "StrTms" }
                    ?.let { markPointAsTimestamp(it) }
        }
    }

    val digitalSignature = arrayOf(
        "N"     to  "Number of digital signature registers",
        "DS"    to  "Digital Signature",
    )

    val certificate = arrayOf(
        "N"     to  "Number of certificate registers",
        "Cert"  to  "Certificate",
    )

    val powerRelatedPoints = arrayOf(
        "A"                 to "AC Current",
        "AphA"              to "AC Current Phase A",
        "AphB"              to "AC Current Phase B",
        "AphC"              to "AC Current Phase C",
        "PPV"               to "AC Voltage LL",
        "PPVphAB"           to "AC Voltage Phase AB",
        "PPVphBC"           to "AC Voltage Phase BC",
        "PPVphCA"           to "AC Voltage Phase CA",
        "PhV"               to "AC Voltage",
        "PhVphA"            to "AC Voltage Phase AN",
        "PhVphB"            to "AC Voltage Phase BN",
        "PhVphC"            to "AC Voltage Phase CN",
        "W"                 to "AC Power",
        "WphA"              to "AC Power Phase A",
        "WphB"              to "AC Power Phase B",
        "WphC"              to "AC Power Phase C",
        "Hz"                to "AC Line Frequency",
        "VA"                to "AC Apparent Power",
        "VAphA"             to "AC Apparent Power Phase A",
        "VAphB"             to "AC Apparent Power Phase B",
        "VAphC"             to "AC Apparent Power Phase C",
        "VAr"               to "AC Reactive Power",
        "VArphA"            to "AC Reactive Power Phase A",
        "VArphB"            to "AC Reactive Power Phase B",
        "VArphC"            to "AC Reactive Power Phase C",
        "VAR"               to "AC Reactive Power",
        "VARphA"            to "AC Reactive Power Phase A",
        "VARphB"            to "AC Reactive Power Phase B",
        "VARphC"            to "AC Reactive Power Phase C",
        "PF"                to "AC Power Factor",
        "PFphA"             to "AC Power Factor Phase A",
        "PFphB"             to "AC Power Factor Phase B",
        "PFphC"             to "AC Power Factor Phase C",
        "Wh"                to "AC Energy",
        "WH"                to "AC Energy",

        "DCA"               to "DC Current",
        "DCV"               to "DC Voltage",
        "DCW"               to "DC Power",

        "TotWhExp"          to "Total Real Energy Exported",
        "TotWhExpPhA"       to "Total Real Energy Exported Phase A",
        "TotWhExpPhB"       to "Total Real Energy Exported Phase B",
        "TotWhExpPhC"       to "Total Real Energy Exported Phase C",
        "TotWhImp"          to "Total Real Energy Imported",
        "TotWhImpPhA"       to "Total Real Energy Imported Phase A",
        "TotWhImpPhB"       to "Total Real Energy Imported Phase B",
        "TotWhImpPhC"       to "Total Real Energy Imported Phase C",
        "TotVAhExp"         to "Total Apparent Energy Exported",
        "TotVAhExpPhA"      to "Total Apparent Energy Exported Phase A",
        "TotVAhExpPhB"      to "Total Apparent Energy Exported Phase B",
        "TotVAhExpPhC"      to "Total Apparent Energy Exported Phase C",
        "TotVAhImp"         to "Total Apparent Energy Imported",
        "TotVAhImpPhA"      to "Total Apparent Energy Imported Phase A",
        "TotVAhImpPhB"      to "Total Apparent Energy Imported Phase B",
        "TotVAhImpPhC"      to "Total Apparent Energy Imported Phase C",
        "TotVArhImpQ1"      to "Total Reactive Energy Imported Q1",
        "TotVArhImpQ1PhA"   to "Total Reactive Energy Imported Q1 Phase A",
        "TotVArhImpQ1PhB"   to "Total Reactive Energy Imported Q1 Phase B",
        "TotVArhImpQ1PhC"   to "Total Reactive Energy Imported Q1 Phase C",
        "TotVArhImpQ2"      to "Total Reactive Energy Imported Q2",
        "TotVArhImpQ2PhA"   to "Total Reactive Energy Imported Q2 Phase A",
        "TotVArhImpQ2PhB"   to "Total Reactive Energy Imported Q2 Phase B",
        "TotVArhImpQ2PhC"   to "Total Reactive Energy Imported Q2 Phase C",
        "TotVArhExpQ3"      to "Total Reactive Energy Exported Q3",
        "TotVArhExpQ3PhA"   to "Total Reactive Energy Exported Q3 Phase A",
        "TotVArhExpQ3PhB"   to "Total Reactive Energy Exported Q3 Phase B",
        "TotVArhExpQ3PhC"   to "Total Reactive Energy Exported Q3 Phase C",
        "TotVArhExpQ4"      to "Total Reactive Energy Exported Q4",
        "TotVArhExpQ4PhA"   to "Total Reactive Energy Exported Q4 Phase A",
        "TotVArhExpQ4PhB"   to "Total Reactive Energy Exported Q4 Phase B",
        "TotVArhExpQ4PhC"   to "Total Reactive Energy Exported Q4 Phase C",

        "Evt"               to "Events",
        "Evt1"              to "Event Bitfield 1",
        "Evt2"              to "Event Bitfield 2",
    )

    // Map model.id -> point.name --> better point label
    val pointNameToLabel: Map<Int, Map<String, String>> = mapOf(
        2 to mapOf(
            "AID"   to "Aggregated model id",
            "N"     to "Number of aggregated models",
            "UN"    to "Update Number",
        ),

        3 to mapOf(
            "X"     to  "Number of requested registers",
            *digitalSignature,
            *(( 1..50 ).map { "Off${it}" to "Offset $it" }.toTypedArray()),
        ),

        4 to mapOf(
            "X"     to  "Number of requested registers",
            *digitalSignature,
            *(( 1..50 ).map { "Val${it}" to "Value at Offset $it" }.toTypedArray()),
        ),

        5 to mapOf(
            "X"     to  "Number of pairs being written",
            *digitalSignature,
            *(( 1..50 ).map { "Off${it}" to "Offset $it" }.toTypedArray()),
            *(( 1..50 ).map { "Val${it}" to "Value at Offset $it" }.toTypedArray()),
        ),

        6 to mapOf(
            "X"     to  "Number of pairs being written",
            *digitalSignature,
            "Off"   to  "Starting Offset",
            *(( 1..80 ).map { "Val${it}" to "Value at Offset $it" }.toTypedArray()),
            "Rsrvd" to "Reserved",
        ),

        7 to mapOf(
            *digitalSignature,
            "Rsrvd" to "Reserved",
        ),

        8 to mapOf(
            *certificate,
        ),

        9 to mapOf(
            *(( 1..80 ).map { "Frg${it}" to "Fragment word $it" }.toTypedArray()),
            *certificate,
        ),

        101 to mapOf(
            *powerRelatedPoints,
        ),

        102 to mapOf(
            *powerRelatedPoints,
        ),

        103 to mapOf(
            *powerRelatedPoints,
        ),

        111 to mapOf(
            *powerRelatedPoints,
        ),

        112 to mapOf(
            *powerRelatedPoints,
        ),

        113 to mapOf(
            *powerRelatedPoints,
        ),

        120 to mapOf(
            "DERTyp"        to "DERDeviceType",
            "WRtg"          to "Continuous power output capability",
            "VARtg"         to "Continuous Volt-Ampere capability",
            "VArRtgQ1"      to "Continuous VAR capability in quadrant 1",
            "VArRtgQ2"      to "Continuous VAR capability in quadrant 2",
            "VArRtgQ3"      to "Continuous VAR capability in quadrant 3",
            "VArRtgQ4"      to "Continuous VAR capability in quadrant 4",
            "ARtg"          to "Maximum RMS AC current level capability",
            "PFRtgQ1"       to "Minimum power factor capability in quadrant 1",
            "PFRtgQ2"       to "Minimum power factor capability in quadrant 2",
            "PFRtgQ3"       to "Minimum power factor capability in quadrant 3",
            "PFRtgQ4"       to "Minimum power factor capability in quadrant 4",
            "WHRtg"         to "Nominal energy rating",
            "AhrRtg"        to "Usable battery capacity",
            "MaxChaRte"     to "Maximum charging energy transfer rate",
            "MaxDisChaRte"  to "Maximum discharging energy transfer rate",
        ),

        121 to mapOf(
            "WMax"       to "Maximum power output",
            "VRef"       to "Voltage at the PCC",
            "VRefOfs"    to "Offset from PCC to inverter",
            "VMax"       to "Maximum voltage",
            "VMin"       to "Minimum voltage",
            "VAMax"      to "Maximum apparent power",
            "VArMaxQ1"   to "Maximum reactive power in quadrant 1",
            "VArMaxQ2"   to "Maximum reactive power in quadrant 2",
            "VArMaxQ3"   to "Maximum reactive power in quadrant 3",
            "VArMaxQ4"   to "Maximum reactive power in quadrant 4",
            "WGra"       to "Default ramp rate",
            "PFMinQ1"    to "Minimum power factor value in quadrant 1",
            "PFMinQ2"    to "Minimum power factor value in quadrant 2",
            "PFMinQ3"    to "Minimum power factor value in quadrant 3",
            "PFMinQ4"    to "Minimum power factor value in quadrant 4",
            "VArAct"     to "VAR action",
            "ClcTotVA"   to "Calculation method total apparent power",
            "MaxRmpRte"  to "Maximum ramp rate",
            "ECPNomHz"   to "Nominal frequency at ECP",
            "ConnPh"     to "Connected phase",
        ),

        122 to mapOf(
            "PVConn"       to  "PV inverter connection status",
            "StorConn"     to  "Storage inverter connection status",
            "ECPConn"      to  "ECP connection status",
            "ActWh"        to  "AC lifetime active energy output",
            "ActVAh"       to  "AC lifetime apparent energy output",
            "ActVArhQ1"    to  "AC lifetime reactive energy output in quadrant 1",
            "ActVArhQ2"    to  "AC lifetime reactive energy output in quadrant 2",
            "ActVArhQ3"    to  "AC lifetime reactive energy output in quadrant 3", // FIXME: Correction reported as https://github.com/sunspec/models/issues/263
            "ActVArhQ4"    to  "AC lifetime reactive energy output in quadrant 4",
            "VArAval"      to  "Available reactive power",
            "WAval"        to  "Available power",
            "StSetLimMsk"  to  "Limit reached",
            "StActCtl"     to  "Active inverter controls",
            "TmSrc"        to  "Time Source",
            "Tms"          to  "Timestamp",
            "RtSt"         to  "Active ride-through status",
            "Ris"          to  "Isolation resistance",
        ),

        123 to mapOf(
            "Conn_WinTms"        to "Connection Time window",
            "Conn_RvrtTms"       to "Connection Timeout period",
            "Conn"               to "Connection control",
            "WMaxLimPct"         to "Power Output Limit Pct",
            "WMaxLimPct_WinTms"  to "Power Output Limit Pct Change Time window",
            "WMaxLimPct_RvrtTms" to "Power Output Limit Pct Change Timeout period",
            "WMaxLimPct_RmpTms"  to "Power Output Limit Pct Change Ramp Time",
            "WMaxLim_Ena"        to "Power Output Limit Pct Change Throttle",
            "OutPFSet"           to "Power Factor",
            "OutPFSet_WinTms"    to "Power Factor Change Time window",
            "OutPFSet_RvrtTms"   to "Power Factor Change Timeout period",
            "OutPFSet_RmpTms"    to "Power Factor Change Ramp Time",
            "OutPFSet_Ena"       to "Power Factor Change Throttle",
            "VArWMaxPct"         to "Reactive power in percent of WMax",
            "VArMaxPct"          to "Reactive power in percent of VArMax",
            "VArAvalPct"         to "Reactive power in percent of VArAval",
            "VArPct_WinTms"      to "VAR Limit Change Time window",
            "VArPct_RvrtTms"     to "VAR Limit Change Timeout period",
            "VArPct_RmpTms"      to "VAR Limit Change Ramp Time",
            "VArPct_Mod"         to "VAR percent limit mode",
            "VArPct_Ena"         to "VAR Percent limit control",
        ),

        124 to mapOf(
            "WChaMax"            to "Maximum charge",
            "WChaGra"            to "Maximum charging rate",
            "WDisChaGra"         to "Maximum discharge rate",
            "StorCtl_Mod"        to "Storage control mode",
            "VAChaMax"           to "Maximum charge VA",
            "MinRsvPct"          to "Minimum reserve percentage",
            "ChaState"           to "Charge Percentage",
            "StorAval"           to "Storage Available",
            "InBatV"             to "Internal battery voltage",
            "ChaSt"              to "Charge status",
            "OutWRte"            to "Discharge rate percentage",
            "InWRte"             to "Charging rate percentage",
            "InOutWRte_WinTms"   to "Charge Discharge Rate Change Time window",
            "InOutWRte_RvrtTms"  to "Charge Discharge Rate Change Timeout period",
            "InOutWRte_RmpTms"   to "Charge Discharge Rate Change Ramp Time",
            "ChaGriSet"          to "Charge Grid setting",
        ),

        125 to mapOf(
            "ModEna"     to  "Enabled",
            "SigType"    to  "Signal Type",
            "Sig"        to  "Signal",
            "WinTms"     to  "Pricing Change Time window",
            "RvtTms"     to  "Pricing Change Timeout period",
            "RmpTms"     to  "Pricing Change Ramp Time",
        ),

        202 to mapOf(
            *powerRelatedPoints,
        ),

        203 to mapOf(
            *powerRelatedPoints,
        ),

        204 to mapOf(
            *powerRelatedPoints,
        ),

        211 to mapOf(
            *powerRelatedPoints,
        ),

        212 to mapOf(
            *powerRelatedPoints,
        ),

        213 to mapOf(
            *powerRelatedPoints,
        ),

        214 to mapOf(
            *powerRelatedPoints,
        ),

        220 to mapOf(
            *powerRelatedPoints,
            *digitalSignature,
            "Rsrvd" to "Reserved",
        ),

        501 to mapOf(
            "Tmp"  to "Module Temperature"
        ),

        502 to mapOf(
            "Tmp"  to "Module Temperature"
        ),

        701 to mapOf(
            "WL1"            to "Active power L1",
            "VAL1"           to "Apparent power L1",
            "VarL1"          to "Reactive power L1",
            "PFL1"           to "Power factor L1",
            "AL1"            to "Current L1",
            "VL1L2"          to "Phase voltage L1-L2",
            "VL1"            to "Phase voltage L1-N",
            "TotWhInjL1"     to "Total active energy injected L1",
            "TotWhAbsL1"     to "Total active energy absorbed L1",
            "TotVarhInjL1"   to "Total reactive energy injected L1",
            "TotVarhAbsL1"   to "Total reactive energy absorbed L1",

            "WL2"            to "Active power L2",
            "VAL2"           to "Apparent power L2",
            "VarL2"          to "Reactive power L2",
            "PFL2"           to "Power factor L2",
            "AL2"            to "Current L2",
            "VL2L3"          to "Phase voltage L2-L3",
            "VL2"            to "Phase voltage L2-N",
            "TotWhInjL2"     to "Total active energy injected L2",
            "TotWhAbsL2"     to "Total active energy absorbed L2",
            "TotVarhInjL2"   to "Total reactive energy injected L2",
            "TotVarhAbsL2"   to "Total reactive energy absorbed L2",

            "WL3"            to "Active power L3",
            "VAL3"           to "Apparent power L3",
            "VarL3"          to "Reactive power L3",
            "PFL3"           to "Power factor L3",
            "AL3"            to "Current L3",
            "VL3L1"          to "Phase voltage L3-L1",
            "VL3"            to "Phase voltage L3-N",
            "TotWhInjL3"     to "Total active energy injected L3",
            "TotWhAbsL3"     to "Total active energy absorbed L3",
            "TotVarhInjL3"   to "Total reactive energy injected L3",
            "TotVarhAbsL3"   to "Total reactive energy absorbed L3",
        ),

        702 to mapOf(
            "IntIslandCatRtg" to "Intentional Island Categories Rating",
            "WUndExtRtgPF"    to "Specified Under-Excited Rating Power Factor",
            "WOvrExtRtgPF"    to "Specified Over-Excited Rating Power Factor",
        ),

    )

}
