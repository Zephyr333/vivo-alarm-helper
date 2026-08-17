$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$sourceRoot = Join-Path $projectRoot 'src\com\example\vivoalarmhelper'

$visualActivities = @(
    'MainActivity.java',
    'EditProfileActivity.java',
    'EditAlarmActivity.java',
    'RunBaseActivity.java',
    'RepeatActivity.java',
    'RingtoneActivity.java',
    'VibrationActivity.java',
    'ShiftActivity.java',
    'VoiceBroadcastActivity.java'
)

foreach ($file in $visualActivities) {
    $path = Join-Path $sourceRoot $file
    $content = Get-Content -Raw -LiteralPath $path
    if ($content -notmatch 'Ui\.setContentView\(this,') {
        throw "$file does not apply the shared system-bar insets"
    }
}

$userFacingSources = $visualActivities + @('AlarmAccessibilityService.java')
foreach ($file in $userFacingSources) {
    $path = Join-Path $sourceRoot $file
    $content = Get-Content -Raw -LiteralPath $path
    if ($content -match '"[^"\r\n]*\bt0\b') {
        throw "$file still exposes t0 in a user-facing string"
    }
    if ($content -match '保存编码|偏移分钟|总分钟') {
        throw "$file still exposes an implementation term"
    }
    if ($content -match '秒') {
        throw "$file exposes unsupported alarm seconds"
    }
}

$service = Get-Content -Raw -LiteralPath (
    Join-Path $sourceRoot 'AlarmAccessibilityService.java')
$requiredExtras = @(
    'hour', 'minutes', 'repeat', 'daysofweek', 'remindway', 'message',
    'snooze', 'talker', 'china_holiday', 'snooze_talker',
    'default_ringtone', 'broadcast_content', 'vibrate_mode',
    'delete_after_ring_switch', 'alarm_remind_later_enable',
    'alarm_remind_later_max_number', 'date_alarm'
)
foreach ($extra in $requiredExtras) {
    if ($service -notmatch ('putExtra\("' + [regex]::Escape($extra) + '"')) {
        throw "Verified vivo extra missing: $extra"
    }
}

$store = Get-Content -Raw -LiteralPath (Join-Path $sourceRoot 'ProfileStore.java')
foreach ($offset in @(480, 495, 500)) {
    if ($store -notmatch ('defaultRelative\(' + $offset + '\)')) {
        throw "Default offset missing: $offset"
    }
}

$alarmConfig = Get-Content -Raw -LiteralPath (
    Join-Path $sourceRoot 'AlarmConfig.java')
if ($alarmConfig -notmatch 'private boolean snoozeEnabled;' -or
        $alarmConfig -notmatch 'optBoolean\("snoozeEnabled", false\)') {
    throw 'New alarms must default to snooze disabled'
}

$main = Get-Content -Raw -LiteralPath (Join-Path $sourceRoot 'MainActivity.java')
$shortcut = Get-Content -Raw -LiteralPath (Join-Path $sourceRoot 'ShortcutHelper.java')
if ($main -notmatch 'createRunIntent' -or $shortcut -notmatch 'createRunIntent') {
    throw 'In-app execution and desktop shortcuts do not share the run intent'
}
if ($main -notmatch 'handleProfileDrop' -or $store -notmatch 'moveProfile') {
    throw 'Profile drag sorting support is missing'
}
if ($main -match '方案顺序已保存') {
    throw 'Profile sorting still shows a success notification'
}

$profile = Get-Content -Raw -LiteralPath (Join-Path $sourceRoot 'AlarmProfile.java')
$timing = Get-Content -Raw -LiteralPath (Join-Path $sourceRoot 'ProfileTiming.java')
if ($profile -notmatch 'TIMING_SEQUENCE' -or $timing -notmatch 'elapsedAfterRuntimeBaseMinutes') {
    throw 'Relative sequence timing support is missing'
}

$runBase = Get-Content -Raw -LiteralPath (Join-Path $sourceRoot 'RunBaseActivity.java')
$pending = Get-Content -Raw -LiteralPath (
    Join-Path $sourceRoot 'PendingExecutionStore.java')
if ($runBase -notmatch '从现在起' -or $runBase -notmatch '指定时间' -or
        $runBase -notmatch 'createRuntimeRunIntent') {
    throw 'Execution-time base selection is incomplete'
}
if ($pending -notmatch 'runtimeBaseTargetAt') {
    throw 'Execution-time base is not persisted through accessibility recovery'
}

Write-Output 'Source regression checks passed'
