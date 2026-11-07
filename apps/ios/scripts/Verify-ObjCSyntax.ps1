$ErrorActionPreference = "Stop"
$srcDir = Join-Path $PSScriptRoot "..\Sources"
$objCFile = Join-Path $srcDir "MyCaffeHardwareBridge.m"

if (Get-Command zig -ErrorAction SilentlyContinue) {
    Write-Output "[MyCaffe iOS Lint] Compiling & verifying Objective-C module via embedded LLVM/Clang ('zig cc')..."
    $outObj = Join-Path $env:TEMP "MyCaffeHardwareBridge.o"
    & zig cc -x objective-c -c -Wall -Wextra -Werror -I"$srcDir" "$objCFile" -o "$outObj"
    if ($LASTEXITCODE -ne 0) { throw "Objective-C compilation check failed with exit code $LASTEXITCODE" }
    Write-Output "[PASS] Objective-C compilation check succeeded cleanly on Windows 11 -> $outObj"
} elseif (Get-Command clang -ErrorAction SilentlyContinue) {
    & clang -x objective-c -fsyntax-only -Wall -Wextra -Werror -I"$srcDir" "$objCFile"
    Write-Output "[PASS] Objective-C syntax check succeeded via Clang."
} else {
    throw "Neither 'zig' nor 'clang' found on PATH for local Objective-C syntax verification."
}
