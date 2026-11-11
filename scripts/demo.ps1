param(
    [ValidateSet('PAID', 'PAYMENT_FAILED')]
    [string]$ExpectedStatus = 'PAID',
    [string]$HostUrl = 'http://127.0.0.1',
    [int]$TimeoutSeconds = 60,
    [string]$EvidencePath
)

$ErrorActionPreference = 'Stop'

function Wait-ForResult {
    param([string]$Url, [scriptblock]$Ready)
    $deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
    do {
        $result = Invoke-RestMethod -Uri $Url -TimeoutSec 15
        if (& $Ready $result) { return $result }
        Start-Sleep -Milliseconds 300
    } while ([DateTime]::UtcNow -lt $deadline)
    throw "Timed out waiting for $Url. Last response: $($result | ConvertTo-Json -Depth 6 -Compress)"
}

$before = Invoke-RestMethod "${HostUrl}:8082/inventory/P1002" -TimeoutSec 15
$body = @{customerId = 'DEMO-CUSTOMER'; items = @(@{productId = 'P1002'; quantity = 3})} | ConvertTo-Json -Depth 4
$order = Invoke-RestMethod "${HostUrl}:8081/orders" -Method Post -ContentType 'application/json' -Body $body -TimeoutSec 60
Write-Output "Created order $($order.id): $($order.status)"
$completed = Wait-ForResult "${HostUrl}:8081/orders/$($order.id)" { param($value) $value.status -eq $ExpectedStatus }
$noticeCount = if ($ExpectedStatus -eq 'PAID') { 2 } else { 1 }
$notices = @(Wait-ForResult "${HostUrl}:8085/notifications/$($order.id)" { param($value) @($value).Count -eq $noticeCount })
$after = Invoke-RestMethod "${HostUrl}:8082/inventory/P1002" -TimeoutSec 15
$expectedStock = if ($ExpectedStatus -eq 'PAID') { $before.availableQuantity - 3 } else { $before.availableQuantity }
if ($after.availableQuantity -ne $expectedStock) { throw "Stock mismatch: expected $expectedStock, got $($after.availableQuantity)" }
if ($ExpectedStatus -eq 'PAID') {
    $delivery = Invoke-RestMethod "${HostUrl}:8084/deliveries/orders/$($order.id)" -TimeoutSec 15
    if ($delivery.orderId -ne $order.id -or $delivery.status -ne 'CREATED') { throw 'Delivery did not match the order' }
    $delivery | ConvertTo-Json
}
$completed | ConvertTo-Json -Depth 6
$notices | ConvertTo-Json -Depth 6
Write-Output "Verified $ExpectedStatus; stock $($before.availableQuantity) -> $($after.availableQuantity); $($notices.Count) notification(s)."
if ($EvidencePath) {
    $parent = Split-Path -Parent $EvidencePath
    if ($parent) { New-Item -ItemType Directory -Force -Path $parent | Out-Null }
    @{
        capturedAt = [DateTime]::UtcNow.ToString('o')
        request = $body | ConvertFrom-Json
        created = $order
        completed = $completed
        stockBefore = $before
        stockAfter = $after
        delivery = $delivery
        notifications = $notices
    } | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath $EvidencePath -Encoding utf8
}
