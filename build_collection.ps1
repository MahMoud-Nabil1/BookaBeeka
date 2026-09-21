# Build collection object
$folders = @()

# ── 01 Auth ──────────────────────────────────────────────────────────────
function req($name,$method,$url,$body,$auth,$extraHeaders) {
  $h = @(@{key="Content-Type";value="application/json"})
  if ($auth) { $h += @{key="Authorization";value="Bearer $auth"} }
  if ($extraHeaders) { $h += $extraHeaders }
  $r = @{name=$name;request=@{method=$method;header=$h;url=@{raw=$url}}}
  if ($body) { $r.request.body = @{mode="raw";raw=$body} }
  $r
}

function folder($name,$items) { @{name=$name;item=$items} }
function withTest($req,$tests) { $req.event = @(@{listen="test";script=@{exec=$tests}}); $req }

$authItems = @(
  withTest (req "Register Owner (skip if exists)" "POST" "{{baseUrl}}/api/auth/register/owner" '{"firstName":"Ahmed","lastName":"Hassan","email":"owner@grandhotel.com","password":"Owner@2024","tenantName":"Grand Hotel","subdomain":"grand-hotel","phone":"+201001234567"}') @("if([201,409].includes(pm.response.code)){pm.test('OK',()=>true);if(pm.response.code===201){const b=pm.response.json();if(b.tenantId)pm.collectionVariables.set('tenantId',b.tenantId);}}"),
  withTest (req "Login Owner -> ownerToken" "POST" "{{baseUrl}}/api/auth/login/owner" '{"email":"owner@grandhotel.com","password":"Owner@2024"}') @("pm.test('200',()=>pm.response.to.have.status(200));const b=pm.response.json();pm.collectionVariables.set('ownerToken',b.token);if(b.tenantId)pm.collectionVariables.set('tenantId',b.tenantId);"),
  withTest (req "Appoint Hotel Admin" "POST" "{{baseUrl}}/api/owner/admin" '{"firstName":"Sara","lastName":"Ali","email":"admin@grandhotel.com","password":"Admin@2024"}' "{{ownerToken}}") @("pm.test('OK',()=>[201,409].includes(pm.response.code));"),
  withTest (req "Login Admin -> adminToken" "POST" "{{baseUrl}}/api/auth/login/admin" '{"email":"admin@grandhotel.com","password":"Admin@2024"}') @("pm.test('200',()=>pm.response.to.have.status(200));pm.collectionVariables.set('adminToken',pm.response.json().token);"),
  withTest (req "Register Customer (skip if exists)" "POST" "{{baseUrl}}/api/auth/register/customer" '{"firstName":"Mohamed","lastName":"Nabil","email":"customer@test.com","password":"Customer@2024","phone":"+201009876543"}') @("pm.test('OK',()=>[201,409].includes(pm.response.code));"),
  withTest (req "Login Customer -> customerToken" "POST" "{{baseUrl}}/api/auth/login/customer" '{"email":"customer@test.com","password":"Customer@2024"}') @("pm.test('200',()=>pm.response.to.have.status(200));const b=pm.response.json();pm.collectionVariables.set('customerToken',b.token);const cid=b.customerId||b.userId||b.id;if(cid)pm.collectionVariables.set('customerId',cid);console.log('customerId:',cid);"),
  withTest (req "Login SuperAdmin -> superAdminToken" "POST" "{{baseUrl}}/api/auth/login/super-admin" '{"email":"superadmin@bookabeeka.com","password":"superadmin123"}') @("pm.test('200',()=>pm.response.to.have.status(200));pm.collectionVariables.set('superAdminToken',pm.response.json().token);")
)

$tenantItems = @(
  withTest (@{name="Get Tenant by Subdomain -> tenantId";request=@{method="GET";header=@();url=@{raw="{{baseUrl}}/api/tenants/by-subdomain/grand-hotel"}}}) @("pm.test('200',()=>pm.response.to.have.status(200));const b=pm.response.json();if(b.id)pm.collectionVariables.set('tenantId',b.id);"),
  withTest (@{name="Get My Tenant Profile (Owner)";request=@{method="GET";header=@(@{key="Authorization";value="Bearer {{ownerToken}}"});url=@{raw="{{baseUrl}}/api/owner/tenant"}}}) @("pm.test('200',()=>pm.response.to.have.status(200));")
)

