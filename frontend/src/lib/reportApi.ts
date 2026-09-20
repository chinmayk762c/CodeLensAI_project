import { apiFetch } from "@/lib/api"

export interface IssueResponse {
  id: number
  severity: "LOW" | "MEDIUM" | "HIGH" | "CRITICAL"
  category: "BUG" | "CODE_SMELL" | "SECURITY" | "PERFORMANCE" | "STYLE"
  description: string
  lineNumber: number | null
  suggestion: string | null
}

export interface AnalysisReportResponse {
  id: number
  submissionId: number
  status: "PENDING" | "IN_PROGRESS" | "COMPLETED" | "FAILED"
  overallScore: number | null
  qualityGatePassed: boolean
  coveragePercentage: number | null
  aiSummary: string | null
  optimizedCode: string | null
  createdAt: string
  issues: IssueResponse[]
}

export interface DocstringResponse {
  submissionId: number
  documentedCode: string
}

export function getReport(submissionId: number): Promise<AnalysisReportResponse> {
  return apiFetch<AnalysisReportResponse>(`/submissions/${submissionId}/report`)
}

export function triggerAnalysis(submissionId: number): Promise<AnalysisReportResponse> {
  return apiFetch<AnalysisReportResponse>(`/submissions/${submissionId}/analyze`, {
    method: "POST",
  })
}

export function generateDocstrings(submissionId: number): Promise<DocstringResponse> {
  return apiFetch<DocstringResponse>(`/submissions/${submissionId}/docstrings`, {
    method: "POST",
  })
}
export async function exportReport(submissionId: number, format: "csv" | "pdf"): Promise<void> {
  const token = localStorage.getItem("token")
    const res = await fetch(`/api/submissions/${submissionId}/export/${format}`, {
    headers: { Authorization: `Bearer ${token}` },
  })
  if (!res.ok) throw new Error("Export failed")

  const blob = await res.blob()
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement("a")
  a.href = url
  a.download = `report-${submissionId}.${format}`
  document.body.appendChild(a)
  a.click()
  a.remove()
  window.URL.revokeObjectURL(url)
}
