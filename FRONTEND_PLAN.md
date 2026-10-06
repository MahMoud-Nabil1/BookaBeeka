# BookaBeeka Frontend — Full Implementation Plan

> Stack conventions: React Router v7 · Redux Toolkit (auth/ui) · TanStack Query v5 · shadcn/ui + Radix + Tailwind · react-hook-form + zod · axios (shared `api` instance) · lucide-react icons · sonner toasts

---

## Overview of Gaps

| Priority | Area                        | Missing                                  |
| -------- | --------------------------- | ---------------------------------------- |
| CRITICAL | Booking Completion          | Admin "Complete" action                  |
| CRITICAL | Wallet Checkout             | Payment actually wired to booking confirm|
| CRITICAL | Inventory link/unlink bug   | Query param vs path variable mismatch    |
| HIGH     | Room Blocks                 | Full CRUD admin UI                       |
| HIGH     | Amenity Management          | Full CRUD + room linking                 |
| HIGH     | Media / Photos              | Upload, list, delete, reorder            |
| HIGH     | Notifications               | Bell icon + notification center          |
| MEDIUM   | Owner Module                | Register, admins, dashboard, revenue     |
| MEDIUM   | Admin/Owner Profiles        | View + edit own profile                  |
| MEDIUM   | Tenant Settings             | Hotel info edit                          |
| MEDIUM   | OTP Flow                    | Request + verify screens                 |
| MEDIUM   | Room Reviews Display        | Show reviews on RoomDetailPage           |
| MEDIUM   | Refund UI                   | Trigger refund from booking detail       |
| LOW      | Payment Summary             | Per-booking payment summary              |
| LOW      | Super Admin Portal          | Full platform management portal          |

---

## Phase 1 — Bug Fixes & Critical Wiring (no new pages)

### 1.1  Fix link-service / unlink-service URL mismatch

File: FrontEnd/src/features/catalog/api/inventoryApi.ts

The backend uses path variables; the frontend sends query params — both linkRoomType and
unlinkRoomType currently return 404.

BEFORE (broken):
  linkRoomType:   POST /api/inventory/resources/{id}/link-service?serviceOfferingId=xxx
  unlinkRoomType: DELETE /api/inventory/resources/{id}/unlink-service?serviceOfferingId=xxx

AFTER (matches InventoryController):
  linkRoomType:   POST /api/inventory/resources/{roomId}/link-service/{serviceOfferingId}
  unlinkRoomType: DELETE /api/inventory/resources/{roomId}/unlink-service/{serviceOfferingId}

Keep tenantId as a query param on both calls — that part is correct.


### 1.2  Wire Wallet Checkout into Booking Confirm Flow

File: FrontEnd/src/features/bookings/hooks/useCreateBooking.ts

Current flow calls POST /api/bookings/{id}/confirm but never calls the payment endpoint,
so no money is actually deducted.

Required flow:
  1. createBooking()           → returns bookingId
  2. paymentApi.checkout()     → PaymentRequest { bookingId, customerId, tenantId, paymentAmount }
  3. bookingApi.confirmBooking() → finalise booking
  4. On checkout failure: call bookingApi.cancelBooking() to roll back
  5. On success: invalidate ['bookings','mine'] and ['wallet','balance', customerId]

No new types needed — PaymentRequest already exists in src/types/payment.ts.


### 1.3  Add "Complete Booking" Action for Staff

File: FrontEnd/src/features/bookings/api/bookingApi.ts
  Add: completeBooking(bookingId, tenantId)
       POST /api/bookings/{bookingId}/complete?tenantId=

File: FrontEnd/src/features/staff-dashboard/shared/hooks/useStaffBookings.ts
  Add: useCompleteBooking()
       useMutation → calls bookingApi.completeBooking
       onSuccess: toast.success + invalidate ['staff','bookings']
       onError:   toast.error with backend message

UI changes:
  - AdminBookingsPage.tsx:        add "Complete" button on CONFIRMED bookings
  - ReceptionistBookingsPage.tsx: same
  - Both use a confirm Dialog (same pattern as the cancel dialog)

---

## Phase 2 — Room Block Management

Who uses it: ADMIN and OWNER roles (staff dashboard).

### 2.1  New types

File: FrontEnd/src/types/availability.ts  (append to existing file)

  RoomBlockResponse {
    id, tenantId, roomId, startDate (ISO date), endDate (ISO date), reason | null, createdAt
  }
  CreateRoomBlockRequest { roomId, startDate, endDate, reason? }
  UpdateRoomBlockRequest { startDate?, endDate?, reason? }


