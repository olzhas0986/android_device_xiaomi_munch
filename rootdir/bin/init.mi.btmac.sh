#!/vendor/bin/sh
# Copyright (C) 2021 KudProject Development
# SPDX-License-Identifier: GPL-3.0-or-later OR Apache-2.0

LOG_TAG="MiSetBtMac"
logi () { log -t "$LOG_TAG" -p i "$@"; }

# hex binary containing mac address
BT_MAC_HEX_PATH="/data/vendor/mac_addr/bt.mac";
if [ ! -f "$BT_MAC_HEX_PATH" ]; then
    logi "bt.mac file not found, exiting"
    exit
fi

# The file must contain exactly one six-byte Bluetooth address.
RAW_MAC=$(xxd -p "$BT_MAC_HEX_PATH")
if [ "${#RAW_MAC}" -ne 12 ]; then
    logi "Invalid bt.mac length, leaving the current address unchanged"
    exit 1
fi
case "$RAW_MAC" in
    *[!0-9a-fA-F]* | 000000000000 | ffffffffffff | FFFFFFFFFFFF)
        logi "Invalid bt.mac address, leaving the current address unchanged"
        exit 1
        ;;
esac

DEC_MAC=$(printf '%s' "$RAW_MAC" | sed 's/../&:/g;s/:$//')
if setprop persist.vendor.service.bdroid.bdaddr "$DEC_MAC"; then
    logi "bt.mac file found, setting mac addr"
else
    logi "Failed to set Bluetooth address"
    exit 1
fi
