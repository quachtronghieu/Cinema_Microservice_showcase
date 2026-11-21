# DEV ONLY: prepares booking_db data for docs/postman/cinema-booking.postman_collection.json and writes the
# git-ignored Postman environment docs/postman/booking-local.postman_environment.json.
# Needs: sqlcmd with Windows authentication to the local SQL Server, booking_db seeded by
# docs/migration/booking_sales_snapshot_import.sql. Run again before every collection run (holds are single-use).
#
# Usage (from the repository root):
#   powershell -ExecutionPolicy Bypass -File docs/postman/prepare-booking-tests.ps1 -CustomerPassword 'your-password' -AdminPassword 'admin-password'
#   add -RunNewman to run the collection from the command line as well.
param(
    [string]$Username = 'Tuongvy1',
    [string]$CustomerPassword = '',
    [string]$CustomerId = 'CS000003',
    [string]$OtherCustomerId = 'CS000001',
    [string]$AdminUsername = 'admin_demo',
    [string]$AdminPassword = '',
    [string]$BaseUrl = 'http://localhost:8080',
    [string]$LoginUrl = '',
    [switch]$RunNewman
)
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
if (-not $LoginUrl) { $LoginUrl = $BaseUrl }

function Invoke-Sql([string]$query) {
    $out = sqlcmd -S localhost -E -C -I -b -W -h -1 -s '|' -d booking_db -Q "SET NOCOUNT ON; $query"
    if ($LASTEXITCODE -ne 0) { throw "sqlcmd failed: $out" }
    return @($out | Where-Object { $_ -and $_.Trim() })
}

function New-Hold([string]$customer, [string]$showtime, [int]$seatCount, [int]$minutes) {
    $script = Join-Path $root 'docs\dev\create_test_hold.sql'
    $out = sqlcmd -S localhost -E -C -I -b -W -h -1 -s '|' -d booking_db -i $script `
        -v customerId="$customer" showtimeId="$showtime" seatCount=$seatCount minutes=$minutes
    if ($LASTEXITCODE -ne 0) { throw "create_test_hold.sql failed: $out" }
    $line = $out | Where-Object { $_ -match '^[0-9a-f-]{36}\|' } | Select-Object -First 1
    $parts = $line.Split('|')
    return [pscustomobject]@{ holdId = $parts[0]; showtimeId = $parts[1]; seats = @($parts[2].Split(',')) }
}

# Five different open showtimes: one customer may hold seats in only one hold per showtime.
$showtimes = Invoke-Sql @"
SELECT TOP 5 s.showtime_id FROM dbo.Sales_Snapshot s
JOIN dbo.Showtime_Seat t ON t.showtime_id = s.showtime_id AND t.status = 'AVAILABLE'
WHERE s.sales_status = 'OPEN' GROUP BY s.showtime_id HAVING COUNT(*) >= 2 ORDER BY s.showtime_id
"@
if ($showtimes.Count -lt 5) { throw 'Need 5 open showtimes with free seats. Run docs/migration/booking_sales_snapshot_import.sql.' }

$holdA = New-Hold $CustomerId $showtimes[0] 2 30        # happy path
$holdB = New-Hold $CustomerId $showtimes[1] 2 30        # error cases, then voucher booking
$holdC = New-Hold $CustomerId $showtimes[2] 1 30        # voucher already used up
$holdX = New-Hold $CustomerId $showtimes[3] 1 0         # expires immediately
$holdO = New-Hold $OtherCustomerId $showtimes[4] 1 30   # belongs to another customer

# Fresh one-use 10% voucher per run so reruns never collide.
$code = 'PM' + (-join ((48..57) + (65..90) | Get-Random -Count 6 | ForEach-Object { [char]$_ }))
Invoke-Sql "INSERT INTO dbo.Voucher (voucher_id, code, voucher_name, discount_type, discount_value, usage_limit, used_count, reserved_count, status) VALUES (LOWER(CONVERT(VARCHAR(36), NEWID())), '$code', N'Postman 10%', 'PERCENTAGE', 10, 1, 0, 0, 1)" | Out-Null

function Seats-Json($hold) { return '[' + (($hold.seats | ForEach-Object { '"' + $_ + '"' }) -join ',') + ']' }
$values = [ordered]@{
    baseUrl = $BaseUrl; loginUrl = $LoginUrl; username = $Username; password = $CustomerPassword
    holdA = $holdA.holdId; showtimeA = $holdA.showtimeId; seatsA = (Seats-Json $holdA)
    holdB = $holdB.holdId; showtimeB = $holdB.showtimeId; seatsB = (Seats-Json $holdB); seatB1 = $holdB.seats[0]
    holdC = $holdC.holdId; showtimeC = $holdC.showtimeId; seatsC = (Seats-Json $holdC)
    holdExpired = $holdX.holdId; showtimeExpired = $holdX.showtimeId; seatsExpired = (Seats-Json $holdX)
    holdOther = $holdO.holdId; showtimeOther = $holdO.showtimeId; seatsOther = (Seats-Json $holdO)
    voucherCode = $code; adminUsername = $AdminUsername; adminPassword = $AdminPassword
}
$environment = [ordered]@{
    name = 'Cinema Booking (local)'
    values = @($values.GetEnumerator() | ForEach-Object {
        [ordered]@{ key = $_.Key; value = [string]$_.Value; type = $(if ($_.Key -in 'password', 'adminPassword') { 'secret' } else { 'default' }); enabled = $true } })
}
$envFile = Join-Path $PSScriptRoot 'booking-local.postman_environment.json'
$environment | ConvertTo-Json -Depth 5 | Set-Content -Path $envFile -Encoding utf8
Write-Host "Prepared 5 holds and voucher $code. Environment: $envFile"
if (-not $CustomerPassword) { Write-Host "Set 'password' in the Postman environment before running." }
if (-not $AdminPassword) { Write-Host "Set 'adminPassword' in the Postman environment before running requests 19-23." }

if ($RunNewman) {
    $ErrorActionPreference = 'Continue'  # npm prints deprecation warnings on stderr; they are not failures
    npx -y newman@6.2.3 run (Join-Path $PSScriptRoot 'cinema-booking.postman_collection.json') -e $envFile --color off 2>$null
    exit $LASTEXITCODE
}
