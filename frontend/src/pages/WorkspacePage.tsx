import { useEffect, useState, useCallback } from "react"
import { useParams, useNavigate } from "react-router-dom"
import Editor from "@monaco-editor/react"
import { executeCode, type ExecuteResponse } from "@/lib/executeApi"
import { Play, Send } from "lucide-react"
import { Group as PanelGroup, Panel, Separator as PanelResizeHandle } from "react-resizable-panels"
import { Button } from "@/components/ui/button"
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/components/ui/tabs"
import Logo from "@/components/Logo"
import { getSubmission, createSubmission } from "@/lib/SubmissionApi"
import {
  getReport,
  triggerAnalysis,
  generateDocstrings,
  exportReport,
  type AnalysisReportResponse,
  type IssueResponse,
} from "@/lib/reportApi"

const DEFAULT_CODE = `public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, CodeLens AI!");
    }
}
`

const SEVERITY_ORDER = ["CRITICAL", "HIGH", "MEDIUM", "LOW"] as const
const NO_REPORT_MESSAGE = "No analysis report found for this submission"

const EDITOR_OPTIONS = {
  fontSize: 14,
  fontFamily: "JetBrains Mono Variable, monospace",
  fontLigatures: true,
  minimap: { enabled: false },
  automaticLayout: true,
  smoothScrolling: true,
  cursorBlinking: "smooth" as const,
  cursorSmoothCaretAnimation: "on" as const,
  renderLineHighlight: "all" as const,
  bracketPairColorization: { enabled: true },
  padding: { top: 12 },
}

function severityColor(severity: string) {
  switch (severity) {
    case "CRITICAL": return "text-red-400 border-red-400/30"
    case "HIGH": return "text-orange-400 border-orange-400/30"
    case "MEDIUM": return "text-yellow-400 border-yellow-400/30"
    default: return "text-blue-400 border-blue-400/30"
  }
}