$invItems = @(
  withTest (req "Create Room Type -> roomTypeId" "POST" "{{baseUrl}}/api/inventory/room-types?tenantId={{tenantId}}" '{"name":"Deluxe Room","description":"Nile view","defaultCapacity":2,"defaultSpecs":{"bedType":"KING","view":"Nile"}}' "{{ownerToken}}") @("pm.test('OK',()=>[200,201].includes(pm.response.code));const b=pm.response.json();const id=b.id||b.roomTypeId;if(id)pm.collectionVariables.set('roomTypeId',id);console.log('roomTypeId:',id);"),
  withTest (req "Create Room -> roomId" "POST" "{{baseUrl}}/api/inventory/resources?tenantId={{tenantId}}" '{"name":"Room 101","roomTypeId":"{{roomTypeId}}","capacity":2,"pricePerNight":850,"currency":"EGP","isActive":true,"isBookable":true,"specs":{"bedType":"KING","view":"Nile"}}' "{{ownerToken}}") @("pm.test('OK',()=>[200,201].includes(pm.response.code));const b=pm.response.json();const id=b.id||b.resourceId||b.roomId;if(id)pm.collectionVariables.set('roomId',id);console.log('roomId:',id);"),
  withTest (@{name="List Rooms (verify)";request=@{method="GET";header=@(@{key="Authorization";value="Bearer {{ownerToken}}"});url=@{raw="{{baseUrl}}/api/inventory/resources?tenantId={{tenantId}}"}}} ) @("pm.test('200',()=>pm.response.to.have.status(200));const arr=pm.response.json();const list=Array.isArray(arr)?arr:(arr.content||[]);pm.test('Rooms exist',()=>pm.expect(list.length).to.be.above(0));if(!pm.collectionVariables.get('roomId')&&list.length>0){const id=list[0].id||list[0].resourceId;if(id)pm.collectionVariables.set('roomId',id);}"),
  withTest (req "Create Service Offering -> serviceId" "POST" "{{baseUrl}}/api/inventory/service-offerings?tenantId={{tenantId}}" '{"name":"Standard Stay","description":"Per-night","price":850,"currency":"EGP","durationMinutes":1440,"bufferMinutes":60}' "{{ownerToken}}") @("pm.test('OK',()=>[200,201].includes(pm.response.code));const b=pm.response.json();const id=b.id||b.serviceId||b.serviceOfferingId;if(id)pm.collectionVariables.set('serviceId',id);")
)

$avItems = @(
  withTest (@{name="Search Available Rooms (Public)";request=@{method="GET";header=@();url=@{raw="{{baseUrl}}/api/availability/search?hotelId={{tenantId}}&checkIn=2026-10-01&checkOut=2026-10-05&page=0&size=20"}}}) @("pm.test('200',()=>pm.response.to.have.status(200));const b=pm.response.json();const rooms=b.content||b;pm.test('Rooms found',()=>pm.expect(rooms.length).to.be.above(0));"),
  withTest (req "Create Schedule Rule Thu=4" "POST" "{{baseUrl}}/api/availability/schedule-rules?tenantId={{tenantId}}&resourceId={{roomId}}" '{"dayOfWeek":4,"startTime":"14:00","endTime":"23:00"}' "{{adminToken}}") @("pm.test('OK',()=>[200,201].includes(pm.response.code));"),
  withTest (@{name="Get Available Slots (Legacy)";request=@{method="GET";header=@(@{key="Authorization";value="Bearer {{customerToken}}"});url=@{raw="{{baseUrl}}/api/availability/slots?tenantId={{tenantId}}&resourceId={{roomId}}&date=2026-10-01"}}}) @("pm.test('200',()=>pm.response.to.have.status(200));console.log('Slots:',pm.response.json().length);")
)

$bookItems = @(
  withTest (req "Create Booking -> bookingId" "POST" "{{baseUrl}}/api/bookings" '{"tenantId":"{{tenantId}}","roomId":"{{roomId}}","start":"2026-10-01T14:00:00Z","end":"2026-10-05T11:00:00Z","checkInDate":"2026-10-01","checkOutDate":"2026-10-05","numberOfRooms":1,"specialRequests":"High floor please","metadata":{"source":"e2e"}}' "{{customerToken}}" @(@{key="Idempotency-Key";value="e2e-oct-001"})) @("pm.test('201',()=>pm.response.to.have.status(201));const b=pm.response.json();pm.collectionVariables.set('bookingId',b.bookingId);pm.test('PENDING_PAYMENT',()=>pm.expect(b.status).to.eql('PENDING_PAYMENT'));console.log('bookingId:',b.bookingId);"),
  withTest (@{name="Booking Status -> PENDING_PAYMENT";request=@{method="GET";header=@(@{key="Authorization";value="Bearer {{customerToken}}"});url=@{raw="{{baseUrl}}/api/bookings/{{bookingId}}/status?tenantId={{tenantId}}"}}} ) @("pm.test('PENDING_PAYMENT',()=>pm.expect(pm.response.json().status).to.eql('PENDING_PAYMENT'));"),
  withTest (@{name="List My Bookings -> totalAmount";request=@{method="GET";header=@(@{key="Authorization";value="Bearer {{customerToken}}"});url=@{raw="{{baseUrl}}/api/bookings/mine?tenantId={{tenantId}}"}}} ) @("pm.test('200',()=>pm.response.to.have.status(200));const bks=pm.response.json();const tid=pm.collectionVariables.get('bookingId');const found=bks.find(b=>b.bookingId===tid||b.id===tid);if(found&&found.totalAmount){pm.collectionVariables.set('totalAmount',String(found.totalAmount));console.log('totalAmount:',found.totalAmount);}")
)

