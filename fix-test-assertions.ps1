# Script to replace kotlin.test assertions with org.junit.Assert

$testFiles = Get-ChildItem -Path "app\src\test" -Filter "*.kt" -Recurse

foreach ($file in $testFiles) {
    $content = Get-Content $file.FullName -Raw
    $modified = $false
    
    # Replace kotlin.test imports with org.junit.Assert
    if ($content -match 'import kotlin\.test\.') {
        $content = $content -replace 'import kotlin\.test\.assertEquals', 'import org.junit.Assert.assertEquals'
        $content = $content -replace 'import kotlin\.test\.assertTrue', 'import org.junit.Assert.assertTrue'
        $content = $content -replace 'import kotlin\.test\.assertFalse', 'import org.junit.Assert.assertFalse'
        $content = $content -replace 'import kotlin\.test\.assertNotNull', 'import org.junit.Assert.assertNotNull'
        $content = $content -replace 'import kotlin\.test\.assertNull', 'import org.junit.Assert.assertNull'
        $modified = $true
        Write-Host "Replaced kotlin.test imports in $($file.Name)" -ForegroundColor Yellow
    }
    
    # Save if modified
    if ($modified) {
        Set-Content -Path $file.FullName -Value $content -NoNewline
        Write-Host "Updated $($file.FullName)" -ForegroundColor Green
    }
}

Write-Host "`nAssertion import fixes completed!" -ForegroundColor Cyan
