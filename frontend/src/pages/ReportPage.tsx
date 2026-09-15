import { useEffect, useState } from "react"
import { useParams, useNavigate } from "react-router-dom"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import {
  getReport,
  triggerAnalysis,
  generateDocstrings,
  type AnalysisReportResponse,
  type IssueResponse,
} from "@/lib/reportApi"

const SEVERITY_ORDER = ["CRITICAL", "HIGH", "MEDIUM", "LOW"] as const
const NO_REPORT_MESSAGE = "No analysis report found for this submission"

function severityColor(severity: string) {
  switch (severity) {
    case "CRITICAL": return "text-red-400 border-red-400/30"
    case "HIGH": return "text-orange-400 border-orange-400/30"
    case "MEDIUM": return "text-yellow-400 border-yellow-400/30"
    default: return "text-blue-400 border-blue-400/30"
  }
}

export default function ReportPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()

  const [report, setReport] = useState<AnalysisReportResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [needsAnalysis, setNeedsAnalysis] = useState(false)
  const [analyzing, setAnalyzing] = useState(false)

  const [docstrings, setDocstrings] = useState<string | null>(null)
  const [docstringsLoading, setDocstringsLoading] = useState(false)

  function loadReport() {
    if (!id) return
    setLoading(true)
    setError(null)
    setNeedsAnalysis(false)

    getReport(Number(id))
      .then(setReport)
      .catch((err) => {
        const message = err instanceof Error ? err.message : "Failed to load report"
        if (message === NO_REPORT_MESSAGE) {
          setNeedsAnalysis(true)
        } else {
          setError(message)
        }
      })
      .finally(() => setLoading(false))
  }

  useEffect(loadReport, [id])

  async function handleAnalyze() {
    if (!id) return
    setAnalyzing(true)
    setError(null)
    try {
      const result = await triggerAnalysis(Number(id))
      setReport(result)
      setNeedsAnalysis(false)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Analysis failed")
    } finally {
      setAnalyzing(false)
    }
  }

  async function handleGenerateDocstrings() {
    if (!id) return
    setDocstringsLoading(true)
    try {
      const res = await generateDocstrings(Number(id))
      setDocstrings(res.documentedCode)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Docstring generation failed")
    } finally {
      setDocstringsLoading(false)
    }
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-background text-foreground flex items-center justify-center font-mono">
        <p className="text-muted-foreground">Loading...</p>
      </div>
    )
  }

  if (needsAnalysis) {
    return (
      <div className="min-h-screen bg-background text-foreground flex flex-col items-center justify-center font-mono gap-4">
        <p className="text-muted-foreground">This submission hasn't been analyzed yet.</p>
        {error && <p className="text-red-400 text-sm">{error}</p>}
        <div className="flex gap-2">
          <Button onClick={handleAnalyze} disabled={analyzing}>
            {analyzing ? "Analyzing..." : "Run Analysis"}
          </Button>
          <Button variant="outline" onClick={() => navigate("/")}>Back to Home</Button>
        </div>
      </div>
    )
  }

  if (error || !report) {
    return (
      <div className="min-h-screen bg-background text-foreground flex flex-col items-center justify-center font-mono gap-4">
        <p className="text-red-400">{error ?? "Report not found"}</p>
        <Button variant="outline" onClick={() => navigate("/")}>Back to Home</Button>
      </div>
    )
  }

  const groupedIssues = SEVERITY_ORDER.map((sev) => ({
    severity: sev,
    items: report.issues.filter((i) => i.severity === sev),
  })).filter((group) => group.items.length > 0)

  return (
    <div className="min-h-screen bg-background text-foreground font-mono">
      <header className="border-b border-border px-6 py-3 flex items-center justify-between">
        <button onClick={() => navigate("/")} className="text-sm text-muted-foreground hover:text-foreground">
          ← Back
        </button>
        <div className="flex items-center gap-3">
          <span className="text-sm text-muted-foreground">Submission #{report.submissionId}</span>
          <Button size="sm" variant="outline" onClick={handleAnalyze} disabled={analyzing}>
            {analyzing ? "Re-analyzing..." : "Re-analyze"}
          </Button>
        </div>
      </header>

      <main className="max-w-3xl mx-auto p-6 space-y-6">
        {error && <p className="text-red-400 text-sm">{error}</p>}

        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle className="text-base font-normal">Score</CardTitle>
              <span
                className={`text-xs px-2 py-1 rounded border ${
                  report.qualityGatePassed
                    ? "text-green-400 border-green-400/30"
                    : "text-red-400 border-red-400/30"
                }`}
              >
                {report.qualityGatePassed ? "PASSED" : "FAILED"} quality gate
              </span>
            </div>
          </CardHeader>
          <CardContent>
            <p className="text-4xl font-semibold">{report.overallScore ?? "—"}<span className="text-lg text-muted-foreground">/100</span></p>
          </CardContent>
        </Card>

        {report.aiSummary && (
          <Card>
            <CardHeader>
              <CardTitle className="text-base font-normal">AI Summary</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-muted-foreground leading-relaxed">{report.aiSummary}</p>
            </CardContent>
          </Card>
        )}

        <Card>
          <CardHeader>
            <CardTitle className="text-base font-normal">
              Issues ({report.issues.length})
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            {groupedIssues.map((group) => (
              <div key={group.severity}>
                <p className={`text-xs mb-2 ${severityColor(group.severity)}`}>
                  {group.severity} ({group.items.length})
                </p>
                <div className="space-y-2">
                  {group.items.map((issue: IssueResponse) => (
                    <div
                      key={issue.id}
                      className={`text-sm border-l-2 pl-3 py-1 ${severityColor(group.severity)}`}
                    >
                      <span className="text-muted-foreground text-xs">[{issue.category}] </span>
                      {issue.description}
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </CardContent>
        </Card>

        {report.optimizedCode && (
          <Card>
            <CardHeader>
              <CardTitle className="text-base font-normal">AI-Optimized Code</CardTitle>
            </CardHeader>
            <CardContent>
              <pre className="text-xs bg-muted/30 p-4 rounded overflow-x-auto whitespace-pre-wrap">
                {report.optimizedCode}
              </pre>
            </CardContent>
          </Card>
        )}

        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle className="text-base font-normal">Documentation</CardTitle>
              <Button size="sm" onClick={handleGenerateDocstrings} disabled={docstringsLoading}>
                {docstringsLoading ? "Generating..." : "Generate Docstrings"}
              </Button>
            </div>
          </CardHeader>
          {docstrings && (
            <CardContent>
              <pre className="text-xs bg-muted/30 p-4 rounded overflow-x-auto whitespace-pre-wrap">
                {docstrings}
              </pre>
            </CardContent>
          )}
        </Card>
        {report.coveragePercentage !== null && (
          <Card>
            <CardHeader>
              <CardTitle className="text-base font-normal">Test Coverage</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-4xl font-semibold">
                {report.coveragePercentage.toFixed(1)}
                <span className="text-lg text-muted-foreground">%</span>
              </p>
            </CardContent>
          </Card>
        )}
      </main>
    </div>
  )
}