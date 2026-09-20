import { useState } from "react"
import { useNavigate } from "react-router-dom"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { analyzeRepo, type RepoAnalysisResult } from "@/lib/repoAnalysisApi"

export default function RepoAnalysisPage() {
  const [repoUrl, setRepoUrl] = useState("")
  const [result, setResult] = useState<RepoAnalysisResult | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const navigate = useNavigate()

  async function handleAnalyze() {
    setLoading(true)
    setError(null)
    setResult(null)
    try {
      const res = await analyzeRepo(repoUrl)
      setResult(res)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Repo analysis failed")
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-background text-foreground font-mono">
      <header className="border-b border-border px-6 py-3 flex items-center justify-between">
        <button onClick={() => navigate("/")} className="text-sm text-muted-foreground hover:text-foreground">
          ← Back
        </button>
        <h1 className="text-sm text-muted-foreground">Repo-Wide Analysis</h1>
      </header>

      <main className="max-w-2xl mx-auto p-6 space-y-6">
        <div className="flex gap-2">
          <Input
            placeholder="https://github.com/owner/repo"
            value={repoUrl}
            onChange={(e) => setRepoUrl(e.target.value)}
          />
          <Button onClick={handleAnalyze} disabled={loading || !repoUrl}>
            {loading ? "Analyzing..." : "Analyze Repo"}
          </Button>
        </div>

        {loading && (
          <p className="text-sm text-muted-foreground">
            Fetching and analyzing files — this can take a minute or two for larger repos...
          </p>
        )}

        {error && <p className="text-sm text-red-400">{error}</p>}

        {result && (
          <Card>
            <CardHeader>
              <CardTitle className="text-base font-normal">
                {result.repoUrl} — {result.files.length} file(s)
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-2">
              {result.files.map((f, i) => (
                <div
                  key={i}
                  className={`text-sm border-l-2 pl-3 py-1 flex items-center justify-between ${
                    f.errorMessage ? "border-red-400/30" : "border-green-400/30 cursor-pointer hover:bg-muted/20"
                  }`}
                  onClick={() => f.submissionId && navigate(`/submissions/${f.submissionId}`)}
                >
                  <span>{f.filePath}</span>
                  {f.errorMessage ? (
                    <span className="text-red-400 text-xs">{f.errorMessage}</span>
                  ) : (
                    <span className="text-muted-foreground text-xs">View report →</span>
                  )}
                </div>
              ))}
            </CardContent>
          </Card>
        )}
      </main>
    </div>
  )
}