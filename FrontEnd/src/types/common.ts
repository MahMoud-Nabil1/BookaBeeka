// Standard Spring error response shape
export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}

// Spring Page<T> paginated response
export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;       // current page index (0-based)
  first: boolean;
  last: boolean;
  empty: boolean;
}
