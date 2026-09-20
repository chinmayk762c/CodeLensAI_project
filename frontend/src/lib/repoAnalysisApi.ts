import { apiFetch } from "@/lib/api"

export interface RepoAnalysisFileResult {
  filePath: string
  submissionId: number | null
  errorMessage: string | null
}

export interface RepoAnalysisResult {
  id: number
  repoUrl: string
  status: string
  createdAt: string
  files: RepoAnalysisFileResult[]
}

export function analyzeRepo(repoUrl: string): Promise<RepoAnalysisResult> {
  return apiFetch<RepoAnalysisResult>("/repo-analysis", {
    method: "POST",
    body: JSON.stringify({ repoUrl }),
  })
}