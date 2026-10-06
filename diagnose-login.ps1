#!/usr/bin/env pwsh
# BookaBeeka Owner Login Diagnostic Script
# This script tests the owner login portal and identifies issues

Write-Host ""
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host " BookaBeeka Login Diagnostics" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

$backendUrl = "http://localhost:8081"
$testEmail = "superadmin@bookabeeka.com"
$testPassword = "superadmin123"

# Test 1: Backend Connectivity
Write-Host "[1/5] Testing backend connectivity..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "$backendUrl/api/auth/owner/login" -Method Options -TimeoutSec 5 -ErrorAction Stop
    Write-Host "✓ Backend is reachable" -ForegroundColor Green
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 401 -or $_.Exception.Response.StatusCode.value__ -eq 404) {
        Write-Host "✓ Backend is reachable (returned $($_.Exception.Response.StatusCode.value__))" -ForegroundColor Green
    } else {
        Write-Host "✗ Cannot reach backend at $backendUrl" -ForegroundColor Red
        Write-Host "  Make sure the Spring Boot backend is running" -ForegroundColor Yellow
        Write-Host "  Error: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
}

Write-Host ""

# Test 2: Login Endpoint
Write-Host "[2/5] Testing login endpoint..." -ForegroundColor Yellow
$loginBody = @{
    email = $testEmail
    password = $testPassword
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "$backendUrl/api/auth/owner/login" `
        -Method Post `
        -ContentType "application/json" `
        -Body $loginBody `
        -ErrorAction Stop
    
    Write-Host "✓ Login successful!" -ForegroundColor Green
    Write-Host "  Token received: $($loginResponse.token.Substring(0, 50))..." -ForegroundColor Cyan
    Write-Host "  User Type: $($loginResponse.userType)" -ForegroundColor Cyan
    
    # Test 3: Token Structure
    Write-Host ""
    Write-Host "[3/5] Analyzing token structure..." -ForegroundColor Yellow
    
    $tokenParts = $loginResponse.token.Split('.')
    if ($tokenParts.Length -eq 3) {
        Write-Host "✓ Token has correct JWT structure (3 parts)" -ForegroundColor Green
        
        # Decode payload (base64url decode)
        $payload = $tokenParts[1]
        # Add padding if needed
        $padding = 4 - ($payload.Length % 4)
        if ($padding -ne 4) {
            $payload += "=" * $padding
        }
        # Replace url-safe characters
        $payload = $payload.Replace('-', '+').Replace('_', '/')
        
        try {
            $decodedBytes = [System.Convert]::FromBase64String($payload)
            $decodedJson = [System.Text.Encoding]::UTF8.GetString($decodedBytes)
            $tokenData = $decodedJson | ConvertFrom-Json
            
            Write-Host "  Claims found:" -ForegroundColor Cyan
            Write-Host "    - sub: $($tokenData.sub)" -ForegroundColor White
            Write-Host "    - role: $($tokenData.role)" -ForegroundColor White
            Write-Host "    - tenant_id: $($tokenData.tenant_id)" -ForegroundColor White
        } catch {
            Write-Host "  Warning: Could not decode token payload" -ForegroundColor Yellow
        }
    } else {
        Write-Host "✗ Token has invalid structure" -ForegroundColor Red
    }
    
    Write-Host ""
    Write-Host "[4/5] Testing frontend configuration..." -ForegroundColor Yellow
    
    $envFile = ".\FrontEnd\.env"
    if (Test-Path $envFile) {
        $envContent = Get-Content $envFile -Raw
        if ($envContent -match "VITE_API_BASE_URL=$backendUrl") {
            Write-Host "✓ Frontend .env configured correctly" -ForegroundColor Green
        } else {
            Write-Host "⚠ Frontend .env might have wrong API URL" -ForegroundColor Yellow
            Write-Host "  Expected: VITE_API_BASE_URL=$backendUrl" -ForegroundColor Cyan
        }
    } else {
        Write-Host "⚠ Frontend .env file not found" -ForegroundColor Yellow
    }
    
    Write-Host ""
    Write-Host "[5/5] Overall Status" -ForegroundColor Yellow
    Write-Host "✓ Backend is working correctly" -ForegroundColor Green
    Write-Host "✓ Login credentials are valid" -ForegroundColor Green
    Write-Host "✓ JWT token generation is working" -ForegroundColor Green
    Write-Host ""
    Write-Host "The backend login system is functioning properly!" -ForegroundColor Green
    Write-Host ""
    Write-Host "If login still fails in the browser:" -ForegroundColor Yellow
    Write-Host "1. Open browser DevTools (F12) > Console" -ForegroundColor White
    Write-Host "2. Try logging in at http://localhost:5173/login/owner" -ForegroundColor White
    Write-Host "3. Check for JavaScript errors or network failures" -ForegroundColor White
    Write-Host "4. Verify the Network tab shows the POST request" -ForegroundColor White
    
} catch {
    Write-Host "✗ Login failed" -ForegroundColor Red
    Write-Host ""
    
    $statusCode = $_.Exception.Response.StatusCode.value__
    Write-Host "  Status Code: $statusCode" -ForegroundColor Red
    
    if ($statusCode -eq 401) {
        Write-Host ""
        Write-Host "Diagnosis: Invalid credentials or account doesn't exist" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "Possible solutions:" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "1. Verify SuperAdmin exists in database:" -ForegroundColor White
        Write-Host "   Run this SQL query:" -ForegroundColor Gray
        Write-Host "   SELECT * FROM super_admin WHERE email = '$testEmail';" -ForegroundColor Gray
        Write-Host ""
        Write-Host "2. If no record exists, create it manually:" -ForegroundColor White
        Write-Host "   Run the migration: Backend\src\main\resources\db\migration\V4__seed_super_admin.sql" -ForegroundColor Gray
        Write-Host ""
        Write-Host "3. Check backend logs for authentication errors:" -ForegroundColor White
        Write-Host "   Look for 'BadCredentialsException' or password mismatch errors" -ForegroundColor Gray
        Write-Host ""
        Write-Host "4. Verify database connection in Backend\src\main\resources\application.properties" -ForegroundColor White
        Write-Host ""
        
    } elseif ($statusCode -eq 404) {
        Write-Host ""
        Write-Host "Diagnosis: Endpoint not found" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "Solution: Verify backend is running with correct API mappings" -ForegroundColor White
        
    } else {
        Write-Host ""
        Write-Host "Diagnosis: Unexpected error" -ForegroundColor Yellow
        Write-Host "Error details: $($_.Exception.Message)" -ForegroundColor Red
    }
    
    Write-Host ""
    Write-Host "[3/5] Skipped (login failed)" -ForegroundColor Gray
    Write-Host "[4/5] Skipped (login failed)" -ForegroundColor Gray
    Write-Host "[5/5] Skipped (login failed)" -ForegroundColor Gray
}

Write-Host ""
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host " Diagnostics Complete" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""
