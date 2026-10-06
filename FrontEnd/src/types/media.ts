// Media module types for photo management with Cloudinary integration

export interface MediaPhotoResponse {
  id: string;
  tenantId: string;
  resourceId: string;
  url: string;
  publicId: string;  // Cloudinary public ID
  isPrimary: boolean;
  sortOrder: number;
  createdAt: string;
}

export interface AttachPhotoRequest {
  url: string;
  publicId: string;
  isPrimary?: boolean;
}

export interface UploadSignatureResponse {
  signature: string;
  timestamp: number;
  cloudName: string;
  apiKey: string;
  folder: string;
}

export interface ReorderPhotosRequest {
  photoIds: string[];  // Ordered list of photo IDs
}
