#!/bin/bash

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

RBENV_ROOT="/opt/rbenv"
export RBENV_ROOT

add_local_path "${RBENV_ROOT}/bin"
add_local_path "${RBENV_ROOT}/shims"
add_local_path "${RBENV_ROOT}/plugins/ruby-install/bin"
