# PowerShell script to test password hash generation
# This will help verify if the BCrypt hash is correct

Write-Host "=== BCrypt Password Hash Tester ===" -ForegroundColor Cyan
Write-Host ""

$testPassword = "superadmin123"
$storedHash = '$2b$10$rxCoBwmZ9HVLGNCKKeWuG.cE/FTLUq9q0JqHJhcMKM262STgRoL2m'

Write-Host "Test Password: $testPassword" -ForegroundColor Yellow
Write-Host "Stored Hash: $storedHash" -ForegroundColor Yellow
Write-Host ""
Write-Host "To verify the hash, you need to:" -ForegroundColor Green
Write-Host "1. Run the backend application" -ForegroundColor White
Write-Host "2. Use the BCryptHashGeneratorTest.java test class" -ForegroundColor White
Write-Host "3. Or use an online BCrypt tester like: https://bcrypt-generator.com/" -ForegroundColor White
Write-Host ""
Write-Host "Note: BCrypt hashes cannot be decoded, only verified by comparing." -ForegroundColor Cyan
