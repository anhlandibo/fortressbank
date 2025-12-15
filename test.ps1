# --- BƯỚC 1: CẤU HÌNH ---
# QUAN TRỌNG: Thay Key MỚI của bạn vào đây (Key cũ đã lộ, đừng dùng nữa!)


# Lấy thời gian hiện tại (Unix Timestamp) để không bị lỗi "Date too old"
$currentTimestamp = [DateTimeOffset]::Now.ToUnixTimeSeconds()

$headers = @{
    "Authorization" = "Bearer $stripeKey"
}

# --- BƯỚC 2: TẠO BODY DỮ LIỆU ---
$body = @{
    "type" = "custom"
    "country" = "US"
    "email" = "user_fix_final_01@example.com" # Đổi email khác nếu cần
    "capabilities[card_payments][requested]" = "true"
    "capabilities[transfers][requested]" = "true"
    "business_type" = "individual"
    
    # MẸO: Dùng description thay cho URL để tránh lỗi "Not a valid URL"
    "business_profile[product_description]" = "Demo User for Banking App"
    "business_profile[mcc]" = "5734" # Mã ngành phần mềm
    
    # Dùng thời gian hiện tại
    "tos_acceptance[date]" = $currentTimestamp
    "tos_acceptance[ip]" = "127.0.0.1"
    
    "individual[first_name]" = "Tai"
    "individual[last_name]" = "Khoan Test"
    "individual[address][line1]" = "123 Market St"
    "individual[address][city]" = "San Francisco"
    "individual[address][state]" = "CA"
    "individual[address][postal_code]" = "94111"
    "individual[ssn_last_4]" = "0000"
}

# --- BƯỚC 3: GỌI STRIPE ---
try {
    Write-Host "Dang gui lenh tao Account..." -ForegroundColor Yellow
    $response = Invoke-RestMethod -Uri "https://api.stripe.com/v1/accounts" -Method Post -Headers $headers -Body $body
    
    Write-Host "`n>>> THANH CONG! (SUCCESS)" -ForegroundColor Green
    Write-Host "--------------------------------"
    Write-Host "New Account ID: $($response.id)" -ForegroundColor Cyan
    Write-Host "Type: $($response.type)"
    Write-Host "Email: $($response.email)"
    Write-Host "Payouts Enabled: $($response.payouts_enabled)"
    Write-Host "--------------------------------"
} catch {
    Write-Host "`n>>> THAT BAI! (FAILED)" -ForegroundColor Red
    Write-Host "Error Code: $($_.Exception.Response.StatusCode)" -ForegroundColor Yellow
    
    # Đọc và in chi tiết lỗi từ JSON của Stripe để biết sai ở đâu
    if ($_.Exception.Response) {
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        $errorJson = $reader.ReadToEnd()
        Write-Host "Chi tiet loi tu Stripe:" -ForegroundColor Red
        Write-Host $errorJson -ForegroundColor Gray
    }
}