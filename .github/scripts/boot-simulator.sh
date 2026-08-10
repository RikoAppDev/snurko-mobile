#!/usr/bin/env bash
set -euo pipefail

runtime=$(xcrun simctl list runtimes --json | python3 -c '
import json, sys

runtimes = [
    runtime for runtime in json.load(sys.stdin)["runtimes"]
    if runtime["isAvailable"] and "SimRuntime.iOS" in runtime["identifier"]
]
runtimes.sort(key=lambda runtime: [int(part) for part in runtime["version"].split(".")])
print(runtimes[-1]["identifier"])
')

device_type=$(xcrun simctl list devicetypes --json | python3 -c '
import json, sys

types = json.load(sys.stdin)["devicetypes"]
by_name = {device["name"]: device["identifier"] for device in types}
for name in ["iPhone 16", "iPhone 15"]:
    if name in by_name:
        print(by_name[name])
        break
else:
    print(next(d["identifier"] for d in types if d["name"].startswith("iPhone")))
')

echo "runtime: $runtime"
echo "device type: $device_type"

udid=$(xcrun simctl create snurko-ci "$device_type" "$runtime")
xcrun simctl boot "$udid"
xcrun simctl bootstatus "$udid" -b

xcrun simctl ui "$udid" appearance light
xcrun simctl status_bar "$udid" override \
    --time "09:41" \
    --batteryState charged \
    --batteryLevel 100 \
    --cellularMode active \
    --cellularBars 4 \
    --wifiMode active \
    --wifiBars 3

echo "udid=$udid" >> "$GITHUB_OUTPUT"
