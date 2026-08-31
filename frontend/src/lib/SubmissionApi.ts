import { apiFetch } from "@/lib/api"

export type Language = "JAVA"

export interface SubmissionResponse {
  id: number
  language: Language
  code: string
  createdAt: string
}

export function createSubmission(language: Language, code: string): Promise<SubmissionResponse> {
  return apiFetch<SubmissionResponse>("/submissions", {
    method: "POST",
    body: JSON.stringify({ language, code }),
  })
}

export function getSubmission(id: number): Promise<SubmissionResponse> {
  return apiFetch<SubmissionResponse>(`/submissions/${id}`)
}