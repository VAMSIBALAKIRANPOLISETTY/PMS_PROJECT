import axios from "axios";

export const api = axios.create({ baseURL: "/api" });

export function authHeaders(token: string) {
  return { Authorization: `Bearer ${token}` };
}