### 2.2  New API file

File: FrontEnd/src/features/catalog/api/roomBlockApi.ts

  roomBlockApi.createRoomBlock(req)          POST /api/availability/room-blocks
  roomBlockApi.listRoomBlocks()              GET  /api/availability/room-blocks
  roomBlockApi.getRoomBlock(id)              GET  /api/availability/room-blocks/{id}
  roomBlockApi.updateRoomBlock(id, req)      PUT  /api/availability/room-blocks/{id}
  roomBlockApi.deleteRoomBlock(id)           DELETE /api/availability/room-blocks/{id}

All calls are authenticated via the shared axios instance (JWT injected automatically).


### 2.3  New hook file

File: FrontEnd/src/features/catalog/hooks/useRoomBlocks.ts

  useRoomBlocks()        useQuery(['room-blocks'])
  useCreateRoomBlock()   useMutation + invalidate ['room-blocks']
  useUpdateRoomBlock()   useMutation + invalidate ['room-blocks']
  useDeleteRoomBlock()   useMutation + invalidate ['room-blocks']


### 2.4  New page

File: FrontEnd/src/features/staff-dashboard/admin/pages/AdminRoomBlocksPage.tsx

  Wrap in <PageLayout title="Room Blocks">
  - "Block a Room" button → opens CreateRoomBlockModal
  - shadcn Table: columns = Room, Start Date, End Date, Reason, Actions (Edit / Delete)
  - Edit action → opens EditRoomBlockModal
  - Delete action → inline confirm Popover

New components:
  features/staff-dashboard/admin/components/CreateRoomBlockModal.tsx
    - react-hook-form + zod
    - Room selector dropdown (populated from useRooms())
    - Start/end date pickers (shadcn Calendar)
    - Optional reason Textarea

  features/staff-dashboard/admin/components/EditRoomBlockModal.tsx
    - Same fields, pre-filled from existing block data


### 2.5  Register route

File: FrontEnd/src/routes/AppRouter.tsx
  Add inside /staff/admin section:
    <Route path="room-blocks" element={<AdminRoomBlocksPage />} />

File: FrontEnd/src/features/staff-dashboard/admin/pages/AdminDashboardLayout.tsx
  Add "Room Blocks" nav item to sidebar (icon: CalendarX2 from lucide-react)

---

## Phase 3 — Amenity Management

Who uses it: ADMIN and OWNER roles.

### 3.1  New types

File: FrontEnd/src/types/inventory.ts  (append to existing file)

  AmenityResponse      { id, tenantId, name, icon? | null, description? | null }
  CreateAmenityRequest { name, icon?, description? }
  UpdateAmenityRequest { name?, icon?, description? }


### 3.2  New API file

File: FrontEnd/src/features/catalog/api/amenityApi.ts

  amenityApi.listAmenities()                           GET  /api/inventory/amenities
  amenityApi.getAmenity(id)                            GET  /api/inventory/amenities/{id}
  amenityApi.createAmenity(req)                        POST /api/inventory/amenities
  amenityApi.updateAmenity(id, req)                    PUT  /api/inventory/amenities/{id}
  amenityApi.deleteAmenity(id)                         DELETE /api/inventory/amenities/{id}
  amenityApi.linkAmenityToRoom(roomId, amenityId)      POST /api/inventory/resources/{roomId}/amenities/{amenityId}
  amenityApi.unlinkAmenityFromRoom(roomId, amenityId)  DELETE /api/inventory/resources/{roomId}/amenities/{amenityId}
  amenityApi.listRoomAmenities(roomId)                 GET  /api/inventory/resources/{roomId}/amenities


### 3.3  New hook file

File: FrontEnd/src/features/catalog/hooks/useAmenities.ts

  useAmenities()                 useQuery(['amenities'])
  useRoomAmenities(roomId)       useQuery(['amenities','room',roomId])
  useCreateAmenity()             useMutation + invalidate ['amenities']
  useUpdateAmenity()             useMutation + invalidate ['amenities']
  useDeleteAmenity()             useMutation + invalidate ['amenities']
  useLinkAmenity()               useMutation + invalidate ['amenities','room',roomId]
  useUnlinkAmenity()             useMutation + invalidate ['amenities','room',roomId]


### 3.4  New page

