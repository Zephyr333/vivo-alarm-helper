$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$sourceRoot = Join-Path $projectRoot 'src\com\example\vivoalarmhelper'

$visualActivities = @(
    'MainActivity.java',
    'EditProfileActivity.java',
    'EditAlarmActivity.java',
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

Write-Output 'Source regression checks passed'
