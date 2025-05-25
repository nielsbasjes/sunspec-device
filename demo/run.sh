#!/bin/bash
#
# Dutch Smart Meter Requirements (DSMR) Toolkit
# Copyright (C) 2019-2024 Niels Basjes
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
#docker rmi demo-sunspec-to-influxdb
#docker compose up
#docker compose down

./SunSpecToInfluxDB.main.kts -ip sunspec.iot.basjes.nl

# The kind of commandline to expect in a normal situation (all fake hostnames)
# ./SunSpecToInfluxDB.main.kts -ip sunspec.iot.basjes.nl -port 502 -databaseUrl http://influx.iot.basjes.nl:8086 -databaseName thermia -databaseToken InfluxDBAPITokenValue=== -databaseOrg basjes -databaseBucket solar

