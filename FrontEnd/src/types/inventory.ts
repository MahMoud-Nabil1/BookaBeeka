// ── Rooms ─────────────────────────────────────────────────────────────────────
// Frontend uses hotel terminology (Room), backend API uses "Resource"

export interface RoomResponse {
  id: string;
  tenantId: string;
  branchId: string;
  name: string;               // e.g., "Room 101", "Deluxe Suite 205"
  roomCategory: string;       // Backend: resourceType - e.g., "STANDARD", "DELUXE", "SUITE"
  capacity: number;           // Number of guests
  specs: Record<string, unknown> | null;  // Amenities, bed type, etc.
  isActive: boolean;
  isBookable: boolean;
  createdAt: string;
}

export interface CreateRoomRequest {
  tenantId: string;
  branchId: string;
  name: string;
  roomCategory: string;       // Maps to backend: resourceType
  capacity?: number;
  specs?: Record<string, unknown>;
}

export interface UpdateRoomRequest {
  name?: string;
  roomCategory?: string;      // Maps to backend: resourceType
  capacity?: number;
  specs?: Record<string, unknown>;
  isActive?: boolean;
  isBookable?: boolean;
}

// ── Room Types ────────────────────────────────────────────────────────────────
// Frontend uses hotel terminology (RoomType), backend API uses "ServiceOffering"

export interface RoomTypeResponse {
  id: string;
  tenantId: string;
  branchId: string;
  name: string;               // e.g., "Standard Room", "Deluxe King", "Presidential Suite"
  price: number;              // Price per night
  durationMinutes: number;    // Backend field - typically 1440 for 24 hours
  bufferMinutes: number;      // Backend field - cleaning/turnover time
  customAttributes: Record<string, unknown> | null;  // Room features, descriptions
  isActive: boolean;
  createdAt: string;
}

export interface CreateRoomTypeRequest {
  tenantId: string;
  branchId: string;
  name: string;
  price: number;
  durationMinutes: number;
  bufferMinutes?: number;
  customAttributes?: Record<string, unknown>;
}

export interface UpdateRoomTypeRequest {
  name?: string;
  price?: number;
  durationMinutes?: number;
  bufferMinutes?: number;
  customAttributes?: Record<string, unknown>;
  isActive?: boolean;
}
