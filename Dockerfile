# syntax=docker/dockerfile:1.2

# Copyright (C) 2020 Bosch Software Innovations GmbH
# Copyright (C) 2021 Bosch.IO GmbH
# Copyright (C) 2021 Alliander N.V.
# Copyright (C) 2021 Helio Chissini de Castro <helio@kde.org>
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# SPDX-License-Identifier: Apache-2.0
# License-Filename: LICENSE

#------------------------------------------------------------------------
# build base for main
FROM adoptopenjdk/openjdk11:ubuntu-slim AS base

# Set shell for bash and default pipefail
SHELL ["/bin/bash", "-o", "pipefail", "-c"]

# Basic intro
RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get upgrade -y --no-install-recommends \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        ca-certificates \
        curl \
        gnupg

#------------------------------------------------------------------------
# External repositories for SBT
RUN echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | tee /etc/apt/sources.list.d/sbt.list
RUN echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | tee /etc/apt/sources.list.d/sbt_old.list
ENV KEYURL=https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823
RUN curl -ksS "$KEYURL" | gpg --dearmor | tee "/etc/apt/trusted.gpg.d/scala_ubuntu.gpg" > /dev/null
ARG SBT_VERSION=1.3.8

#------------------------------------------------------------------------
# External repositories for Dart
ENV REPOURL="https://storage.googleapis.com/download.dartlang.org/linux/debian/dart_stable.list"
RUN curl -ksS "$REPOURL" | tee "/etc/apt/sources.list.d/dart_stable.list" > /dev/null
ENV KEYURL="https://dl-ssl.google.com/linux/linux_signing_key.pub"
RUN curl -ksS "$KEYURL" | gpg --dearmor | tee "/etc/apt/trusted.gpg.d/dart_ubuntu.gpg" > /dev/null

#------------------------------------------------------------------------
# Add git ppa for latest git
RUN echo "deb http://ppa.launchpad.net/git-core/ppa/ubuntu focal main" > /etc/apt/sources.list.d/git-core-ubuntu-ppa-focal.list
ENV KEYURL=https://keyserver.ubuntu.com/pks/lookup?op=get&search=0xa1715d88e1df1f24
RUN curl -ksS "$KEYURL" | gpg --dearmor | tee "/etc/apt/trusted.gpg.d/git-core_ubuntu_ppa.gpg"  > /dev/null

