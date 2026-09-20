const API_BASE_URL = "/api/auth"

export interface AuthResponse {
  token: string
  fullName: string
  email: string
}

interface ErrorResponse {
  error: string
}

export async function registerUser(
  fullName: string,
  email: string,
  password: string
): Promise<AuthResponse> {
  const res = await fetch(`${API_BASE_URL}/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ fullName, email, password }),
  })

  const data = await res.json()

  if (!res.ok) {
    throw new Error((data as ErrorResponse).error || "Registration failed")
  }

  return data as AuthResponse
}

export async function loginUser(
  email: string,
  password: string
): Promise<AuthResponse> {
  const res = await fetch(`${API_BASE_URL}/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password }),
  })

  const data = await res.json()

  if (!res.ok) {
    throw new Error((data as ErrorResponse).error || "Login failed")
  }

  return data as AuthResponse
}