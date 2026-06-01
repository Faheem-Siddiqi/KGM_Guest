param(
    [string]$OutputDir,
    [switch]$CleanTarget
)

$ErrorActionPreference = "Stop"

$script:StepIndex = 0
$script:TotalSteps = 12
$script:Activity = "KGM app update"

function Write-Step {
    param(
        [string]$Message,
        [int]$Percent
    )

    $script:StepIndex++
    $status = "Step $script:StepIndex of $script:TotalSteps - $Message"
    Write-Progress -Activity $script:Activity -Status $status -PercentComplete $Percent
    Write-Host ("[{0,3}%] {1}" -f $Percent, $Message)
}

function Complete-Progress {
    Write-Progress -Activity $script:Activity -Completed
}

function Resolve-ProjectRoot {
    return Split-Path -Parent $PSCommandPath
}

function Require-Command {
    param(
        [string]$Name,
        [string]$Help
    )

    $command = Get-Command $Name -ErrorAction SilentlyContinue
    if (-not $command) {
        throw "Required tool '$Name' was not found. $Help"
    }

    Write-Host "  OK: $Name -> $($command.Source)"
}

function Check-RequiredTools {
    $requiredTools = @(
        @{ Name = "git"; Help = "Install Git and add it to PATH." },
        @{ Name = "mvn"; Help = "Install Maven and add it to PATH." },
        @{ Name = "java"; Help = "Install JDK 21 and add it to PATH." },
        @{ Name = "jpackage"; Help = "Install a full JDK 21; jpackage is included with the JDK." }
    )

    foreach ($tool in $requiredTools) {
        Require-Command -Name $tool.Name -Help $tool.Help
    }
}

function Check-Wix {
    $wixCommands = @("wix", "candle", "light")
    foreach ($name in $wixCommands) {
        $command = Get-Command $name -ErrorAction SilentlyContinue
        if ($command) {
            Write-Host "  OK: WiX tool found -> $name"
            return $true
        }
    }

    Write-Warning "WiX was not found. Continuing because this script creates a jpackage app-image with KGM.exe. WiX is only needed for MSI/installer packaging."
    return $false
}

function Require-Java21 {
    $versionOutput = & java --version
    if ($LASTEXITCODE -ne 0 -or -not $versionOutput) {
        throw "Could not determine Java version. Install JDK 21 or newer."
    }

    $firstLine = ($versionOutput | Select-Object -First 1).ToString()
    $versionText = ($firstLine -replace "^[^\d]*", "").Trim()
    $majorText = ($versionText -split "\.")[0]
    $major = [int]$majorText
    if ($major -lt 21) {
        throw "Java $versionText found. This project requires JDK 21 or newer."
    }

    Write-Host "  OK: Java $versionText"
}

function Get-EnvFileValue {
    param(
        [string]$Path,
        [string]$Name,
        [string]$DefaultValue
    )

    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        return $DefaultValue
    }

    foreach ($line in Get-Content -LiteralPath $Path) {
        $text = $line.Trim()
        if ($text.Length -eq 0 -or $text.StartsWith("#")) {
            continue
        }
        if ($text.StartsWith("export ")) {
            $text = $text.Substring(7).Trim()
        }
        $separator = $text.IndexOf("=")
        if ($separator -le 0) {
            continue
        }
        $key = $text.Substring(0, $separator).Trim()
        if ($key -ne $Name) {
            continue
        }
        $value = $text.Substring($separator + 1).Trim()
        if (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'"))) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        return $value
    }

    return $DefaultValue
}

function Test-MySqlPort {
    param(
        [string]$HostName = "127.0.0.1",
        [int]$Port = 3306,
        [int]$TimeoutMs = 3000
    )

    $client = $null
    try {
        $client = New-Object System.Net.Sockets.TcpClient
        $async = $client.BeginConnect($HostName, $Port, $null, $null)
        $success = $async.AsyncWaitHandle.WaitOne($TimeoutMs, $false)

        if (-not $success) {
            $client.Close()
            Write-Warning "MySQL check timed out after $TimeoutMs ms. Continuing update..."
            return $false
        }

        $client.EndConnect($async)
        $client.Close()
        Write-Host "  OK: MySQL reachable at ${HostName}:${Port}"
        return $true
    } catch {
        if ($client) {
            $client.Close()
        }
        Write-Warning "MySQL not reachable on ${HostName}:${Port}. Continuing update..."
        return $false
    }
}