File: FrontEnd/src/features/staff-dashboard/admin/pages/AdminAmenitiesPage.tsx

  Wrap in <PageLayout title="Amenities">
  - "Add Amenity" button → CreateAmenityModal
  - Grid or Table: name, icon, description, Edit / Delete actions
  - Delete uses inline confirm Popover


### 3.5  Extend room edit with amenity assignment

Wherever the room edit form/modal lives in admin pages, add:
  - A checklist or multi-select of all amenities
  - Pre-loaded with useRoomAmenities(roomId)
  - Toggling an amenity calls linkAmenityToRoom or unlinkAmenityFromRoom


### 3.6  Display amenities in customer-facing view

File: FrontEnd/src/features/catalog/pages/RoomDetailPage.tsx
  - Load with useRoomAmenities(roomId)
  - Render as pill badges (shadcn Badge) below the room description


### 3.7  Register route

File: FrontEnd/src/routes/AppRouter.tsx
  Add: <Route path="amenities" element={<AdminAmenitiesPage />} />

File: FrontEnd/src/features/staff-dashboard/admin/pages/AdminDashboardLayout.tsx
  Add "Amenities" nav item (icon: Tag from lucide-react)

---

## Phase 4 — Media / Photo Management

### 4.1  Update types

File: FrontEnd/src/types/media.ts  (replace the existing stub)

  MediaPhotoResponse {
    id, tenantId, resourceId, url, publicId (Cloudinary), isPrimary, sortOrder, createdAt
  }
  AttachPhotoRequest     { url, publicId, isPrimary? }
  UploadSignatureResponse { signature, timestamp, cloudName, apiKey, folder }
  ReorderPhotosRequest   { photoIds: string[] }  // ordered list


### 4.2  New API file

File: FrontEnd/src/features/catalog/api/mediaApi.ts

  mediaApi.getUploadSignature(resourceId)          POST /api/media/resources/{resourceId}/upload-signature
  mediaApi.attachPhoto(resourceId, req)             POST /api/media/resources/{resourceId}/photos
  mediaApi.listPhotos(resourceId)                   GET  /api/media/resources/{resourceId}/photos
  mediaApi.deletePhoto(photoId)                     DELETE /api/media/photos/{photoId}
  mediaApi.setPrimaryPhoto(photoId)                 PATCH /api/media/photos/{photoId}/primary
  mediaApi.reorderPhotos(resourceId, req)           PATCH /api/media/resources/{resourceId}/photos/reorder


### 4.3  New hook file

File: FrontEnd/src/features/catalog/hooks/useMedia.ts

  useRoomPhotos(resourceId)    useQuery(['photos', resourceId])
  useUploadPhoto(resourceId)   handles the 3-step Cloudinary flow:
                                 1. getUploadSignature(resourceId) → signed params
                                 2. POST directly to https://api.cloudinary.com/v1_1/{cloudName}/image/upload
                                    with FormData (file + signature + timestamp + apiKey)
                                 3. attachPhoto(resourceId, { url, publicId })
                                 4. invalidate ['photos', resourceId]
  useDeletePhoto()             useMutation + invalidate
  useSetPrimaryPhoto()         useMutation + invalidate
  useReorderPhotos()           useMutation + invalidate


### 4.4  New component (staff side)

File: FrontEnd/src/features/staff-dashboard/admin/components/RoomPhotoManager.tsx

  Renders inside the room detail/edit view:
  - Photo grid with drag-to-reorder (up/down buttons if @dnd-kit not available)
  - Upload zone: shadcn <Input type="file" accept="image/*">
  - Per-photo actions: star button (Set Primary), trash button (Delete)
  - Primary photo shown with a "Primary" Badge overlay


### 4.5  Display photos in customer-facing view

File: FrontEnd/src/features/catalog/pages/RoomDetailPage.tsx
  - Load with useRoomPhotos(roomId)
  - Render as a scrollable image row / simple carousel at the top of the page

---

## Phase 5 — Notifications

### 5.1  New types file

File: FrontEnd/src/types/notification.ts  (new file)

  NotificationType = 'BOOKING_CONFIRMED' | 'BOOKING_CANCELLED' | 'PAYMENT_RECEIVED'
                   | 'BOOKING_REMINDER' | 'REVIEW_REPLY'

  NotificationDto {
    id, tenantId, customerId, type: NotificationType,
    title, message, isRead: boolean, createdAt
  }


