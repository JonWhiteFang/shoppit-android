# Script to fix missing imports in test files

$testFiles = Get-ChildItem -Path "app\src\test" -Filter "*.kt" -Recurse

foreach ($file in $testFiles) {
    $content = Get-Content $file.FullName -Raw
    $modified = $false
    
    # Check if file has test methods
    if ($content -match '@Test') {
        
        # Add kotlinx.coroutines.test.runTest if using runTest but not imported
        if ($content -match '\brunTest\b' -and $content -notmatch 'import kotlinx\.coroutines\.test\.runTest') {
            $content = $content -replace '(package [^\n]+\n)', "`$1`nimport kotlinx.coroutines.test.runTest"
            $modified = $true
            Write-Host "Added runTest import to $($file.Name)"
        }
        
        # Add ExperimentalCoroutinesApi if using runTest
        if ($content -match '\brunTest\b' -and $content -notmatch 'import kotlinx\.coroutines\.ExperimentalCoroutinesApi') {
            $content = $content -replace '(package [^\n]+\n)', "`$1`nimport kotlinx.coroutines.ExperimentalCoroutinesApi"
            $modified = $true
            Write-Host "Added ExperimentalCoroutinesApi import to $($file.Name)"
        }
        
        # Ensure kotlin.test assertions are imported if used
        if ($content -match '\bassertTrue\b' -and $content -notmatch 'import kotlin\.test\.assertTrue') {
            $content = $content -replace '(package [^\n]+\n)', "`$1`nimport kotlin.test.assertTrue"
            $modified = $true
            Write-Host "Added assertTrue import to $($file.Name)"
        }
        
        if ($content -match '\bassertFalse\b' -and $content -notmatch 'import kotlin\.test\.assertFalse') {
            $content = $content -replace '(package [^\n]+\n)', "`$1`nimport kotlin.test.assertFalse"
            $modified = $true
            Write-Host "Added assertFalse import to $($file.Name)"
        }
        
        if ($content -match '\bassertEquals\b' -and $content -notmatch 'import kotlin\.test\.assertEquals') {
            $content = $content -replace '(package [^\n]+\n)', "`$1`nimport kotlin.test.assertEquals"
            $modified = $true
            Write-Host "Added assertEquals import to $($file.Name)"
        }
        
        if ($content -match '\bassertNotNull\b' -and $content -notmatch 'import kotlin\.test\.assertNotNull') {
            $content = $content -replace '(package [^\n]+\n)', "`$1`nimport kotlin.test.assertNotNull"
            $modified = $true
            Write-Host "Added assertNotNull import to $($file.Name)"
        }
        
        if ($content -match '\bassertNull\b' -and $content -notmatch 'import kotlin\.test\.assertNull') {
            $content = $content -replace '(package [^\n]+\n)', "`$1`nimport kotlin.test.assertNull"
            $modified = $true
            Write-Host "Added assertNull import to $($file.Name)"
        }
        
        # Save if modified
        if ($modified) {
            Set-Content -Path $file.FullName -Value $content -NoNewline
            Write-Host "Updated $($file.FullName)" -ForegroundColor Green
        }
    }
}

Write-Host "`nImport fixes completed!" -ForegroundColor Cyan
