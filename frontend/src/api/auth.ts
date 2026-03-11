import axios from 'axios';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
}

export async function loginLocal(data: LoginRequest) {
  return axios.post('/api/v1/auth/login', data);
}

export async function registerLocal(data: RegisterRequest) {
  return axios.post('/api/v1/auth/register', data);
}