function Check-MySqlReachable {
    param([string]$EnvFile)

    $hostName = Get-EnvFileValue -Path $EnvFile -Name "KGM_DB_HOST" -DefaultValue "127.0.0.1"
    $portText = Get-EnvFileValue -Path $EnvFile -Name "KGM_DB_PORT" -DefaultValue "3306"
    $port = [int]$portText

    Test-MySqlPort -HostName $hostName -Port $port -TimeoutMs 3000 | Out-Null
}

function Copy-DirectoryFresh {
    param(
        [string]$Source,
        [string]$Destination
    )

    Invoke-WithRetry -Description "Replacing folder $Destination" -ScriptBlock {
        if (Test-Path -LiteralPath $Destination) {
            Remove-Item -LiteralPath $Destination -Recurse -Force
        }
        Copy-Item -LiteralPath $Source -Destination $Destination -Recurse -Force
    }
}

function Invoke-WithRetry {
    param(
        [string]$Description,
        [scriptblock]$ScriptBlock,
        [int]$Attempts = 6,
        [int]$DelaySeconds = 3
    )

    for ($attempt = 1; $attempt -le $Attempts; $attempt++) {
        try {
            & $ScriptBlock
            return
        } catch {
            $message = $_.Exception.Message
            $isLastAttempt = $attempt -eq $Attempts
            if ($message -match "being used by another process|Access is denied|cannot access the file") {
                if ($isLastAttempt) {
                    throw "$Description failed because a file is locked. Close KGM.exe and any window using D:\KGM-App, then run the script again. Original error: $message"
                }
                Write-Warning "$Description is locked. Close KGM.exe if it is open. Retrying in $DelaySeconds seconds... ($attempt/$Attempts)"
                Start-Sleep -Seconds $DelaySeconds
                continue
            }
            throw
        }
    }
}

function Copy-FileWithRetry {
    param(
        [string]$Source,
        [string]$Destination
    )

    Invoke-WithRetry -Description "Replacing file $Destination" -ScriptBlock {
        Copy-Item -LiteralPath $Source -Destination $Destination -Force
    }
}

function ConvertTo-ArgumentString {
    param([string[]]$Arguments)

    $quoted = foreach ($argument in $Arguments) {
        if ($null -eq $argument) {
            '""'
        } elseif ($argument -match '[\s"]') {
            '"' + ($argument -replace '"', '\"') + '"'
        } else {
            $argument
        }
    }

    return ($quoted -join " ")
}