### 5.2  New API file

File: FrontEnd/src/features/notifications/api/notificationApi.ts

  notificationApi.getMyNotifications(tenantId)
    GET /api/notifications/my-notifications?tenantId=

  notificationApi.getCustomerNotifications(customerId, tenantId)
    GET /api/notifications/customer/{customerId}?tenantId=

  notificationApi.getNotification(id)
    GET /api/notifications/{id}


### 5.3  New hook file

File: FrontEnd/src/features/notifications/hooks/useNotifications.ts

  useMyNotifications(tenantId)
    useQuery(['notifications', tenantId])
    refetchInterval: 60_000   (poll every 60 seconds)
    returns { data, unreadCount: data?.filter(n => !n.isRead).length ?? 0 }

  useCustomerNotifications(customerId, tenantId)   // staff view only


### 5.4  New component

File: FrontEnd/src/features/notifications/components/NotificationBell.tsx

  - Bell icon (lucide-react Bell) with a red badge showing unreadCount
  - Click opens a shadcn Popover
  - Inside: scrollable list of notification items
  - Each item: title, message, relative time (date-fns formatDistanceToNow)
  - Unread items: highlighted background (bg-muted or similar)


### 5.5  Integrate into Navbar

File: FrontEnd/src/components/layout/Navbar.tsx
  - Import and render <NotificationBell /> between nav links and avatar dropdown
  - Only render when: isAuthenticated && userType === 'CUSTOMER'

---

## Phase 6 — Owner Module

Who uses it: OWNER role only.

### 6.1  New types file

File: FrontEnd/src/types/owner.ts  (new file)

  RegisterOwnerRequest { businessName, ownerName, email, password, phone }
  AppointAdminRequest  { name, email, password }
  AdminDto             { id, name, email, createdAt }
  OwnerDashboardDto    { totalBookings, activeRooms, revenue, recentActivity }
  OwnerRevenueDto      { totalRevenue, currency, breakdown: RevenueBreakdown[] }
  RevenueBreakdown     { label, amount, currency }


### 6.2  New API file

File: FrontEnd/src/features/owner/api/ownerApi.ts

  ownerApi.registerOwner(req)    POST /api/v1/owner/register
  ownerApi.appointAdmin(req)     POST /api/v1/owner/admins
  ownerApi.listAdmins()          GET  /api/v1/owner/admins
  ownerApi.getDashboard()        GET  /api/v1/owner/dashboard
  ownerApi.getRevenue()          GET  /api/v1/owner/revenue


### 6.3  New hook file

File: FrontEnd/src/features/owner/hooks/useOwner.ts

  useOwnerDashboard()   useQuery(['owner','dashboard'])
  useOwnerRevenue()     useQuery(['owner','revenue'])
  useAdminList()        useQuery(['owner','admins'])
  useAppointAdmin()     useMutation + invalidate ['owner','admins']
  useRegisterOwner()    useMutation (public — no auth needed)


### 6.4  Owner Registration Page

File: FrontEnd/src/features/owner/pages/OwnerRegisterPage.tsx

  react-hook-form + zod form with fields:
    businessName, ownerName, email, password, confirmPassword, phone
  On success: toast.success + redirect to /login/staff

Register in AppRouter.tsx inside <GuestOnly>:
  <Route path="/register/owner" element={<OwnerRegisterPage />} />

Add "Register as Hotel Owner" link on StaffLoginPage.tsx


### 6.5  Owner-specific sections in AdminOverviewPage

File: FrontEnd/src/features/staff-dashboard/admin/pages/AdminOverviewPage.tsx

  When role === 'OWNER', conditionally render two additional sections:

  Revenue Summary Card
    - Uses useOwnerRevenue()
    - Shows totalRevenue with currency, breakdown list

  My Admins Section
    - Uses useAdminList()
    - Table: name, email, createdAt
    - "Appoint New Admin" button → AppointAdminModal

New component:
File: FrontEnd/src/features/staff-dashboard/admin/components/AppointAdminModal.tsx
  - react-hook-form + zod
  - Fields: name, email, password
  - Calls useAppointAdmin()

---

## Phase 7 — Admin & Owner Profile Pages

### 7.1  New API file

File: FrontEnd/src/features/staff-dashboard/api/staffProfileApi.ts

  staffProfileApi.getAdminProfile()         GET /api/v1/admin/profile
  staffProfileApi.updateAdminProfile(req)   PUT /api/v1/admin/profile


