import { apiFetch } from "@/lib/api"

export type Language = "JAVA"

export interface SubmissionResponse {
  id: number
  language: Language
  code: string
  testCode?: string | null
  createdAt: string
}

export function createSubmission(language: Language, code: string, testCode?: string): Promise<SubmissionResponse> {
  return apiFetch<SubmissionResponse>("/submissions", {
    method: "POST",
    body: JSON.stringify({ language, code, testCode: testCode || null }),
  })
}

export function getSubmission(id: number): Promise<SubmissionResponse> {
  return apiFetch<SubmissionResponse>(`/submissions/${id}`)
}
export interface SubmissionSummary {
  id: number
  language: Language
  codePreview: string
  createdAt: string
  latestScore: number | null
  latestStatus: "PENDING" | "IN_PROGRESS" | "COMPLETED" | "FAILED" | null
}

export function listSubmissions(): Promise<SubmissionSummary[]> {
  return apiFetch<SubmissionSummary[]>("/submissions")
}