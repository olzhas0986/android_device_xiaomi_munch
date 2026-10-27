#!/usr/bin/env python3
#
# SPDX-FileCopyrightText: The LineageOS Project
# SPDX-License-Identifier: Apache-2.0
#

import runpy
import sys
from pathlib import Path

if __name__ == '__main__':
    sys.argv.insert(1, '--regenerate_makefiles')
    runpy.run_path(str(Path(__file__).resolve().with_name('extract-files.py')), run_name='__main__')
