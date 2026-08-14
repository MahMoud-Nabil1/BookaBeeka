// TODO: useCreateBooking hook
// Orchestrates the 3-step booking creation flow:
// 1. createBooking()  → PENDING_PAYMENT + lockId
// 2. checkout()       → wallet deducted
// 3. confirmBooking() → CONFIRMED
// Uses useMutation + invalidates bookings + balance queries on success

export {};
