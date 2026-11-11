param([Parameter(Mandatory)][string]$ScenesPath)
$ErrorActionPreference = 'Stop'
$scenes = Get-Content -Raw -LiteralPath $ScenesPath -Encoding utf8 | ConvertFrom-Json
$folder = Split-Path -Parent (Resolve-Path -LiteralPath $ScenesPath)
$voice = New-Object -ComObject SAPI.SpVoice
$voice.Rate = -1
try {
    foreach ($scene in $scenes) {
        $stream = New-Object -ComObject SAPI.SpFileStream
        try {
            $stream.Open((Join-Path $folder "$($scene.id).wav"), 3, $false)
            $voice.AudioOutputStream = $stream
            $voice.Speak($scene.narration) | Out-Null
        } finally {
            $stream.Close()
            [System.Runtime.InteropServices.Marshal]::ReleaseComObject($stream) | Out-Null
        }
    }
} finally {
    [System.Runtime.InteropServices.Marshal]::ReleaseComObject($voice) | Out-Null
}
