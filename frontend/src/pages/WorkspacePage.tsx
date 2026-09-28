import { useEffect, useMemo, useState, useCallback } from "react"
import { useParams, useNavigate } from "react-router-dom"
import Editor, { type Monaco } from "@monaco-editor/react"
import { Group as PanelGroup, Panel, Separator as PanelResizeHandle } from "react-resizable-panels"
import { Play, Send, Terminal as TerminalIcon, Copy, Check, X, Loader2, Sparkles, Code2 } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/components/ui/tabs"
import Logo from "@/components/Logo"
import { getSubmission, createSubmission } from "@/lib/SubmissionApi"
import { executeCode, type ExecuteResponse } from "@/lib/executeApi"
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
const CATEGORY_ORDER = ["BUG", "SECURITY", "PERFORMANCE", "CODE_SMELL", "STYLE"] as const
const NO_REPORT_MESSAGE = "No analysis report found for this submission"

const OCCURRENCES_REGEX = /\s*\(\D{0,2}(\d+) occurrences\)\s*$/
const NOISE_REGEX = /Note: Annotation processing is enabled[\s\S]*?to disable annotation processing\.\s*/

const SEPARATOR_HORIZONTAL_LINE = "h-1 bg-border hover:bg-primary/60 transition-colors"
const SEPARATOR_VERTICAL_LINE = "w-1 bg-border hover:bg-primary/60 transition-colors"

const SEVERITY_STYLES: Record<string, { text: string; border: string; bg: string; dot: string }> = {
  CRITICAL: { text: "text-red-400", border: "border-red-400/30", bg: "bg-red-400/5", dot: "bg-red-400" },
  HIGH: { text: "text-orange-400", border: "border-orange-400/30", bg: "bg-orange-400/5", dot: "bg-orange-400" },
  MEDIUM: { text: "text-yellow-400", border: "border-yellow-400/30", bg: "bg-yellow-400/5", dot: "bg-yellow-400" },
  LOW: { text: "text-blue-400", border: "border-blue-400/30", bg: "bg-blue-400/5", dot: "bg-blue-400" },
}

const EDITOR_OPTIONS = {
  fontSize: 14,
  fontFamily: "JetBrains Mono Variable, monospace",
  fontLigatures: true,
  minimap: { enabled: false },
  automaticLayout: true,
  smoothScrolling: true,
  cursorBlinking: "smooth" as const,
  cursorSmoothCaretAnimation: "on" as const,
  renderLineHighlight: "line" as const,
  bracketPairColorization: { enabled: true },
  scrollBeyondLastLine: false,
  overviewRulerLanes: 0,
  lineNumbersMinChars: 3,
  padding: { top: 12, bottom: 12 },
}

function defineTheme(monaco: Monaco) {
  monaco.editor.defineTheme("codelens", {
    base: "vs-dark",
    inherit: true,
    rules: [],
    colors: {
      "editor.background": "#0a0a0a",
      "editorGutter.background": "#0a0a0a",
      "editor.lineHighlightBackground": "#141414",
      "editor.lineHighlightBorder": "#00000000",
      "editorLineNumber.foreground": "#525252",
      "editorLineNumber.activeForeground": "#d4d4d4",
      "editorCursor.foreground": "#e5e5e5",
      "editorIndentGuide.background1": "#1c1c1c",
      "editorIndentGuide.activeBackground1": "#333333",
      "editorWidget.background": "#111111",
      "scrollbarSlider.background": "#ffffff12",
      "scrollbarSlider.hoverBackground": "#ffffff20",
    },
  })
}

function PanelHeader({
  icon,
  title,
  children,
}: {
  icon: React.ReactNode
  title: string
  children?: React.ReactNode
}) {
  return (
    <div className="h-9 shrink-0 flex items-center justify-between px-3 border-b border-border bg-card/50">
      <div className="flex items-center gap-2 text-xs text-muted-foreground">
        {icon}
        <span className="font-medium tracking-wide">{title}</span>
      </div>
      <div className="flex items-center gap-1">{children}</div>
    </div>
  )
}

