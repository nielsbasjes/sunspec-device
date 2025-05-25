#
# Modbus Schema Toolkit
# Copyright (C) 2019-2025 Niels Basjes
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
FROM ubuntu:24.04

ENV INSIDE_DOCKER=Yes

ARG DEBIAN_FRONTEND=noninteractive

# The name of the Kotlin script to inject
ARG SCRIPT

# Suppress an apt-key warning about standard out not being a terminal. Use in this script is safe.
ENV APT_KEY_DONT_WARN_ON_DANGEROUS_USAGE=DontWarn


SHELL ["/bin/bash", "-o", "pipefail", "-c"]

#####
# Disable suggests/recommends
#####
RUN echo APT::Install-Recommends "0"\; > /etc/apt/apt.conf.d/10disableextras
RUN echo APT::Install-Suggests "0"\; >>  /etc/apt/apt.conf.d/10disableextras

ENV DEBIAN_FRONTEND="noninteractive"
ENV DEBCONF_TERSE="true"

####
## Update and install common packages
####

RUN apt clean cache \
   && apt -q update \
   && apt install -y software-properties-common apt-utils apt-transport-https ca-certificates wget unzip openjdk-21-jdk locales \
   && apt clean cache

# Installs Kotlin
ARG KOTLIN_VERSION=2.1.21
WORKDIR /usr/local/
RUN wget "https://github.com/JetBrains/kotlin/releases/download/v$KOTLIN_VERSION/kotlin-compiler-$KOTLIN_VERSION.zip" && \
    unzip "kotlin-compiler-$KOTLIN_VERSION.zip" && \
    rm "kotlin-compiler-$KOTLIN_VERSION.zip" && \
    chmod -R +x /usr/local/kotlinc/bin/ && \
    ln -s /usr/local/kotlinc/bin/kotlin /bin/kotlin && \
    ln -s /usr/local/kotlinc/bin/kotlinc /bin/kotlinc
ENV PATH=$PATH:/usr/local/kotlinc/bin/

###
# Set the locale ( see https://stackoverflow.com/a/28406007/114196 )
###
RUN sed -i '/en_US.UTF-8/s/^# //g' /etc/locale.gen && \
    locale-gen
ENV LANG="en_US.UTF-8"
ENV LANGUAGE="en_US:en"
ENV LC_ALL="en_US.UTF-8"

###
# Better reproducibility: deliberately set default timezone VERY uncommon (only ~200 people in this timezone)
###
RUN ln -fs /usr/share/zoneinfo/Australia/Eucla /etc/localtime && \
    dpkg-reconfigure -f noninteractive tzdata

RUN apt install -y maven

ADD ${SCRIPT} /scripts/entrypoint.main.kts
RUN chmod 755 /scripts/entrypoint.main.kts

# This will pull in all dependencies, precompile the script and make them part of the image.
RUN /scripts/entrypoint.main.kts

ENTRYPOINT [ "/scripts/entrypoint.main.kts" ]

#