$payItems = @(
  withTest (req "Top Up Wallet 5000 EGP" "POST" "{{baseUrl}}/api/payments/wallet/top-up" '{"customerId":"{{customerId}}","amount":5000}' "{{customerToken}}") @("pm.test('201',()=>pm.response.to.have.status(201));pm.test('Balance>0',()=>pm.expect(pm.response.json().balanceAfter).to.be.above(0));"),
  withTest (req "Checkout -> booking auto-CONFIRMED" "POST" "{{baseUrl}}/api/payments/wallet/checkout" '{"bookingId":"{{bookingId}}","customerId":"{{customerId}}","tenantId":"{{tenantId}}","paymentAmount":{{totalAmount}},"idempotencyKey":"chk-{{bookingId}}","expectedCurrency":"EGP"}' "{{customerToken}}") @("pm.test('201',()=>pm.response.to.have.status(201));const b=pm.response.json();pm.test('COMPLETED',()=>pm.expect(b.status).to.eql('COMPLETED'));const id=b.id||b.paymentId;if(id)pm.collectionVariables.set('paymentId',id);"),
  withTest (@{name="Booking Status -> CONFIRMED (auto after payment)";request=@{method="GET";header=@(@{key="Authorization";value="Bearer {{customerToken}}"});url=@{raw="{{baseUrl}}/api/bookings/{{bookingId}}/status?tenantId={{tenantId}}"}}} ) @("pm.test('CONFIRMED',()=>pm.expect(pm.response.json().status).to.eql('CONFIRMED'));"),
  withTest (@{name="Get Customer Balance";request=@{method="GET";header=@(@{key="Authorization";value="Bearer {{customerToken}}"});url=@{raw="{{baseUrl}}/api/payments/wallet/balance?customerId={{customerId}}"}}} ) @("pm.test('200',()=>pm.response.to.have.status(200));"),
  withTest (@{name="Get Payment Summary for Booking";request=@{method="GET";header=@(@{key="Authorization";value="Bearer {{ownerToken}}"});url=@{raw="{{baseUrl}}/api/payments/booking/{{bookingId}}/summary"}}} ) @("pm.test('200',()=>pm.response.to.have.status(200));")
)

$reviewItems = @(
  withTest (@{name="Mark Booking COMPLETED (Owner)";request=@{method="POST";header=@(@{key="Authorization";value="Bearer {{ownerToken}}"});url=@{raw="{{baseUrl}}/api/bookings/{{bookingId}}/complete"}}} ) @("pm.test('200',()=>pm.response.to.have.status(200));"),
  withTest (@{name="Booking Status -> COMPLETED";request=@{method="GET";header=@(@{key="Authorization";value="Bearer {{customerToken}}"});url=@{raw="{{baseUrl}}/api/bookings/{{bookingId}}/status?tenantId={{tenantId}}"}}} ) @("pm.test('COMPLETED',()=>pm.expect(pm.response.json().status).to.eql('COMPLETED'));"),
  withTest (req "Create Review -> reviewId" "POST" "{{baseUrl}}/api/reviews" '{"tenantId":"{{tenantId}}","bookingId":"{{bookingId}}","serviceId":"{{roomId}}","rating":5,"comment":"Excellent stay! Nile view was breathtaking."}' "{{customerToken}}") @("pm.test('201',()=>pm.response.to.have.status(201));const b=pm.response.json();pm.test('isVerified',()=>pm.expect(b.isVerified).to.be.true);pm.test('rating 5',()=>pm.expect(b.rating).to.eql(5));const id=b.id||b.reviewId;if(id)pm.collectionVariables.set('reviewId',id);"),
  withTest (@{name="Get My Reviews";request=@{method="GET";header=@(@{key="Authorization";value="Bearer {{customerToken}}"});url=@{raw="{{baseUrl}}/api/reviews/mine?page=0&size=20"}}} ) @("pm.test('200',()=>pm.response.to.have.status(200));pm.test('Has review',()=>pm.expect(pm.response.json().totalElements).to.be.above(0));"),
  withTest (@{name="List Tenant Reviews (Admin)";request=@{method="GET";header=@(@{key="Authorization";value="Bearer {{adminToken}}"});url=@{raw="{{baseUrl}}/api/admin/reviews?page=0&size=20"}}} ) @("pm.test('200',()=>pm.response.to.have.status(200));"),
  withTest (req "Admin Reply to Review" "POST" "{{baseUrl}}/api/admin/reviews/{{reviewId}}/reply" '{"reply":"Thank you! We look forward to welcoming you again."}' "{{adminToken}}") @("pm.test('200',()=>pm.response.to.have.status(200));pm.test('reply set',()=>pm.expect(pm.response.json().reply).to.be.a('string'));")
)

