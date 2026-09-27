import { apiFetch } from "@/lib/api"

export interface ExecuteResponse {
  output: string
  timedOut: boolean
  compileFailed: boolean
  exitCode: number | null
}

export function executeCode(code: string): Promise<ExecuteResponse> {
  return apiFetch<ExecuteResponse>("/execute", {
    method: "POST",
    body: JSON.stringify({ code }),
  })
}