### 7.2  Extend existing types

File: FrontEnd/src/types/auth.ts  (append to existing file)

  AdminProfileDto {
    id, name, email, phone?, tenantId, role: StaffRole
  }
  UpdateAdminProfileRequest { name?, phone?, password? }


### 7.3  New page

File: FrontEnd/src/features/staff-dashboard/admin/pages/AdminProfilePage.tsx

  Layout: <PageLayout title="My Profile">
  Sections:
  - Display card: name, email, phone, role (read-only)
  - "Edit" button toggles the card into an edit form
  - Separate "Change Password" form at the bottom


### 7.4  Register route

File: FrontEnd/src/routes/AppRouter.tsx
  Add inside /staff/admin:
    <Route path="profile" element={<AdminProfilePage />} />

File: FrontEnd/src/features/staff-dashboard/admin/pages/AdminDashboardLayout.tsx
  Add "Profile" nav item (icon: UserCircle from lucide-react)

---

## Phase 8 — Tenant Settings

Who uses it: OWNER role only.

### 8.1  New types file

File: FrontEnd/src/types/tenant.ts  (new file)

  TenantDto {
    id, name, subdomain, email?, phone?, address?, logoUrl?,
    status: 'ACTIVE' | 'SUSPENDED' | 'INACTIVE'
  }
  UpdateTenantRequest { name?, email?, phone?, address? }


### 8.2  New API file

File: FrontEnd/src/features/tenants/api/tenantApi.ts

  tenantApi.getMyTenant()          GET /api/tenants/me
  tenantApi.updateMyTenant(req)    PUT /api/tenants/me


### 8.3  New page

File: FrontEnd/src/features/staff-dashboard/admin/pages/AdminTenantSettingsPage.tsx

  Layout: <PageLayout title="Hotel Settings">
  Sections:
  - Hotel Info form: name, email, phone, address (editable)
  - Subdomain & Status: read-only display
  Guard: only render if role === 'OWNER' (admins cannot edit hotel-level settings)


### 8.4  Register route

File: FrontEnd/src/routes/AppRouter.tsx
  Add inside /staff/admin:
    <Route path="settings" element={<AdminTenantSettingsPage />} />

File: FrontEnd/src/features/staff-dashboard/admin/pages/AdminDashboardLayout.tsx
  Add "Settings" nav item (icon: Settings from lucide-react), visible only when role === 'OWNER'

---

## Phase 9 — OTP Login Flow

### 9.1  Extend existing API file

File: FrontEnd/src/features/auth/api/authApi.ts  (append to existing file)

  requestOtp(email)               POST /api/auth/otp/request
  verifyOtp(email, code)          POST /api/auth/otp/verify  → returns { token: string }


### 9.2  New hook file

File: FrontEnd/src/features/auth/hooks/useOtp.ts

  useRequestOtp()   useMutation → calls authApi.requestOtp
                    onSuccess: toast.success('OTP sent to your email')

  useVerifyOtp()    useMutation → calls authApi.verifyOtp
                    onSuccess: dispatch(loginSuccess(token)) then navigate to portal


### 9.3  New page

File: FrontEnd/src/features/auth/pages/OtpLoginPage.tsx

  Two-step flow on a single page:
  Step 1: Email input + "Send OTP" button (uses useRequestOtp)
  Step 2: 6-digit code input rendered after step 1 succeeds (uses useVerifyOtp)
  On verify success: loginSuccess dispatched, redirect to /portal


### 9.4  Register route

File: FrontEnd/src/routes/AppRouter.tsx
  Add inside <GuestOnly>:
    <Route path="/login/otp" element={<OtpLoginPage />} />

File: FrontEnd/src/features/customer-portal/pages/CustomerLoginPage.tsx
  Add "Sign in with OTP" link pointing to /login/otp

---

## Phase 10 — Reviews Display on Room Detail Page

All the pieces already exist. This is wiring only.

File: FrontEnd/src/features/catalog/pages/RoomDetailPage.tsx

  1. Import ProductReviewsSection from '../../reviews'
  2. After the room info section, add:
       <ProductReviewsSection serviceId={room.id} tenantId={room.tenantId} />

ProductReviewsSection internally uses useServiceReviews(serviceId, tenantId, page, size)
and already renders ReviewSummary (average + star distribution) + paginated ReviewCard list.

---

## Phase 11 — Refund UI

### 11.1  Add hook