function Invoke-ExternalCommand {
    param(
        [string]$FilePath,
        [string[]]$Arguments = @(),
        [int]$TimeoutSeconds = 300,
        [string]$Description = $FilePath,
        [switch]$AllowFailure
    )

    $command = Get-Command $FilePath -ErrorAction SilentlyContinue
    if (-not $command) {
        throw "Command not found: $FilePath"
    }

    $resolvedFilePath = $command.Source
    $argumentText = ConvertTo-ArgumentString -Arguments $Arguments
    Write-Host "  Running: $Description"

    if ($resolvedFilePath -match "\.(cmd|bat)$") {
        $runner = $env:ComSpec
        $runnerArgs = "/d /c `"$resolvedFilePath`" $argumentText"
    } else {
        $runner = $resolvedFilePath
        $runnerArgs = $argumentText
    }

    $token = [System.Guid]::NewGuid().ToString("N")
    $stdoutPath = Join-Path ([System.IO.Path]::GetTempPath()) "kgm-$token.out.log"
    $stderrPath = Join-Path ([System.IO.Path]::GetTempPath()) "kgm-$token.err.log"
    $stdoutLines = New-Object System.Collections.Generic.List[string]
    $stderrLines = New-Object System.Collections.Generic.List[string]

    try {
        $process = Start-Process `
            -FilePath $runner `
            -ArgumentList $runnerArgs `
            -WorkingDirectory (Get-Location).Path `
            -NoNewWindow `
            -PassThru `
            -RedirectStandardOutput $stdoutPath `
            -RedirectStandardError $stderrPath
    } catch {
        throw "Could not start: $Description. Reason: $($_.Exception.Message)"
    }

    $started = Get-Date
    while (-not $process.WaitForExit(1000)) {
        $elapsed = [int]((Get-Date) - $started).TotalSeconds
        if ($elapsed -gt 0 -and $elapsed % 30 -eq 0) {
            Write-Host "  Still running: $Description ($elapsed/$TimeoutSeconds seconds)"
        }
        if ($elapsed -ge $TimeoutSeconds) {
            try {
                $process.Kill()
            } catch {
            }
            if (Test-Path -LiteralPath $stdoutPath) {
                foreach ($line in Get-Content -LiteralPath $stdoutPath -ErrorAction SilentlyContinue) {
                    if ($line) {
                        $stdoutLines.Add($line)
                    }
                }
            }
            if (Test-Path -LiteralPath $stderrPath) {
                foreach ($line in Get-Content -LiteralPath $stderrPath -ErrorAction SilentlyContinue) {
                    if ($line) {
                        $stderrLines.Add($line)
                    }
                }
            }
            $reason = Get-TerminationReason -Description $Description -ExitCode $null -TimedOut -StdErrLines $stderrLines -StdOutLines $stdoutLines
            throw "$Description was terminated after $TimeoutSeconds seconds. Reason: $reason"
        }
    }

    $process.WaitForExit()
    $process.Refresh()

    if (Test-Path -LiteralPath $stdoutPath) {
        foreach ($line in Get-Content -LiteralPath $stdoutPath -ErrorAction SilentlyContinue) {
            if ($line) {
                $stdoutLines.Add($line)
                Write-Host $line
            }
        }
    }
    if (Test-Path -LiteralPath $stderrPath) {
        foreach ($line in Get-Content -LiteralPath $stderrPath -ErrorAction SilentlyContinue) {
            if ($line) {
                $stderrLines.Add($line)
                Write-Host $line
            }
        }
    }
    Remove-Item -LiteralPath $stdoutPath, $stderrPath -Force -ErrorAction SilentlyContinue

    $exitCode = $process.ExitCode
    if ($null -eq $exitCode) {
        Write-Warning "$Description finished, but Windows did not report an exit code. Continuing because the process completed."
        return $true
    }

    if ($exitCode -ne 0) {
        $reason = Get-TerminationReason -Description $Description -ExitCode $exitCode -StdErrLines $stderrLines -StdOutLines $stdoutLines
        $message = "$Description failed with exit code $exitCode. Reason: $reason"
        if ($AllowFailure) {
            Write-Warning $message
            return $false
        }
        throw $message
    }

    return $true
}

function Get-TerminationReason {
    param(
        [string]$Description,
        [Nullable[int]]$ExitCode,
        [switch]$TimedOut,
        [System.Collections.Generic.List[string]]$StdErrLines,
        [System.Collections.Generic.List[string]]$StdOutLines
    )

    $combined = @()
    if ($StdErrLines) {
        $combined += $StdErrLines
    }
    if ($StdOutLines) {
        $combined += $StdOutLines
    }
    $text = ($combined -join "`n")

    if ($TimedOut) {
        if ($Description -like "*Maven*") {
            return "Maven took too long. Possible causes: dependency download/network issue, locked files, antivirus scan, or a slow build."
        }
        if ($Description -like "*jpackage*") {
            return "jpackage took too long. Possible causes: antivirus scan, locked output files, or JDK packaging issue."
        }
        if ($Description -like "*git*") {
            return "Git took too long. Possible causes: network issue, authentication prompt, or remote server delay."
        }
        if ($Description -like "*target*") {
            return "Target cleanup took too long. Possible cause: locked files in target folder."
        }
        return "The process exceeded its timeout."
    }

    if ($text -match "Access is denied|being used by another process|cannot access the file|locked") {
        return "A file is locked or access was denied. Close the running app/IDE process or try again as a user with permission."
    }
    if ($text -match "Could not resolve dependencies|Failed to collect dependencies|repo.maven|Unknown host|Connection timed out") {
        return "Maven dependency download failed. Check internet connection, proxy, or Maven repository access."
    }
    if ($text -match "BUILD FAILURE|COMPILATION ERROR|Failed to execute goal") {
        return "Maven build failed. See the Maven error lines above for the exact compile/test/package issue."
    }
    if ($text -match "not a git repository|Your local changes|Authentication failed|could not read Username|Permission denied") {
        return "Git update failed. Check repository state, credentials, or uncommitted local changes."
    }
    if ($text -match "jpackage|Invalid Option|main jar|No module|Error:") {
        return "jpackage failed. Check the JDK installation, jar path, and jpackage output above."
    }
    if ($ExitCode -ne $null) {
        return "The command returned exit code $ExitCode. See output above for details."
    }

    return "No extra error output was captured."
}