function CopyButton({ text }: { text: string }) {
  const [copied, setCopied] = useState(false)

  async function handleCopy() {
    try {
      await navigator.clipboard.writeText(text)
      setCopied(true)
      setTimeout(() => setCopied(false), 1500)
    } catch {
      // clipboard not available
    }
  }

  return (
    <Button size="sm" variant="outline" className="h-7 text-xs gap-1.5" onClick={handleCopy}>
      {copied ? <Check size={12} /> : <Copy size={12} />}
      {copied ? "Copied" : "Copy"}
    </Button>
  )
}

function ScoreRing({ score }: { score: number }) {
  const radius = 46
  const circumference = 2 * Math.PI * radius
  const pct = Math.max(0, Math.min(100, score)) / 100
  const color = score >= 70 ? "#4ade80" : score >= 40 ? "#facc15" : "#f87171"

  return (
    <div className="relative w-28 h-28 shrink-0">
      <svg viewBox="0 0 120 120" className="w-full h-full -rotate-90">
        <circle cx="60" cy="60" r={radius} fill="none" stroke="currentColor" strokeWidth="8" className="text-muted/50" />
        <circle
          cx="60"
          cy="60"
          r={radius}
          fill="none"
          stroke={color}
          strokeWidth="8"
          strokeLinecap="round"
          strokeDasharray={circumference}
          strokeDashoffset={circumference * (1 - pct)}
          className="transition-all duration-700"
        />
      </svg>
      <div className="absolute inset-0 flex flex-col items-center justify-center">
        <span className="text-2xl font-semibold">{score}</span>
        <span className="text-[10px] text-muted-foreground">/ 100</span>
      </div>
    </div>
  )
}

function IssueRow({ issue }: { issue: IssueResponse }) {
  const [expanded, setExpanded] = useState(false)
  const style = SEVERITY_STYLES[issue.severity] ?? SEVERITY_STYLES.LOW

  let text = issue.description
  const isAi = text.startsWith("[AI] ")
  if (isAi) text = text.slice(5)
  text = text.replace(NOISE_REGEX, "")

  let count = 1
  const match = text.match(OCCURRENCES_REGEX)
  if (match) {
    count = Number(match[1])
    text = text.replace(OCCURRENCES_REGEX, "")
  }

  const isLong = text.length > 140

  return (
    <div className={`rounded-md border ${style.border} ${style.bg} px-3 py-2`}>
      <div className="flex items-start gap-2">
        <span className={`mt-1.5 h-1.5 w-1.5 rounded-full shrink-0 ${style.dot}`} />
        <div className="flex-1 min-w-0">
          <div className="flex flex-wrap items-center gap-1.5 mb-1">
            <span className="text-[10px] uppercase tracking-wide text-muted-foreground">
              {issue.category.replace("_", " ")}
            </span>
            {isAi && (
              <span className="text-[10px] px-1.5 rounded bg-primary/15 text-primary inline-flex items-center gap-1">
                <Sparkles size={10} />
                AI
              </span>
            )}
            {count > 1 && (
              <span className="text-[10px] px-1.5 rounded bg-muted text-muted-foreground">x{count}</span>
            )}
            {issue.lineNumber !== null && (
              <span className="text-[10px] text-muted-foreground">line {issue.lineNumber}</span>
            )}
          </div>
          <p className={`text-xs leading-relaxed text-foreground/90 break-words ${isLong && !expanded ? "line-clamp-2" : ""}`}>
            {text}
          </p>
          {isLong && (
            <button
              onClick={() => setExpanded(!expanded)}
              className="text-[10px] text-muted-foreground hover:text-foreground mt-1"
            >
              {expanded ? "Show less" : "Show more"}
            </button>
          )}
        </div>
      </div>
    </div>
  )
}

function CodeViewer({ value, path }: { value: string; path: string }) {
  return (
    <div className="rounded-md border border-border overflow-hidden">
      <Editor
        height="380px"
        defaultLanguage="java"
        path={path}
        theme="codelens"
        value={value}
        beforeMount={defineTheme}
        options={{
          ...EDITOR_OPTIONS,
          readOnly: true,
          domReadOnly: true,
          fontSize: 13,
          scrollbar: { alwaysConsumeMouseWheel: false },
        }}
      />
    </div>
  )
}