File: FrontEnd/src/features/billing/hooks/useWallet.ts  (append)

  useRefundBooking()
    useMutation → paymentApi.refund(bookingId, RefundRequest)
    onSuccess: toast.success + invalidate ['wallet','balance']
    onError:   toast.error with backend message


### 11.2  Add UI to BookingDetailPage

File: FrontEnd/src/features/bookings/pages/BookingDetailPage.tsx

  - Add "Request Refund" button
  - Only visible when: booking.status === 'CANCELLED' && booking.totalAmount > 0
  - Wrapped in a Dialog showing the refund amount before confirming
  - Calls useRefundBooking() on confirm

---

## Phase 12 — Payment Summary per Booking

### 12.1  Add type

File: FrontEnd/src/types/payment.ts  (append)

  BookingPaymentSummary {
    bookingId, amount, currency, status: 'PENDING'|'COMPLETED'|'FAILED'|'REFUNDED',
    paymentMethod, createdAt, updatedAt
  }


### 12.2  Add API call

File: FrontEnd/src/features/billing/api/paymentApi.ts  (append)

  getBookingPaymentSummary(bookingId)
    GET /api/payments/booking/{bookingId}/summary


### 12.3  Add to BookingDetailPage

File: FrontEnd/src/features/bookings/pages/BookingDetailPage.tsx

  - useQuery to fetch BookingPaymentSummary by bookingId
  - Render a "Payment" Card section below the booking status card
  - Shows: amount, currency, method, status (PaymentStatusBadge), transaction date

---

## Phase 13 — Super Admin Portal

