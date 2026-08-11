#!/usr/bin/env bash
set -euo pipefail

udid=$1
app=$2

bundle_id=app.snurko
database_identity_hash=6a6556c054fde2745aef64439f1260e1
shots=build/screenshots

mkdir -p "$shots"

install_fresh() {
    xcrun simctl uninstall "$udid" "$bundle_id" >/dev/null 2>&1 || true
    xcrun simctl install "$udid" "$app"
}

# Writes a notebook Room can open as-is: schema copied from the exported schema
# JSON, eleven finished days behind an unfinished day twelve.
seed_notebook() {
    local title=$1 first=$2 first_fail=$3 second=$4 second_fail=$5 third=$6
    local container database start_epoch_day

    container=$(xcrun simctl get_app_container "$udid" "$bundle_id" data)
    database="$container/Documents/snurko.db"
    start_epoch_day=$(( $(date -u +%s) / 86400 - 11 ))

    mkdir -p "$container/Documents"
    rm -f "$database" "$database-wal" "$database-shm"

    sqlite3 "$database" <<'SCHEMA'
PRAGMA user_version = 3;
CREATE TABLE IF NOT EXISTS `challenges` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `startDateEpochDay` INTEGER NOT NULL, `lengthDays` INTEGER NOT NULL);
CREATE TABLE IF NOT EXISTS `habits` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `challengeId` INTEGER NOT NULL, `name` TEXT NOT NULL, `failCondition` TEXT, `position` INTEGER NOT NULL, FOREIGN KEY(`challengeId`) REFERENCES `challenges`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE );
CREATE INDEX IF NOT EXISTS `index_habits_challengeId_position` ON `habits` (`challengeId`, `position`);
CREATE TABLE IF NOT EXISTS `attempts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `challengeId` INTEGER NOT NULL, `number` INTEGER NOT NULL, `startDateEpochDay` INTEGER NOT NULL, `status` TEXT NOT NULL, `failedOnDay` INTEGER, FOREIGN KEY(`challengeId`) REFERENCES `challenges`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE );
CREATE UNIQUE INDEX IF NOT EXISTS `index_attempts_challengeId_number` ON `attempts` (`challengeId`, `number`);
CREATE INDEX IF NOT EXISTS `index_attempts_challengeId_status` ON `attempts` (`challengeId`, `status`);
CREATE TABLE IF NOT EXISTS `day_checks` (`attemptId` INTEGER NOT NULL, `day` INTEGER NOT NULL, `habitId` INTEGER NOT NULL, PRIMARY KEY(`attemptId`, `day`, `habitId`), FOREIGN KEY(`attemptId`) REFERENCES `attempts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE );
CREATE INDEX IF NOT EXISTS `index_day_checks_habitId` ON `day_checks` (`habitId`);
CREATE TABLE IF NOT EXISTS `reminders` (`habitId` INTEGER NOT NULL, `minuteOfDay` INTEGER NOT NULL, PRIMARY KEY(`habitId`), FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE );
CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT);
SCHEMA

    {
        echo "INSERT INTO room_master_table (id, identity_hash) VALUES (42, '$database_identity_hash');"
        echo "INSERT INTO challenges (id, name, startDateEpochDay, lengthDays) VALUES (1, '$title', $start_epoch_day, 90);"
        echo "INSERT INTO habits (id, challengeId, name, failCondition, position) VALUES (1, 1, '$first', '$first_fail', 0);"
        echo "INSERT INTO habits (id, challengeId, name, failCondition, position) VALUES (2, 1, '$second', '$second_fail', 1);"
        echo "INSERT INTO habits (id, challengeId, name, failCondition, position) VALUES (3, 1, '$third', NULL, 2);"
        echo "INSERT INTO attempts (id, challengeId, number, startDateEpochDay, status, failedOnDay) VALUES (1, 1, 1, $start_epoch_day, 'ACTIVE', NULL);"
        for day in $(seq 1 11); do
            for habit in 1 2 3; do
                echo "INSERT INTO day_checks (attemptId, day, habitId) VALUES (1, $day, $habit);"
            done
        done
        echo "INSERT INTO day_checks (attemptId, day, habitId) VALUES (1, 12, 1);"
        echo "INSERT INTO reminders (habitId, minuteOfDay) VALUES (2, 1260);"
    } | sqlite3 "$database"
}

report_crash() {
    sleep 3
    echo "::group::Crash reports"
    find "$HOME/Library/Logs/DiagnosticReports" -name "Snurko*" -print -exec cat {} \; 2>/dev/null || true
    xcrun simctl spawn "$udid" log show --style compact --last 3m \
        --predicate 'senderImagePath CONTAINS "Snurko" OR processImagePath CONTAINS "Snurko"' \
        2>/dev/null | tail -80 || true
    echo "::endgroup::"
}

launch_and_shoot() {
    local name=$1
    shift
    local pid

    pid=$(xcrun simctl launch "$udid" "$bundle_id" "$@" | awk -F': ' '{ print $2 }')
    echo "launched $name as pid $pid"

    for _ in $(seq 1 12); do
        sleep 1
        if ! ps -p "$pid" >/dev/null 2>&1; then
            echo "::error::Snurko died while opening $name"
            report_crash
            exit 1
        fi
    done

    xcrun simctl io "$udid" screenshot "$shots/$name.png"
    xcrun simctl terminate "$udid" "$bundle_id" >/dev/null 2>&1 || true
}

install_fresh

launch_and_shoot 01-setup
launch_and_shoot 02-setup-uk -AppleLanguages "(uk)" -AppleLocale uk_UA

seed_notebook "My 90 days" "Workout" "less than 30 minutes" "Read" "fewer than 10 pages" "No alcohol"
launch_and_shoot 03-home

seed_notebook "Мої 90 днів" "Зарядка" "менше ніж 30 хвилин" "Читання" "менше ніж 10 сторінок" "Без алкоголю"
launch_and_shoot 04-home-uk -AppleLanguages "(uk)" -AppleLocale uk_UA

ls -l "$shots"
