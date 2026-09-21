// Media module — REST endpoints not yet exposed by backend.
// Types defined here for future use.

export interface MediaPhoto {
  id: string;
  tenantId: string;
  entityType: string;   // e.g., 'ROOM' | 'ROOM_TYPE' (backend uses 'RESOURCE' | 'SERVICE')
  entityId: string;
  url: string;
  isPrimary: boolean;
  displayOrder: number;
}
