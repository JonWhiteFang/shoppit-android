# Fix test assertion issues with nullable types

$testFiles = @(
    "app\src\test\java\com\shoppit\app\data\auth\AuthRepositoryImplTest.kt",
    "app\src\test\java\com\shoppit\app\data\sync\RetryPolicyTest.kt",
    "app\src\test\java\com\shoppit\app\data\sync\SyncEngineImplTest.kt",
    "app\src\test\java\com\shoppit\app\data\sync\SyncIntegrationTest.kt"
)

foreach ($file in $testFiles) {
    if (Test-Path $file) {
        Write-Host "Fixing $file..."
        $content = Get-Content $file -Raw
        
        # Fix: result.getOrNull().property -> result.getOrNull()!!.property
        $content = $content -replace '(\$\w+\.getOrNull\(\))\.(\w+)', '$1!!.$2'
        
        # Fix: result.exceptionOrNull().property -> result.exceptionOrNull()!!.property  
        $content = $content -replace '(\$\w+\.exceptionOrNull\(\))\.(\w+)', '$1!!.$2'
        
        # Fix: user.property -> user!!.property (after getOrNull)
        $content = $content -replace 'val (\w+) = result\.getOrNull\(\)\s+assertNotNull\(\1\)\s+assertEquals\(([^,]+), \1\.', 'val $1 = result.getOrNull()!!`n        assertEquals($2, $1.'
        
        Set-Content $file $content -NoNewline
        Write-Host "Fixed $file"
    }
}

Write-Host "Done!"
