#! /vendor/bin/sh

# Copyright (c) 2009-2016, The Linux Foundation. All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
#     * Redistributions of source code must retain the above copyright
#       notice, this list of conditions and the following disclaimer.
#     * Redistributions in binary form must reproduce the above copyright
#       notice, this list of conditions and the following disclaimer in the
#       documentation and/or other materials provided with the distribution.
#     * Neither the name of The Linux Foundation nor
#       the names of its contributors may be used to endorse or promote
#       products derived from this software without specific prior written
#       permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
# NON-INFRINGEMENT ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
# CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
# EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
# PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
# OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
# WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
# OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
# ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#

start_msm_irqbalance_kona()
{
         if [ -f /vendor/bin/msm_irqbalance ]; then
                start vendor.msm_irqbalance
         fi
}

echo 1 > /proc/sys/net/ipv6/conf/default/accept_ra_defrtr

start_msm_irqbalance_kona

#
# Make modem config folder and copy firmware config to that folder for RIL
#
firmware_config=/vendor/firmware_mnt/image/modem_pr/mcfg/configs
firmware_version=/vendor/firmware_mnt/verinfo/ver_info.txt
firmware_ota=/vendor/firmware_mnt/image/modem_pr/mbn_ota.txt
modem_config=/data/vendor/modem_config

# A missing firmware mount must not erase the last working modem configuration.
if [ -r "$firmware_version" ] && [ -d "$firmware_config" ] &&
        [ -r "$firmware_ota" ] && cur_version_info=$(cat "$firmware_version") &&
        [ -n "$cur_version_info" ]; then
    prev_version_info=$(cat "$modem_config/ver_info.txt" 2>/dev/null)
    if [ ! -f "$modem_config/ver_info.txt" ] || [ "$prev_version_info" != "$cur_version_info" ]; then
        if chmod g+w -R "$modem_config" &&
                rm -rf "$modem_config"/* &&
                cp --preserve=m -dr "$firmware_config"/. "$modem_config"/ &&
                cp --preserve=m -d "$firmware_ota" "$modem_config"/ &&
                chown -hR radio.root "$modem_config" &&
                cp --preserve=m -d "$firmware_version" "$modem_config"/ &&
                chown radio.root "$modem_config/ver_info.txt"; then
            setprop ro.vendor.ril.mbn_copy_completed 1
        else
            rm -f "$modem_config/ver_info.txt"
            log -t init.qcom -p e "Failed to copy modem configuration"
        fi
    else
        setprop ro.vendor.ril.mbn_copy_completed 1
    fi
    chmod g-w "$modem_config"
else
    log -t init.qcom -p e "Modem firmware configuration is unavailable"
fi

#check build variant for printk logging
#current default minimum boot-time-default
buildvariant=`getprop ro.build.type`
case "$buildvariant" in
    "userdebug" | "eng")
        #set default loglevel to KERN_INFO
        echo "4 6 1 7" > /proc/sys/kernel/printk
        ;;
    *)
        #set default loglevel to KERN_WARNING
        echo "4 4 1 4" > /proc/sys/kernel/printk
        ;;
esac
