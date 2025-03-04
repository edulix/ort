#!/bin/sh
#
# Copyright (C) 2020 Bosch.IO GmbH
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

GIT_ROOT=$(git rev-parse --show-toplevel)
GIT_VERSION=$(git describe --abbrev=7 --always --tags --dirty)

echo "Setting ORT_VERSION to $GIT_VERSION."
docker build -f $GIT_ROOT/Dockerfile -t ort \
    --build-arg USER_ID=$(id -u ${USER}) \
    --build-arg GROUP_ID=$(id -g ${USER}) \
    --build-arg ORT_VERSION=$GIT_VERSION \
    $GIT_ROOT