Who uses it: SUPER_ADMIN role only.
New portal at /superadmin/* — completely separate from the staff admin portal.

### 13.1  New types file

File: FrontEnd/src/types/superAdmin.ts  (new file)

  PlatformStats     { totalTenants, activeTenants, totalCustomers, totalBookings, totalRevenue }
  TenantSummary     { id, name, subdomain, status, createdAt }
  TenantDetail      extends TenantSummary with { email, phone, bookingCount, revenue }
  CustomerSummary   { id, email, name, isBanned, createdAt }
  TransactionSummary { id, type, amount, currency, createdAt }
  FailedPayment     { paymentId, bookingId, amount, failedAt, reason }


### 13.2  New API file

File: FrontEnd/src/features/super-admin/api/superAdminApi.ts

  superAdminApi.getStats()                          GET  /api/admin/super/stats
  superAdminApi.listTenants(page, size)             GET  /api/admin/super/tenants?page=&size=
  superAdminApi.getTenant(id)                       GET  /api/admin/super/tenants/{id}
  superAdminApi.updateTenantStatus(id, status)      PATCH /api/admin/super/tenants/{id}/status
  superAdminApi.listCustomers(page, size)           GET  /api/admin/super/customers?page=&size=
  superAdminApi.banCustomer(id)                     PATCH /api/admin/super/customers/{id}/ban
  superAdminApi.unbanCustomer(id)                   PATCH /api/admin/super/customers/{id}/unban
  superAdminApi.listTransactions(page, size)        GET  /api/admin/super/transactions?page=&size=
  superAdminApi.listFailedPayments(page, size)      GET  /api/admin/super/payments/failed?page=&size=
  superAdminApi.listStuckBookings()                 GET  /api/admin/super/bookings/stuck
  superAdminApi.listTenantWallets()                 GET  /api/admin/super/wallets/tenants
  superAdminApi.listCustomerWallets()               GET  /api/admin/super/wallets/customers


### 13.3  New hook file

File: FrontEnd/src/features/super-admin/hooks/useSuperAdmin.ts

  usePlatformStats()
  useTenantList(page, size)
  useTenantDetail(id)
  useUpdateTenantStatus()
  useCustomerList(page, size)
  useBanCustomer()
  useUnbanCustomer()
  useTransactionList(page, size)
  useFailedPayments(page, size)
  useStuckBookings()
  useTenantWallets()
  useCustomerWallets()


### 13.4  New pages

File: FrontEnd/src/features/super-admin/pages/SuperAdminLayout.tsx
  - Sidebar + Outlet (separate brand color to distinguish from hotel admin)
  - Nav items: Overview, Tenants, Customers, Transactions, Failed Payments, Stuck Bookings, Wallets

File: FrontEnd/src/features/super-admin/pages/SuperAdminOverviewPage.tsx
  - Stat cards: totalTenants, activeTenants, totalCustomers, totalBookings, totalRevenue

File: FrontEnd/src/features/super-admin/pages/SuperAdminTenantsPage.tsx
  - Paginated table: name, subdomain, status, createdAt
  - Row actions: View Detail, Change Status (ACTIVE / SUSPENDED / INACTIVE)

File: FrontEnd/src/features/super-admin/pages/SuperAdminCustomersPage.tsx
  - Paginated table: email, name, isBanned, createdAt
  - Row actions: Ban / Unban (toggled by isBanned)

File: FrontEnd/src/features/super-admin/pages/SuperAdminTransactionsPage.tsx
  - Paginated table: id (short), type, amount, currency, createdAt

File: FrontEnd/src/features/super-admin/pages/SuperAdminFailedPaymentsPage.tsx
  - Paginated table: bookingId, amount, failedAt, reason

File: FrontEnd/src/features/super-admin/pages/SuperAdminStuckBookingsPage.tsx
  - Table of stuck bookings (no pagination — backend returns list)
  - Same columns as AdminBookingsPage

File: FrontEnd/src/features/super-admin/pages/SuperAdminWalletsPage.tsx
  - Two tabs: Tenant Wallets / Customer Wallets
  - Each tab is a table with balance + currency


### 13.5  Register routes

File: FrontEnd/src/routes/AppRouter.tsx

  <Route path="/superadmin" element={
    <RequireAuth allowedUserType="STAFF">
      <RequireRole allowedRoles={['SUPER_ADMIN']}>
        <Outlet />
      </RequireRole>
    </RequireAuth>
  }>
    <Route element={<SuperAdminLayout />}>
      <Route index element={<Navigate to="/superadmin/overview" replace />} />
      <Route path="overview"     element={<SuperAdminOverviewPage />} />
      <Route path="tenants"      element={<SuperAdminTenantsPage />} />
      <Route path="customers"    element={<SuperAdminCustomersPage />} />
      <Route path="transactions" element={<SuperAdminTransactionsPage />} />
      <Route path="payments"     element={<SuperAdminFailedPaymentsPage />} />
      <Route path="bookings"     element={<SuperAdminStuckBookingsPage />} />
      <Route path="wallets"      element={<SuperAdminWalletsPage />} />
    </Route>
  </Route>

File: FrontEnd/src/routes/StaffRoleRedirect.tsx
  Update to handle: SUPER_ADMIN → /superadmin/overview

---

## Recommended Build Order

  Phase 1   →  Bug Fixes (1.1, 1.2, 1.3)          ~1 day
  Phase 10  →  Reviews Display (wire-up only)       ~0.5 day
  Phase 2   →  Room Blocks                          ~2 days
  Phase 3   →  Amenity Management                   ~2 days
  Phase 4   →  Media / Photos                       ~3 days
  Phase 5   →  Notifications                        ~1.5 days
  Phase 11  →  Refund UI                            ~0.5 day
  Phase 12  →  Payment Summary                      ~0.5 day
  Phase 6   →  Owner Module                         ~2 days
  Phase 7   →  Admin/Owner Profiles                 ~1 day
  Phase 8   →  Tenant Settings                      ~1 day
  Phase 9   →  OTP Flow                             ~1 day
  Phase 13  →  Super Admin Portal                   ~4 days

  Total estimate: ~20 developer days

---

## Files to Create

  src/types/notification.ts                                                   (Phase 5)
  src/types/owner.ts                                                          (Phase 6)
  src/types/tenant.ts                                                         (Phase 8)
  src/types/superAdmin.ts                                                     (Phase 13)
  src/features/catalog/api/roomBlockApi.ts                                    (Phase 2)
  src/features/catalog/api/amenityApi.ts                                      (Phase 3)
  src/features/catalog/api/mediaApi.ts                                        (Phase 4)
  src/features/catalog/hooks/useRoomBlocks.ts                                 (Phase 2)
  src/features/catalog/hooks/useAmenities.ts                                  (Phase 3)
  src/features/catalog/hooks/useMedia.ts                                      (Phase 4)
  src/features/notifications/api/notificationApi.ts                           (Phase 5)
  src/features/notifications/hooks/useNotifications.ts                        (Phase 5)
  src/features/notifications/components/NotificationBell.tsx                  (Phase 5)
  src/features/owner/api/ownerApi.ts                                          (Phase 6)
  src/features/owner/hooks/useOwner.ts                                        (Phase 6)
  src/features/owner/pages/OwnerRegisterPage.tsx                              (Phase 6)
  src/features/staff-dashboard/api/staffProfileApi.ts                         (Phase 7)
  src/features/staff-dashboard/admin/pages/AdminRoomBlocksPage.tsx            (Phase 2)
  src/features/staff-dashboard/admin/pages/AdminAmenitiesPage.tsx             (Phase 3)
  src/features/staff-dashboard/admin/pages/AdminProfilePage.tsx               (Phase 7)
  src/features/staff-dashboard/admin/pages/AdminTenantSettingsPage.tsx        (Phase 8)
  src/features/staff-dashboard/admin/components/CreateRoomBlockModal.tsx      (Phase 2)
  src/features/staff-dashboard/admin/components/EditRoomBlockModal.tsx        (Phase 2)
  src/features/staff-dashboard/admin/components/RoomPhotoManager.tsx          (Phase 4)
  src/features/staff-dashboard/admin/components/AppointAdminModal.tsx         (Phase 6)
  src/features/tenants/api/tenantApi.ts                                        (Phase 8)
  src/features/auth/pages/OtpLoginPage.tsx                                    (Phase 9)
  src/features/auth/hooks/useOtp.ts                                           (Phase 9)
  src/features/super-admin/api/superAdminApi.ts                               (Phase 13)
  src/features/super-admin/hooks/useSuperAdmin.ts                             (Phase 13)
  src/features/super-admin/pages/SuperAdminLayout.tsx                         (Phase 13)
  src/features/super-admin/pages/SuperAdminOverviewPage.tsx                   (Phase 13)
  src/features/super-admin/pages/SuperAdminTenantsPage.tsx                    (Phase 13)
  src/features/super-admin/pages/SuperAdminCustomersPage.tsx                  (Phase 13)
  src/features/super-admin/pages/SuperAdminTransactionsPage.tsx               (Phase 13)
  src/features/super-admin/pages/SuperAdminFailedPaymentsPage.tsx             (Phase 13)
  src/features/super-admin/pages/SuperAdminStuckBookingsPage.tsx              (Phase 13)
  src/features/super-admin/pages/SuperAdminWalletsPage.tsx                    (Phase 13)

---

## Files to Modify

  src/features/catalog/api/inventoryApi.ts                    Fix link/unlink URL         (Phase 1.1)
  src/features/bookings/hooks/useCreateBooking.ts             Wire checkout payment        (Phase 1.2)
  src/features/bookings/api/bookingApi.ts                     Add completeBooking          (Phase 1.3)
  src/features/staff-dashboard/shared/hooks/useStaffBookings.ts  Add useCompleteBooking   (Phase 1.3)
  src/features/staff-dashboard/admin/pages/AdminBookingsPage.tsx  Complete button          (Phase 1.3)
  src/features/staff-dashboard/receptionist/pages/ReceptionistBookingsPage.tsx  Complete  (Phase 1.3)
  src/features/billing/api/paymentApi.ts                      Add getBookingPaymentSummary (Phase 12)
  src/features/billing/hooks/useWallet.ts                     Add useRefundBooking         (Phase 11)
  src/features/bookings/pages/BookingDetailPage.tsx           Refund + payment summary     (Phase 11, 12)
  src/features/catalog/pages/RoomDetailPage.tsx               Reviews + photos + amenities (Phase 3, 4, 10)
  src/components/layout/Navbar.tsx                            Add NotificationBell         (Phase 5)
  src/features/auth/api/authApi.ts                            Add OTP endpoints            (Phase 9)
  src/features/staff-dashboard/admin/pages/AdminOverviewPage.tsx  Owner sections           (Phase 6)
  src/features/staff-dashboard/admin/pages/AdminDashboardLayout.tsx  New sidebar items     (Phase 2, 3, 7, 8)
  src/routes/AppRouter.tsx                                    All new routes               (all phases)
  src/routes/StaffRoleRedirect.tsx                            Handle SUPER_ADMIN           (Phase 13)
  src/types/availability.ts                                   Add RoomBlock types          (Phase 2)
  src/types/inventory.ts                                      Add Amenity types            (Phase 3)
  src/types/media.ts                                          Replace stub with full types (Phase 4)
  src/types/auth.ts                                           Add AdminProfileDto          (Phase 7)
  src/types/payment.ts                                        Add BookingPaymentSummary    (Phase 12)
