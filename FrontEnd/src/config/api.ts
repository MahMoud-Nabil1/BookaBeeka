import axios from 'axios';
import { store } from '../redux';
import { logout } from '../redux/slices/authSlice';

// Create base instance
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
});

// Request interceptor: add JWT token
api.interceptors.request.use((config) => {
  const token = store.getState().auth.token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor: handle 401 Unauthorized
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const userType = store.getState().auth.userType;
      store.dispatch(logout());
      
      // Redirect to correct login page based on previous user type
      if (userType === 'STAFF') {
        window.location.href = '/login/staff';
      } else {
        window.location.href = '/login/customer';
      }
    }
    return Promise.reject(error);
  }
);

export default api;