function EmptyState({
  icon,
  title,
  children,
}: {
  icon: React.ReactNode
  title: string
  children?: React.ReactNode
}) {
  return (
    <div className="h-full flex flex-col items-center justify-center text-center gap-3 p-8">
      <div className="text-muted-foreground">{icon}</div>
      <p className="text-sm font-medium">{title}</p>
      {children && <div className="text-xs text-muted-foreground max-w-xs leading-relaxed">{children}</div>}
    </div>
  )
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
  const [terminalOpen, setTerminalOpen] = useState(false)

  const [tab, setTab] = useState("overview")
  const [severityFilter, setSeverityFilter] = useState<string>("ALL")

  const loadExisting = useCallback(async (submissionId: number) => {
    setLoadingReport(true)
    setError(null)
    setNeedsAnalysis(false)
    setReport(null)
    setDocstrings(null)

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
    setTab("overview")
    setSeverityFilter("ALL")
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
      setTab("overview")
    } catch (err) {
      setError(err instanceof Error ? err.message : "Analysis failed")
    } finally {
      setAnalyzing(false)
    }
  }

  async function handleRun() {
    setTerminalOpen(true)
    setRunning(true)
    setRunResult(null)
    try {
      const res = await executeCode(code)
      setRunResult(res)
    } catch (err) {
      setRunResult({
        output: err instanceof Error ? err.message : "Run failed",
        timedOut: false,
        compileFailed: false,
        exitCode: null,
      })
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

  const severityCounts = useMemo(() => {
    const counts: Record<string, number> = { CRITICAL: 0, HIGH: 0, MEDIUM: 0, LOW: 0 }
    report?.issues.forEach((i) => {
      counts[i.severity] = (counts[i.severity] ?? 0) + 1
    })
    return counts
  }, [report])

  const categoryCounts = useMemo(() => {
    const counts: Record<string, number> = {}
    report?.issues.forEach((i) => {
      counts[i.category] = (counts[i.category] ?? 0) + 1
    })
    return counts
  }, [report])

  const filteredIssues = useMemo(() => {
    if (!report) return []
    const list = severityFilter === "ALL" ? report.issues : report.issues.filter((i) => i.severity === severityFilter)
    return [...list].sort(
      (a, b) =>
        SEVERITY_ORDER.indexOf(a.severity as (typeof SEVERITY_ORDER)[number]) -
        SEVERITY_ORDER.indexOf(b.severity as (typeof SEVERITY_ORDER)[number])
    )
  }, [report, severityFilter])

  const maxCategoryCount = Math.max(1, ...Object.values(categoryCounts))

  const sizes =
    showTests && terminalOpen
      ? { code: "45%", test: "30%", term: "25%" }
      : showTests
        ? { code: "62%", test: "38%", term: "0%" }
        : terminalOpen
          ? { code: "68%", test: "0%", term: "32%" }
          : { code: "100%", test: "0%", term: "0%" }

  const runOk = runResult && !runResult.compileFailed && !runResult.timedOut && runResult.exitCode === 0
  const runLabel = runResult
    ? runResult.compileFailed
      ? "Compile error"
      : runResult.timedOut
        ? "Timed out"
        : `Exit ${runResult.exitCode}`
    : ""

  function renderAnalysis() {
    if (analyzing) {
      return (
        <EmptyState icon={<Loader2 size={28} className="animate-spin" />} title="Analyzing your code...">
          Running PMD, Checkstyle, SpotBugs and the AI review. This can take up to a minute or two.
        </EmptyState>
      )
    }

    if (!id) {
      return (
        <EmptyState icon={<Sparkles size={28} />} title="No analysis yet">
          Write your code on the left, hit Run to try it out, then Submit for Analysis to get a score, issues
          and an optimized version.
        </EmptyState>
      )
    }

    if (loadingReport) {
      return (
        <EmptyState icon={<Loader2 size={28} className="animate-spin" />} title="Loading..." />
      )
    }

    if (!report) {
      return (
        <EmptyState icon={<Sparkles size={28} />} title="Not analyzed yet">
          <div className="space-y-3">
            <p>This submission has not been analyzed. Run the analysis to get a score and detailed feedback.</p>
            <Button size="sm" onClick={handleAnalyze}>
              Run Analysis
            </Button>
          </div>
        </EmptyState>
      )
    }

    return (
      <Tabs value={tab} onValueChange={(v) => setTab(String(v))} className="p-4">
        <TabsList variant="line" className="w-full justify-start">
          <TabsTrigger value="overview" className="flex-none px-3">Overview</TabsTrigger>
          <TabsTrigger value="issues" className="flex-none px-3">Issues ({report.issues.length})</TabsTrigger>
          <TabsTrigger value="optimized" className="flex-none px-3">Optimized</TabsTrigger>
          <TabsTrigger value="docs" className="flex-none px-3">Docs</TabsTrigger>
        </TabsList>

        <TabsContent value="overview" className="mt-4 space-y-5">
          <div className="flex items-center gap-6">
            <ScoreRing score={report.overallScore ?? 0} />
            <div className="space-y-3">
              <span
                className={`inline-block text-xs px-2 py-1 rounded border ${
                  report.qualityGatePassed
                    ? "text-green-400 border-green-400/30 bg-green-400/10"
                    : "text-red-400 border-red-400/30 bg-red-400/10"
                }`}
              >
                {report.qualityGatePassed ? "Quality gate passed" : "Quality gate failed"}
              </span>
              {report.coveragePercentage !== null && (
                <div>
                  <div className="text-[11px] text-muted-foreground mb-1">Test coverage</div>
                  <div className="w-44 h-1.5 rounded bg-muted overflow-hidden">
                    <div
                      className="h-full bg-green-400"
                      style={{ width: `${Math.min(100, report.coveragePercentage)}%` }}
                    />
                  </div>
                  <div className="text-xs mt-1">{report.coveragePercentage.toFixed(1)}%</div>
                </div>
              )}
            </div>
          </div>

          <div>
            <div className="text-[11px] text-muted-foreground mb-2">By severity</div>
            <div className="flex flex-wrap gap-2">
              {SEVERITY_ORDER.map((sev) => {
                const style = SEVERITY_STYLES[sev]
                return (
                  <button
                    key={sev}
                    onClick={() => {
                      setSeverityFilter(sev)
                      setTab("issues")
                    }}
                    className={`text-xs px-2.5 py-1 rounded border ${style.border} ${style.bg} ${style.text} hover:brightness-125 transition`}
                  >
                    {sev} {severityCounts[sev]}
                  </button>
                )
              })}
            </div>
          </div>

          <div>
            <div className="text-[11px] text-muted-foreground mb-2">By category</div>
            <div className="space-y-1.5">
              {CATEGORY_ORDER.filter((c) => categoryCounts[c]).map((cat) => (
                <div key={cat} className="flex items-center gap-2 text-xs">
                  <span className="w-24 text-muted-foreground">{cat.replace("_", " ")}</span>
                  <div className="flex-1 h-1.5 rounded bg-muted overflow-hidden">
                    <div
                      className="h-full bg-foreground/60"
                      style={{ width: `${(categoryCounts[cat] / maxCategoryCount) * 100}%` }}
                    />
                  </div>
                  <span className="w-6 text-right">{categoryCounts[cat]}</span>
                </div>
              ))}
            </div>
          </div>

          {report.aiSummary && (
            <div className="rounded-md border border-border bg-card/40 p-3">
              <div className="flex items-center gap-1.5 text-[11px] text-muted-foreground mb-1.5">
                <Sparkles size={12} />
                AI summary
              </div>
              <p className="text-xs leading-relaxed text-foreground/90">{report.aiSummary}</p>
            </div>
          )}
        </TabsContent>

        <TabsContent value="issues" className="mt-4 space-y-3">
          <div className="flex flex-wrap gap-1.5">
            {["ALL", ...SEVERITY_ORDER].map((sev) => (
              <button
                key={sev}
                onClick={() => setSeverityFilter(sev)}
                className={`text-[11px] px-2 py-0.5 rounded border transition ${
                  severityFilter === sev
                    ? "border-foreground/50 bg-foreground/10 text-foreground"
                    : "border-border text-muted-foreground hover:text-foreground"
                }`}
              >
                {sev === "ALL" ? `All ${report.issues.length}` : `${sev} ${severityCounts[sev]}`}
              </button>
            ))}
          </div>
          {filteredIssues.length === 0 ? (
            <p className="text-xs text-muted-foreground py-6 text-center">No issues in this category.</p>
          ) : (
            <div className="space-y-2">
              {filteredIssues.map((issue) => (
                <IssueRow key={issue.id} issue={issue} />
              ))}
            </div>
          )}
        </TabsContent>

        <TabsContent value="optimized" className="mt-4 space-y-3">
          {report.optimizedCode ? (
            <>
              <div className="flex items-center gap-2">
                <CopyButton text={report.optimizedCode} />
                <Button
                  size="sm"
                  variant="outline"
                  className="h-7 text-xs"
                  onClick={() => setCode(report.optimizedCode ?? "")}
                >
                  Apply to editor
                </Button>
              </div>
              <CodeViewer value={report.optimizedCode} path="Optimized.java" />
            </>
          ) : (
            <p className="text-xs text-muted-foreground py-6 text-center">No optimized code available.</p>
          )}
        </TabsContent>

        <TabsContent value="docs" className="mt-4 space-y-3">
          <div className="flex items-center gap-2">
            <Button
              size="sm"
              className="h-7 text-xs"
              onClick={handleGenerateDocstrings}
              disabled={docstringsLoading}
            >
              {docstringsLoading ? "Generating..." : "Generate Docstrings"}
            </Button>
            {docstrings && (
              <>
                <CopyButton text={docstrings} />
                <Button
                  size="sm"
                  variant="outline"
                  className="h-7 text-xs"
                  onClick={() => setCode(docstrings)}
                >
                  Apply to editor
                </Button>
              </>
            )}
          </div>
          {docstrings ? (
            <CodeViewer value={docstrings} path="Documented.java" />
          ) : (
            <p className="text-xs text-muted-foreground py-6 text-center">
              Generate Javadoc comments for your code with one click.
            </p>
          )}
        </TabsContent>
      </Tabs>
    )
  }

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
          <Button size="sm" variant="ghost" onClick={() => setShowTests(!showTests)}>
            {showTests ? "Hide Tests" : "+ Add Tests"}
          </Button>
          <Button size="sm" variant="ghost" onClick={() => setTerminalOpen(!terminalOpen)} className="gap-1.5">
            <TerminalIcon size={14} />
            Terminal
          </Button>
          <Button size="sm" variant="outline" onClick={handleRun} disabled={running} className="gap-1.5">
            {running ? <Loader2 size={14} className="animate-spin" /> : <Play size={14} />}
            {running ? "Running..." : "Run"}
          </Button>
          {id && (
            <Button size="sm" variant="outline" onClick={handleAnalyze} disabled={analyzing}>
              {analyzing ? "Analyzing..." : report ? "Re-analyze" : "Run Analysis"}
            </Button>
          )}
          <Button size="sm" onClick={handleSubmit} disabled={submitting} className="gap-1.5">
            <Send size={14} />
            {submitting ? "Submitting..." : id ? "Submit as new" : "Submit for Analysis"}
          </Button>
        </div>
      </header>

      {error && <p className="text-xs text-red-400 px-4 py-1 shrink-0">{error}</p>}

      <div className="flex-1 min-h-0">
        <PanelGroup orientation="horizontal" id="workspace">
          <Panel id="left" defaultSize="50%" minSize="25%">
            <PanelGroup key={`left-${showTests}-${terminalOpen}`} orientation="vertical" id="left-stack">
              <Panel id="code" defaultSize={sizes.code} minSize="20%">
                <div className="h-full flex flex-col bg-background">
                  <PanelHeader icon={<Code2 size={13} />} title="Source" />
                  <div className="flex-1 min-h-0">
                    <Editor
                      height="100%"
                      defaultLanguage="java"
                      path="Main.java"
                      keepCurrentModel
                      theme="codelens"
                      value={code}
                      beforeMount={defineTheme}
                      onChange={(v) => setCode(v ?? "")}
                      options={EDITOR_OPTIONS}
                    />
                  </div>
                </div>
              </Panel>

              {showTests && (
                <>
                  <PanelResizeHandle className={SEPARATOR_HORIZONTAL_LINE} />
                  <Panel id="tests" defaultSize={sizes.test} minSize="15%">
                    <div className="h-full flex flex-col bg-background">
                      <PanelHeader icon={<Code2 size={13} />} title="Tests (JUnit)" />
                      <div className="flex-1 min-h-0">
                        <Editor
                          height="100%"
                          defaultLanguage="java"
                          path="MainTest.java"
                          keepCurrentModel
                          theme="codelens"
                          value={testCode}
                          beforeMount={defineTheme}
                          onChange={(v) => setTestCode(v ?? "")}
                          options={EDITOR_OPTIONS}
                        />
                      </div>
                    </div>
                  </Panel>
                </>
              )}

              {terminalOpen && (
                <>
                  <PanelResizeHandle className={SEPARATOR_HORIZONTAL_LINE} />
                  <Panel id="terminal" defaultSize={sizes.term} minSize="10%">
                    <div className="h-full flex flex-col bg-background">
                      <PanelHeader icon={<TerminalIcon size={13} />} title="Terminal">
                        {running && (
                          <span className="flex items-center gap-1 text-[11px] text-muted-foreground mr-1">
                            <Loader2 size={12} className="animate-spin" />
                            Running
                          </span>
                        )}
                        {!running && runResult && (
                          <span
                            className={`text-[11px] px-1.5 py-0.5 rounded mr-1 ${
                              runOk ? "bg-green-400/15 text-green-400" : "bg-red-400/15 text-red-400"
                            }`}
                          >
                            {runLabel}
                          </span>
                        )}
                        <button
                          onClick={() => setTerminalOpen(false)}
                          className="text-muted-foreground hover:text-foreground p-1"
                          title="Close terminal"
                        >
                          <X size={13} />
                        </button>
                      </PanelHeader>
                      <div className="flex-1 min-h-0 overflow-auto bg-[#0a0a0a] p-3 text-xs">
                        {running && !runResult ? (
                          <p className="text-muted-foreground">Compiling and running...</p>
                        ) : runResult ? (
                          <>
                            <p className="text-muted-foreground mb-1">$ java Main</p>
                            <pre
                              className={`whitespace-pre-wrap break-words ${
                                runOk ? "text-foreground/90" : "text-red-400"
                              }`}
                            >
                              {runResult.output || "(no output)"}
                            </pre>
                          </>
                        ) : (
                          <p className="text-muted-foreground">Click Run to execute your code. Output appears here.</p>
                        )}
                      </div>
                    </div>
                  </Panel>
                </>
              )}
            </PanelGroup>
          </Panel>

          <PanelResizeHandle className={SEPARATOR_VERTICAL_LINE} />

          <Panel id="right" defaultSize="50%" minSize="25%">
            <div className="h-full flex flex-col bg-background">
              <PanelHeader icon={<Sparkles size={13} />} title="Analysis">
                {report && id && (
                  <>
                    <Button
                      size="sm"
                      variant="ghost"
                      className="h-7 text-xs"
                      onClick={() => exportReport(Number(id), "csv")}
                    >
                      CSV
                    </Button>
                    <Button
                      size="sm"
                      variant="ghost"
                      className="h-7 text-xs"
                      onClick={() => exportReport(Number(id), "pdf")}
                    >
                      PDF
                    </Button>
                  </>
                )}
              </PanelHeader>
              <div className="flex-1 min-h-0 overflow-y-auto">{renderAnalysis()}</div>
            </div>
          </Panel>
        </PanelGroup>
      </div>
    </div>
  )
}