function Clear-TargetFolder {
    param([string]$TargetPath = ".\target")

    Write-Host "Cleaning old target folder safely..."

    $workspaceRoot = [System.IO.Path]::GetFullPath((Get-Location).Path)
    $resolvedTarget = [System.IO.Path]::GetFullPath($TargetPath)
    $workspacePrefix = $workspaceRoot + [System.IO.Path]::DirectorySeparatorChar
    if ($resolvedTarget.Equals($workspaceRoot, [System.StringComparison]::OrdinalIgnoreCase) -or -not $resolvedTarget.StartsWith($workspacePrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to clean target outside workspace: $resolvedTarget"
    }

    if (-not (Test-Path -LiteralPath $resolvedTarget)) {
        Write-Host "Target folder does not exist. Nothing to clean."
        return $true
    }

    $job = Start-Job -ScriptBlock {
        param([string]$Path)
        Remove-Item -LiteralPath $Path -Recurse -Force -ErrorAction Stop
    } -ArgumentList $resolvedTarget

    $completed = Wait-Job -Job $job -Timeout 20
    if (-not $completed) {
        Stop-Job -Job $job -ErrorAction SilentlyContinue
        Remove-Job -Job $job -Force -ErrorAction SilentlyContinue
        Write-Warning "Target cleanup timed out after 20 seconds. Skipping clean and continuing with mvn package..."
        return $false
    }

    $jobError = $null
    Receive-Job -Job $job -ErrorAction SilentlyContinue -ErrorVariable jobError | Out-Null
    Remove-Job -Job $job -Force -ErrorAction SilentlyContinue

    if ($jobError -or (Test-Path -LiteralPath $resolvedTarget)) {
        Write-Warning "Target folder is locked or could not be removed. Skipping clean and continuing with mvn package..."
        return $false
    }

    Write-Host "Target folder cleaned."
    return $true
}

function Backup-AppFiles {
    param(
        [string]$OutputPath,
        [string]$Label
    )

    $timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $backupDir = Join-Path $OutputPath "backups\$Label-$timestamp"
    New-Item -ItemType Directory -Path $backupDir -Force | Out-Null

    $items = @("KGM.exe", "app", "runtime")
    $hasBackup = $false
    foreach ($item in $items) {
        $path = Join-Path $OutputPath $item
        if (Test-Path -LiteralPath $path) {
            Copy-Item -LiteralPath $path -Destination $backupDir -Recurse -Force
            $hasBackup = $true
        }
    }

    if ($hasBackup) {
        Write-Host "  Backup created: $backupDir"
    } else {
        Write-Host "  No existing app files found to back up."
    }
}

try {
    $projectRoot = Resolve-ProjectRoot
    Set-Location -LiteralPath $projectRoot

    if (-not $OutputDir) {
        $OutputDir = Read-Host "Output folder"
    }
    if (-not $OutputDir) {
        throw "OutputDir is required."
    }

    Write-Step -Message "Resolving project and output folders" -Percent 5
    $resolvedOutput = [System.IO.Path]::GetFullPath($OutputDir)
    if (-not (Test-Path -LiteralPath $resolvedOutput -PathType Container)) {
        throw "Output folder does not exist. Run build-exe.ps1 first: $resolvedOutput"
    }

    Write-Step -Message "Checking required tools dynamically" -Percent 12
    Check-RequiredTools
    Check-Wix | Out-Null

    Write-Step -Message "Checking Java 21" -Percent 20
    Require-Java21

    Write-Step -Message "Checking .env and MySQL with 3000ms timeout" -Percent 28
    $projectEnv = Join-Path $projectRoot ".env"
    $outputEnv = Join-Path $resolvedOutput "config\.env"
    if (Test-Path -LiteralPath $projectEnv -PathType Leaf) {
        Check-MySqlReachable -EnvFile $projectEnv
    } elseif (Test-Path -LiteralPath $outputEnv -PathType Leaf) {
        Check-MySqlReachable -EnvFile $outputEnv
    } else {
        Write-Warning ".env was not found in project root or output config folder. Continuing update, but the app may not run until config\.env exists."
    }

    Write-Step -Message "Pulling latest code from Git" -Percent 36
    Invoke-ExternalCommand -FilePath "git" -Arguments @("pull") -TimeoutSeconds 180 -Description "git pull" | Out-Null

    Write-Step -Message "Preparing target folder before Maven package" -Percent 44
    if ($CleanTarget) {
        Clear-TargetFolder -TargetPath ".\target" | Out-Null
    } else {
        Write-Host "Skipping target cleanup by default to avoid terminating the VS Code PowerShell terminal."
        Write-Host "Use -CleanTarget only when you specifically need to clean target\."
    }

    Write-Step -Message "Running Maven package" -Percent 48
    Invoke-ExternalCommand -FilePath "mvn" -Arguments @("package") -TimeoutSeconds 900 -Description "Maven package" | Out-Null

    $jarName = "my-java-app-1.0.0.jar"
    $jarPath = Join-Path $projectRoot "target\$jarName"
    if (-not (Test-Path -LiteralPath $jarPath -PathType Leaf)) {
        throw "Runnable jar was not created: $jarPath"
    }

    Write-Step -Message "Running jpackage to create updated KGM.exe" -Percent 66
    $jPackageWorkRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("kgm-jpackage-update-" + [System.Guid]::NewGuid().ToString("N"))
    $packageTemp = Join-Path $jPackageWorkRoot "output"
    $packageApp = Join-Path $packageTemp "KGM"
    $jPackageInput = Join-Path $jPackageWorkRoot "input"
    New-Item -ItemType Directory -Path $packageTemp -Force | Out-Null
    New-Item -ItemType Directory -Path $jPackageInput -Force | Out-Null
    Copy-Item -LiteralPath $jarPath -Destination (Join-Path $jPackageInput $jarName) -Force

    Invoke-ExternalCommand -FilePath "jpackage" -Arguments @(
        "--type", "app-image",
        "--dest", $packageTemp,
        "--name", "KGM",
        "--input", $jPackageInput,
        "--main-jar", $jarName,
        "--main-class", "com.kgm.Main",
        "--app-version", "1.0.0",
        "--java-options", "-Dfile.encoding=UTF-8"
    ) -TimeoutSeconds 600 -Description "jpackage app image" | Out-Null

    Write-Step -Message "Ensuring protected folders exist" -Percent 76
    New-Item -ItemType Directory -Path (Join-Path $resolvedOutput "backups") -Force | Out-Null
    New-Item -ItemType Directory -Path (Join-Path $resolvedOutput "logs") -Force | Out-Null
    New-Item -ItemType Directory -Path (Join-Path $resolvedOutput "employees") -Force | Out-Null
    New-Item -ItemType Directory -Path (Join-Path $resolvedOutput "images\uploads") -Force | Out-Null
    New-Item -ItemType Directory -Path (Join-Path $resolvedOutput "config") -Force | Out-Null

    Write-Step -Message "Backing up old KGM.exe, app, and runtime" -Percent 84
    Backup-AppFiles -OutputPath $resolvedOutput -Label "update"

    Write-Step -Message "Replacing generated app files only" -Percent 94
    Copy-FileWithRetry -Source (Join-Path $packageApp "KGM.exe") -Destination (Join-Path $resolvedOutput "KGM.exe")
    Copy-DirectoryFresh -Source (Join-Path $packageApp "app") -Destination (Join-Path $resolvedOutput "app")
    Copy-DirectoryFresh -Source (Join-Path $packageApp "runtime") -Destination (Join-Path $resolvedOutput "runtime")

    Write-Step -Message "Update complete" -Percent 100
    Complete-Progress
    Write-Host ""
    Write-Host "SUCCESS: KGM app package was updated."
    Write-Host "Output: $resolvedOutput"
    Write-Host "Protected folders were not deleted: config, employees, images/uploads, logs, backups"
    Remove-Item -LiteralPath $jPackageWorkRoot -Recurse -Force -ErrorAction SilentlyContinue
} catch {
    Complete-Progress
    Write-Error "UPDATE FAILED: $($_.Exception.Message)"
    return
}
