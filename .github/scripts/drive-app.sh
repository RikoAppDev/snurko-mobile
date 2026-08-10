#!/usr/bin/env bash
set -euo pipefail

udid=$1

bundle_id=app.snurko
result=build/ios-ui.xcresult
attachments=build/ios-attachments
shots=build/screenshots

mkdir -p "$shots"
rm -rf "$result" "$attachments"

xcrun simctl uninstall "$udid" "$bundle_id" >/dev/null 2>&1 || true

set +e
xcodebuild test \
    -project iosApp/iosApp.xcodeproj \
    -scheme iosApp \
    -configuration Debug \
    -destination "id=$udid" \
    -derivedDataPath build/ios \
    -resultBundlePath "$result" \
    CODE_SIGNING_ALLOWED=NO
status=$?
set -e

# The screenshots are worth more when the drive fails than when it passes.
if ! xcrun xcresulttool export attachments --path "$result" --output-path "$attachments"; then
    echo "::warning::could not export the driven screenshots from $result"
    exit "$status"
fi

python3 - "$attachments" "$shots" <<'PY'
import json
import os
import shutil
import sys

source, destination = sys.argv[1], sys.argv[2]
manifest_path = os.path.join(source, "manifest.json")
manifest = json.load(open(manifest_path)) if os.path.isfile(manifest_path) else []
if isinstance(manifest, dict):
    manifest = [manifest]

names = {}
for entry in manifest:
    for attachment in entry.get("attachments", []):
        exported = attachment.get("exportedFileName")
        if exported:
            names[exported] = attachment.get("suggestedHumanReadableName") or exported

copied = 0
for name in sorted(os.listdir(source)):
    if not name.lower().endswith((".png", ".jpeg", ".jpg")):
        continue
    shutil.copyfile(os.path.join(source, name), os.path.join(destination, names.get(name, name)))
    copied += 1
print("driven screenshots kept:", copied)
PY

ls -l "$shots"
exit "$status"