export default function WorkspacePage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()

  const [code, setCode] = useState(DEFAULT_CODE)
  const [testCode, setTestCode] = useState("")
  const [showTests, setShowTests] = useState(false)

  const [report, setReport] = useState<AnalysisReportResponse | null>(null)
  const [loadingReport, setLoadingReport] = useState(false)
  const [needsAnalysis, setNeedsAnalysis] = useState(false)

  const [submitting, setSubmitting] = useState(false)
  const [analyzing, setAnalyzing] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const [docstrings, setDocstrings] = useState<string | null>(null)
  const [docstringsLoading, setDocstringsLoading] = useState(false)

  const [runResult, setRunResult] = useState<ExecuteResponse | null>(null)
  const [running, setRunning] = useState(false)

  const loadExisting = useCallback(async (submissionId: number) => {
    setLoadingReport(true)
    setError(null)
    setNeedsAnalysis(false)
    setReport(null)

    try {
      const sub = await getSubmission(submissionId)
      setCode(sub.code)
      if (sub.testCode) {
        setTestCode(sub.testCode)
        setShowTests(true)
      }
    } catch {
      setError("Could not load submission")
    }

    try {
      const rep = await getReport(submissionId)
      setReport(rep)
    } catch (err) {
      const message = err instanceof Error ? err.message : ""
      if (message === NO_REPORT_MESSAGE) {
        setNeedsAnalysis(true)
      }
    } finally {
      setLoadingReport(false)
    }
  }, [])

  useEffect(() => {
    if (id) {
      loadExisting(Number(id))
    } else {
      setCode(DEFAULT_CODE)
      setTestCode("")
      setShowTests(false)
      setReport(null)
      setNeedsAnalysis(false)
      setDocstrings(null)
      setRunResult(null)
    }
  }, [id, loadExisting])

  async function handleSubmit() {
    setSubmitting(true)
    setError(null)
    try {
      const sub = await createSubmission("JAVA", code, showTests ? testCode : undefined)
      navigate(`/submissions/${sub.id}`)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Submission failed")
    } finally {
      setSubmitting(false)
    }
  }

  async function handleAnalyze() {
    if (!id) return
    setAnalyzing(true)
    setError(null)
    try {
      const rep = await triggerAnalysis(Number(id))
      setReport(rep)
      setNeedsAnalysis(false)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Analysis failed")
    } finally {
      setAnalyzing(false)
    }
  }

  async function handleRun() {
    setRunning(true)
    setRunResult(null)
    try {
      const res = await executeCode(code)
      setRunResult(res)
    } catch (err) {
      setRunResult({ output: err instanceof Error ? err.message : "Run failed", timedOut: false, compileFailed: false, exitCode: null })
    } finally {
      setRunning(false)
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

  const groupedIssues = report
    ? SEVERITY_ORDER.map((sev) => ({
        severity: sev,
        items: report.issues.filter((i) => i.severity === sev),
      })).filter((group) => group.items.length > 0)
    : []

  return (
    <div className="h-screen overflow-hidden bg-background text-foreground flex flex-col font-mono">
      <header className="border-b border-border px-4 py-2 flex items-center justify-between shrink-0">
        <div className="flex items-center gap-3">
          <button onClick={() => navigate("/")} className="cursor-pointer">
            <Logo size="sm" />
          </button>
          <span className="text-xs text-muted-foreground">
            {id ? `/ Submission #${id}` : "/ New Submission"}
          </span>
        </div>
        <div className="flex items-center gap-2">
          {!id && (
            <Button size="sm" variant="outline" onClick={() => setShowTests(!showTests)}>
              {showTests ? "Hide Tests" : "+ Add Tests"}
            </Button>
          )}
          <Button size="sm" variant="outline" onClick={handleRun} disabled={running} className="gap-1.5">
            <Play size={14} />
            {running ? "Running..." : "Run"}
          </Button>
          {!id ? (
            <Button size="sm" onClick={handleSubmit} disabled={submitting} className="gap-1.5">
              <Send size={14} />
              {submitting ? "Submitting..." : "Submit for Analysis"}
            </Button>
          ) : (
            <>
              <Button size="sm" variant="outline" onClick={handleAnalyze} disabled={analyzing}>
                {analyzing ? "Analyzing..." : report ? "Re-analyze" : "Run Analysis"}
              </Button>
              {report && (
                <>
                  <Button size="sm" variant="outline" onClick={() => exportReport(Number(id), "csv")}>
                    CSV
                  </Button>
                  <Button size="sm" variant="outline" onClick={() => exportReport(Number(id), "pdf")}>
                    PDF
                  </Button>
                </>
              )}
            </>
          )}
        </div>
      </header>

      {error && <p className="text-xs text-red-400 px-4 py-1 shrink-0">{error}</p>}

      <div className="flex-1 min-h-0">
        <PanelGroup direction="horizontal">
          <Panel defaultSize={50} minSize={25}>
            <div className="h-full flex flex-col">
              <div className="flex-1 min-h-0">
                {showTests ? (
                  <PanelGroup direction="vertical">
                    <Panel defaultSize={60} minSize={20}>
                      <Editor
                        height="100%"
                        defaultLanguage="java"
                        theme="vs-dark"
                        value={code}
                        onChange={(v) => setCode(v ?? "")}
                        options={EDITOR_OPTIONS}
                      />
                    </Panel>
                    <PanelResizeHandle className="h-1 bg-border hover:bg-primary/50 transition-colors" />
                    <Panel defaultSize={40} minSize={15}>
                      <div className="h-full flex flex-col">
                        <div className="text-xs text-muted-foreground px-2 py-1 border-b border-border shrink-0">Test Code</div>
                        <div className="flex-1 min-h-0">
                          <Editor
                            height="100%"
                            defaultLanguage="java"
                            theme="vs-dark"
                            value={testCode}
                            onChange={(v) => setTestCode(v ?? "")}
                            options={EDITOR_OPTIONS}
                          />
                        </div>
                      </div>
                    </Panel>
                  </PanelGroup>
                ) : (
                  <Editor
                    height="100%"
                    defaultLanguage="java"
                    theme="vs-dark"
                    value={code}
                    onChange={(v) => setCode(v ?? "")}
                    options={EDITOR_OPTIONS}
                  />
                )}
              </div>

              {runResult && (
                <div className="border-t border-border shrink-0 max-h-48 overflow-y-auto">
                  <div className="text-xs text-muted-foreground px-2 py-1 flex items-center justify-between">
                    <span>Console {runResult.exitCode !== null && `(exit ${runResult.exitCode})`}</span>
                    <button onClick={() => setRunResult(null)} className="hover:text-foreground">x</button>
                  </div>
                  <pre className={`text-xs px-3 pb-2 whitespace-pre-wrap ${runResult.timedOut || runResult.compileFailed ? "text-red-400" : "text-foreground"}`}>
                    {runResult.output || "(no output)"}
                  </pre>
                </div>
              )}
            </div>
          </Panel>

          <PanelResizeHandle className="w-1 bg-border hover:bg-primary/50 transition-colors" />

          <Panel defaultSize={50} minSize={25}>
            <div className="h-full overflow-y-auto p-4">
              {!id && (
                <p className="text-sm text-muted-foreground">Submit your code to see analysis results here.</p>
              )}

              {id && loadingReport && (
                <p className="text-sm text-muted-foreground">Loading...</p>
              )}

              {id && !loadingReport && needsAnalysis && (
                <p className="text-sm text-muted-foreground">This submission hasn't been analyzed yet. Click "Run Analysis" above.</p>
              )}

              {id && report && (
                <Tabs defaultValue="overview">
                  <TabsList>
                    <TabsTrigger value="overview">Overview</TabsTrigger>
                    <TabsTrigger value="issues">Issues ({report.issues.length})</TabsTrigger>
                    <TabsTrigger value="ai">AI Summary</TabsTrigger>
                    <TabsTrigger value="optimized">Optimized</TabsTrigger>
                    <TabsTrigger value="docs">Docs</TabsTrigger>
                  </TabsList>

                  <TabsContent value="overview" className="space-y-4 mt-4">
                    <div className="flex items-center justify-between">
                      <p className="text-4xl font-semibold">
                        {report.overallScore ?? "-"}<span className="text-lg text-muted-foreground">/100</span>
                      </p>
                      <span className={`text-xs px-2 py-1 rounded border ${report.qualityGatePassed ? "text-green-400 border-green-400/30" : "text-red-400 border-red-400/30"}`}>
                        {report.qualityGatePassed ? "PASSED" : "FAILED"} quality gate
                      </span>
                    </div>
                    {report.coveragePercentage !== null && (
                      <p className="text-sm text-muted-foreground">
                        Test Coverage: <span className="text-foreground">{report.coveragePercentage.toFixed(1)}%</span>
                      </p>
                    )}
                  </TabsContent>

                  <TabsContent value="issues" className="space-y-4 mt-4">
                    {groupedIssues.map((group) => (
                      <div key={group.severity}>
                        <p className={`text-xs mb-2 ${severityColor(group.severity)}`}>{group.severity} ({group.items.length})</p>
                        <div className="space-y-2">
                          {group.items.map((issue: IssueResponse) => (
                            <div key={issue.id} className={`text-sm border-l-2 pl-3 py-1 ${severityColor(group.severity)}`}>
                              <span className="text-muted-foreground text-xs">[{issue.category}] </span>
                              {issue.description}
                            </div>
                          ))}
                        </div>
                      </div>
                    ))}
                  </TabsContent>

                  <TabsContent value="ai" className="mt-4">
                    <p className="text-sm text-muted-foreground leading-relaxed">{report.aiSummary}</p>
                  </TabsContent>

                  <TabsContent value="optimized" className="mt-4">
                    {report.optimizedCode ? (
                      <pre className="text-xs bg-muted/30 p-4 rounded overflow-x-auto whitespace-pre-wrap">{report.optimizedCode}</pre>
                    ) : (
                      <p className="text-sm text-muted-foreground">No optimized code available.</p>
                    )}
                  </TabsContent>

                  <TabsContent value="docs" className="mt-4 space-y-3">
                    <Button size="sm" onClick={handleGenerateDocstrings} disabled={docstringsLoading}>
                      {docstringsLoading ? "Generating..." : "Generate Docstrings"}
                    </Button>
                    {docstrings && (
                      <pre className="text-xs bg-muted/30 p-4 rounded overflow-x-auto whitespace-pre-wrap">{docstrings}</pre>
                    )}
                  </TabsContent>
                </Tabs>
              )}
            </div>
          </Panel>
        </PanelGroup>
      </div>
    </div>
  )
}