export interface CustomerRegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  password?: string;
  phone?: string;
}

export interface Customer {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  phone?: string;
  createdAt: string;
}