$refundItems = @(
  withTest (req "Create Booking 2 for Refund" "POST" "{{baseUrl}}/api/bookings" '{"tenantId":"{{tenantId}}","roomId":"{{roomId}}","start":"2026-11-10T14:00:00Z","end":"2026-11-13T11:00:00Z","checkInDate":"2026-11-10","checkOutDate":"2026-11-13","numberOfRooms":1,"specialRequests":"Refund test"}' "{{customerToken}}" @(@{key="Idempotency-Key";value="e2e-refund-nov-001"})) @("pm.test('201',()=>pm.response.to.have.status(201));pm.collectionVariables.set('bookingId2',pm.response.json().bookingId);"),
  withTest (req "Pay for Booking 2 (2550 EGP)" "POST" "{{baseUrl}}/api/payments/wallet/checkout" '{"bookingId":"{{bookingId2}}","customerId":"{{customerId}}","tenantId":"{{tenantId}}","paymentAmount":2550,"idempotencyKey":"chk-{{bookingId2}}","expectedCurrency":"EGP"}' "{{customerToken}}") @("pm.test('201',()=>pm.response.to.have.status(201));"),
  withTest (req "Refund -> booking auto-CANCELLED" "POST" "{{baseUrl}}/api/payments/wallet/refund/{{bookingId2}}" '{"bookingId":"{{bookingId2}}","customerId":"{{customerId}}","tenantId":"{{tenantId}}"}' "{{customerToken}}") @("pm.test('200',()=>pm.response.to.have.status(200));pm.test('REFUNDED',()=>pm.expect(pm.response.json().status).to.eql('REFUNDED'));"),
  withTest (@{name="Booking 2 Status -> CANCELLED";request=@{method="GET";header=@(@{key="Authorization";value="Bearer {{customerToken}}"});url=@{raw="{{baseUrl}}/api/bookings/{{bookingId2}}/status?tenantId={{tenantId}}"}}} ) @("pm.test('CANCELLED',()=>pm.expect(pm.response.json().status).to.eql('CANCELLED'));")
)

$dashItems = @(
  withTest (@{name="Owner Dashboard";request=@{method="GET";header=@(@{key="Authorization";value="Bearer {{ownerToken}}"});url=@{raw="{{baseUrl}}/api/owner/dashboard"}}} ) @("pm.test('200',()=>pm.response.to.have.status(200));"),
  withTest (@{name="Owner Revenue";request=@{method="GET";header=@(@{key="Authorization";value="Bearer {{ownerToken}}"});url=@{raw="{{baseUrl}}/api/owner/revenue"}}} ) @("pm.test('200',()=>pm.response.to.have.status(200));"),
  withTest (@{name="Tenant Balance (Owner)";request=@{method="GET";header=@(@{key="Authorization";value="Bearer {{ownerToken}}"});url=@{raw="{{baseUrl}}/api/payments/tenant/balance?tenantId={{tenantId}}"}}} ) @("pm.test('200',()=>pm.response.to.have.status(200));"),
  withTest (@{name="List Admins (Owner)";request=@{method="GET";header=@(@{key="Authorization";value="Bearer {{ownerToken}}"});url=@{raw="{{baseUrl}}/api/owner/admins"}}} ) @("pm.test('200',()=>pm.response.to.have.status(200));")
)