#------------------------------------------------------------------------
# Minimal set of packages for main docker
RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get upgrade -y --no-install-recommends \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        # Install general tools required by this Dockerfile.
        bash \
        cargo \
        composer \
        cvs \
        python2 \
        dart \
        git \
        lib32stdc++6 \
        libffi7 \
        libgmp10 \
        libgomp1 \
        libxext6 \
        libxi6 \
        libxrender1 \
        libxtst6 \
        netbase \
        openssh-client \
        sbt="$SBT_VERSION" \
        subversion \
        unzip \
        xz-utils \
        zlib1g \
        zlib1g-dev \
        libfreetype6-dev \
        libjpeg8-dev \
    && rm -rf /var/lib/apt/lists/*

#------------------------------------------------------------------------
# Build components
FROM base AS build

#------------------------------------------------------------------------
# Ubuntu build toolchain
RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        build-essential \
        ca-certificates \
        dirmngr \
        dpkg-dev \
        git \
        libbluetooth-dev \
        libbz2-dev \
        libc6-dev \
        libexpat1-dev \
        libffi-dev \
        libgmp-dev \
        libgdbm-dev \
        liblzma-dev \
        libmpdec-dev \
        libncursesw5-dev \
        libreadline-dev \
        libsqlite3-dev \
        libssl-dev \
        make \
        netbase \
        openssl \
        tk-dev \
        tzdata \
        unzip \
        uuid-dev \
        xz-utils \
        zlib1g-dev \
    && rm -rf /var/lib/apt/lists/*

# Copy the necessary bash resource to have paths
# for different languages
COPY docker/bash_bootstrap /etc/ort/bash_bootstrap
RUN mkdir -p /etc/ort/bash_modules

#------------------------------------------------------------------------
# Python using pyenv
ARG CONAN_VERSION=1.38.0
ARG PYTHON_VERSION=3.8.11
ARG VIRTUALENV_VERSION=20.2.2
ARG PIP_VERSION=21.2.4

# Python pyenv
ENV PYENV_ROOT=/opt/python
RUN curl -L https://github.com/pyenv/pyenv-installer/raw/master/bin/pyenv-installer | bash
ENV PATH=/opt/python/bin:$PATH
RUN pyenv install "${PYTHON_VERSION}"
RUN pyenv global "${PYTHON_VERSION}"
ENV PATH=/opt/python/shims:$PATH
RUN pip install -U pip=="${PIP_VERSION}" \
    conan=="${CONAN_VERSION}" \
    Mercurial \
    virtualenv=="${VIRTUALENV_VERSION}" \   
    pipenv
COPY docker/python.sh /etc/ort/bash_modules

#------------------------------------------------------------------------
# Nodejs Using nvm
ARG NODEJS_VERSION="--lts"
ENV NVM_DIR=/opt/nodejs
RUN git clone --depth 1 https://github.com/nvm-sh/nvm.git /opt/nodejs
RUN . $NVM_DIR/nvm.sh && nvm install "${NODEJS_VERSION}"
COPY docker/nodejs.sh /etc/ort/bash_modules

#------------------------------------------------------------------------
# Golang
# Golang dep depends on some development packages to be installed, so need build
# in the build stage
ARG GO_DEP_VERSION=0.5.4
ARG GO_VERSION=1.17
ENV GOPATH=/opt/go
RUN curl -L https://golang.org/dl/go${GO_VERSION}.linux-amd64.tar.gz | tar -C /opt -xz
ENV PATH=/opt/go/bin:$PATH
RUN go version
RUN curl -ksS https://raw.githubusercontent.com/golang/dep/v$GO_DEP_VERSION/install.sh | bash
RUN echo "add_local_path /opt/go/bin:$PATH" > /etc/ort/bash_modules/go.sh

#------------------------------------------------------------------------
# Haskell
ARG HASKELL_STACK_VERSION=2.1.3
ENV DEST=/opt/haskell/bin/stack
RUN curl -ksS https://raw.githubusercontent.com/commercialhaskell/stack/v$HASKELL_STACK_VERSION/etc/scripts/get-stack.sh | bash -s -- -d /usr/bin

#------------------------------------------------------------------------
# Ruby using rbenv
ARG RUBY_VERSION=2.7.4
ENV RBENV_ROOT=/opt/rbenv
ENV PATH=${RBENV_ROOT}/bin:${RBENV_ROOT}/shims/:${RBENV_ROOT}/plugins/ruby-build/bin:$PATH
RUN git clone --depth 1 https://github.com/rbenv/rbenv.git ${RBENV_ROOT}
RUN git clone --depth 1 https://github.com/rbenv/ruby-build.git "$(rbenv root)"/plugins/ruby-build
WORKDIR ${RBENV_ROOT}
RUN src/configure \
    && make -C src
RUN rbenv install ${RUBY_VERSION} \
    && rbenv global ${RUBY_VERSION} \
    && gem install bundler cocoapods
COPY docker/ruby.sh /etc/ort/bash_modules

#------------------------------------------------------------------------
# Scancode from official releases
ARG SCANCODE_VERSION=30.1.0
ENV SCANCODE_URL "https://github.com/nexB/scancode-toolkit/releases/download/v${SCANCODE_VERSION}"
RUN pyver=$(python3 --version | sed -e "s/Python //" | tr -d '.' | cut -c1-2) \
    && echo "${SCANCODE_URL}/scancode-toolkit-${SCANCODE_VERSION}_py${pyver}-linux.tar.xz" \
    && mkdir -p /opt/scancode \
    && curl -ksSL ${SCANCODE_URL}/scancode-toolkit-${SCANCODE_VERSION}_py${pyver}-linux.tar.xz | tar -C /opt/scancode -xJ --strip-components=1 \
    && cd /opt/scancode \
    && PYTHON_EXE=python3 ./configure \
    # cleanup unneeded installed binaries
    && rm -rf /opt/scancode/thirdparty

#------------------------------------------------------------------------
# This can be set to a directory containing CRT-files for custom certificates that ORT and all build tools should know about.
ARG CRT_FILES=""
COPY "$CRT_FILES" /tmp/certificates/
ARG ORT_VERSION="DOCKER-SNAPSHOT"

#------------------------------------------------------------------------
# ORT
COPY . /usr/local/src/ort
WORKDIR /usr/local/src/ort

#------------------------------------------------------------------------
# Gradle ORT build.
RUN scripts/import_proxy_certs.sh \
    && scripts/set_gradle_proxy.sh \
    && GRADLE_USER_HOME=/tmp/.gradle/ \
    && sed -i -r 's,(^distributionUrl=)(.+)-all\.zip$,\1\2-bin.zip,' gradle/wrapper/gradle-wrapper.properties \
    && ./gradlew \
        --no-daemon \
        --stacktrace \
        -Pversion=$ORT_VERSION \
        -PscancodeVersion=${SCANCODE_VERSION} \
        :cli:distTar \
        :helper-cli:startScripts

#------------------------------------------------------------------------
# Main ORT docker
FROM base

#------------------------------------------------------------------------
# Python from build
ENV PYENV_ROOT=/opt/python
COPY --from=build /opt/python /opt/python

#------------------------------------------------------------------------
# Ruby from build
ENV RBENV_ROOT=/opt/rbenv
COPY --from=build /opt/rbenv /opt/rbenv

#------------------------------------------------------------------------
# nodejs from build
ENV NVM_DIR=/opt/nodejs
COPY --from=build /opt/nodejs /opt/nodejs

#------------------------------------------------------------------------
# Golang from build
COPY --from=build /opt/go /opt/go/

#------------------------------------------------------------------------
# Stack from build
COPY --from=build /usr/bin/stack /usr/bin/

#------------------------------------------------------------------------
# Google Repo tool
RUN curl -ksS https://storage.googleapis.com/git-repo-downloads/repo > /usr/bin/repo \
    && chmod a+x /usr/bin/repo

#------------------------------------------------------------------------
# Android SDK
ARG ANDROID_SDK_VERSION=6858069
ENV ANDROID_HOME=/opt/android-sdk
RUN set +o pipefail \
     && curl -Os https://dl.google.com/android/repository/commandlinetools-linux-${ANDROID_SDK_VERSION}_latest.zip \
     && unzip -q commandlinetools-linux-${ANDROID_SDK_VERSION}_latest.zip -d $ANDROID_HOME \
     && rm commandlinetools-linux-${ANDROID_SDK_VERSION}_latest.zip \
     && yes | $ANDROID_HOME/cmdline-tools/bin/sdkmanager $SDK_MANAGER_PROXY_OPTIONS \
        --sdk_root=$ANDROID_HOME "platform-tools" "cmdline-tools;latest"

#------------------------------------------------------------------------
# NPM based package managers
ARG BOWER_VERSION=1.8.8
ARG NPM_VERSION=7.20.6
ARG YARN_VERSION=1.22.4
RUN . $NVM_DIR/nvm.sh \
    && npm install --global npm@$NPM_VERSION bower@$BOWER_VERSION yarn@$YARN_VERSION

#------------------------------------------------------------------------
# Scancode
COPY --from=build /opt/scancode /opt/scancode
RUN ln -s /opt/scancode/bin/scancode /usr/bin/scancode \
    && ln -s /opt/scancode/bin/pip /usr/bin/scancode-pip \
    && scancode --reindex-licenses

#------------------------------------------------------------------------
# ORT
COPY --from=build /usr/local/src/ort/scripts/*.sh /opt/ort/bin/
COPY --from=build /usr/local/src/ort/helper-cli/build/scripts/orth /opt/ort/bin/
COPY --from=build /usr/local/src/ort/helper-cli/build/libs/helper-cli-*.jar /opt/ort/lib/
COPY --from=build /usr/local/src/ort/cli/build/distributions/ort-*.tar /opt/ort.tar
RUN tar xf /opt/ort.tar -C /opt/ort --strip-components 1 \
    && rm /opt/ort.tar

#------------------------------------------------------------------------
# Bash modules and wrapper
COPY --from=build /etc/ort /etc/ort
RUN chmod a+x /etc/ort
RUN chmod -R a+r /etc/ort

COPY docker/ort-wrapper.sh /usr/bin/ort
RUN chmod 755 /usr/bin/ort

#------------------------------------------------------------------------
# Add arg user and run as a regular user
ARG USER_ID=0
ARG GROUP_ID=0

RUN if [ ${USER_ID:-0} -ne 0 ] && [ ${GROUP_ID:-0} -ne 0 ]; then \
    groupadd -g ${GROUP_ID} ort \
    && useradd -l -u ${USER_ID} -g ort ort \
    && install -d -m 0755 -o ort -g ort /home/ort; \
    fi

USER ort
WORKDIR /home/ort

ENTRYPOINT ["/usr/bin/ort"]
