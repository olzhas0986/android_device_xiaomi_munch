#!/bin/bash
#
# SPDX-FileCopyrightText: 2024 The LineageOS Project
# SPDX-License-Identifier: Apache-2.0
#

set -e

# Load sort-blobs-list.py and do some sanity checks
MY_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

# List of files to sort
PROPRIETARY_FILES_TXT=(
    "${MY_DIR}/proprietary-files.txt"
)

ANDROID_ROOT="${MY_DIR}/../../.."

HELPER="${ANDROID_ROOT}/tools/extract-utils/sort-blobs-list.py"
if [ ! -f "${HELPER}" ]; then
    echo "Unable to find helper script at ${HELPER}"
    exit 1
fi

# Call the helper to sort the list
# Add --dir-first to give priority to directories and subdirectories
"${HELPER}" --dir-first "${PROPRIETARY_FILES_TXT[@]}"