$superItems = @(
  withTest (@{name="Platform Stats";request=@{method="GET";header=@(@{key="Authorization";value="Bearer {{superAdminToken}}"});url=@{raw="{{baseUrl}}/api/admin/super/stats"}}} ) @("pm.test('200',()=>pm.response.to.have.status(200));"),
  withTest (@{name="List All Tenants";request=@{method="GET";header=@(@{key="Authorization";value="Bearer {{superAdminToken}}"});url=@{raw="{{baseUrl}}/api/admin/super/tenants?page=0&size=20"}}} ) @("pm.test('200',()=>pm.response.to.have.status(200));"),
  withTest (@{name="Get Tenant Detail";request=@{method="GET";header=@(@{key="Authorization";value="Bearer {{superAdminToken}}"});url=@{raw="{{baseUrl}}/api/admin/super/tenants/{{tenantId}}"}}} ) @("pm.test('200',()=>pm.response.to.have.status(200));"),
  withTest (@{name="List Customers";request=@{method="GET";header=@(@{key="Authorization";value="Bearer {{superAdminToken}}"});url=@{raw="{{baseUrl}}/api/admin/super/customers?page=0&size=20"}}} ) @("pm.test('200',()=>pm.response.to.have.status(200));"),
  withTest (@{name="Stuck Bookings";request=@{method="GET";header=@(@{key="Authorization";value="Bearer {{superAdminToken}}"});url=@{raw="{{baseUrl}}/api/admin/super/bookings/stuck?thresholdMinutes=30&page=0&size=20"}}} ) @("pm.test('200',()=>pm.response.to.have.status(200));"),
  withTest (@{name="Failed Payments";request=@{method="GET";header=@(@{key="Authorization";value="Bearer {{superAdminToken}}"});url=@{raw="{{baseUrl}}/api/admin/super/payments/failed?page=0&size=20"}}} ) @("pm.test('200',()=>pm.response.to.have.status(200));"),
  withTest (@{name="All Transactions";request=@{method="GET";header=@(@{key="Authorization";value="Bearer {{superAdminToken}}"});url=@{raw="{{baseUrl}}/api/admin/super/transactions?page=0&size=20"}}} ) @("pm.test('200',()=>pm.response.to.have.status(200));"),
  withTest (req "Deactivate Tenant" "PATCH" "{{baseUrl}}/api/admin/super/tenants/{{tenantId}}/status" '{"status":"INACTIVE"}' "{{superAdminToken}}") @("pm.test('200',()=>pm.response.to.have.status(200));"),
  withTest (req "Re-Activate Tenant" "PATCH" "{{baseUrl}}/api/admin/super/tenants/{{tenantId}}/status" '{"status":"ACTIVE"}' "{{superAdminToken}}") @("pm.test('200',()=>pm.response.to.have.status(200));")
)

$col = @{
  info = @{
    name = "BookaBeeka E2E v3"
    "_postman_id" = "bookabeeka-e2e-v3"
    description = "Full E2E test suite. Run folders 01-10 in order. Credentials: owner@grandhotel.com/Owner@2024, admin@grandhotel.com/Admin@2024, customer@test.com/Customer@2024, superadmin@bookabeeka.com/superadmin123"
    schema = "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  }
  variable = @(
    @{key="baseUrl";value="http://localhost:8080"},
    @{key="tenantId";value=""},@{key="ownerToken";value=""},@{key="adminToken";value=""},
    @{key="customerToken";value=""},@{key="superAdminToken";value=""},@{key="customerId";value=""},
    @{key="roomTypeId";value=""},@{key="roomId";value=""},@{key="serviceId";value=""},
    @{key="bookingId";value=""},@{key="bookingId2";value=""},@{key="paymentId";value=""},
    @{key="reviewId";value=""},@{key="totalAmount";value="3400"}
  )
  item = @(
    @{name="01 — Auth and Setup";item=$authItems},
    @{name="02 — Tenant";item=$tenantItems},
    @{name="03 — Inventory Setup";item=$invItems},
    @{name="04 — Availability";item=$avItems},
    @{name="05 — Booking";item=$bookItems},
    @{name="06 — Payment";item=$payItems},
    @{name="07 — Complete Booking and Review";item=$reviewItems},
    @{name="08 — Refund Flow";item=$refundItems},
    @{name="09 — Owner Dashboard";item=$dashItems},
    @{name="10 — SuperAdmin";item=$superItems}
  )
}

$col | ConvertTo-Json -Depth 25 | Set-Content "d:\BookaBeeka\BookaBeeka_E2E.postman_collection.json" -Encoding UTF8
$total = ($col.item | ForEach-Object { $_.item.Count } | Measure-Object -Sum).Sum
Write-Host "Done. Total requests: $total"
