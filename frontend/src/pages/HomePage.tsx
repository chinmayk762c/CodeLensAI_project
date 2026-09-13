import { useEffect, useState } from "react"
import { useAuth } from "@/context/AuthContext"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { useNavigate } from "react-router-dom"
import { listSubmissions, type SubmissionSummary } from "@/lib/SubmissionApi"

function scoreColor(score: number | null) {
  if (score === null) return "text-muted-foreground"
  if (score >= 70) return "text-green-400"
  if (score >= 40) return "text-yellow-400"
  return "text-red-400"
}

export default function HomePage() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const [submissions, setSubmissions] = useState<SubmissionSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [filter, setFilter] = useState("")

  useEffect(() => {
    listSubmissions()
      .then(setSubmissions)
      .catch((err) => setError(err instanceof Error ? err.message : "Failed to load submissions"))
      .finally(() => setLoading(false))
  }, [])

  function handleLogout() {
    logout()
    navigate("/login")
  }

  const filtered = submissions.filter((s) =>
    filter.trim() === "" ||
    s.language.toLowerCase().includes(filter.toLowerCase()) ||
    s.codePreview.toLowerCase().includes(filter.toLowerCase())
  )

  return (
    <div className="min-h-screen bg-background text-foreground font-mono">
      <header className="border-b border-border px-6 py-3 flex items-center justify-between">
        <h1 className="text-lg">CodeLens AI</h1>
        <div className="flex items-center gap-3">
          <span className="text-sm text-muted-foreground hidden sm:inline">
            {user?.fullName}
          </span>
          <Button onClick={handleLogout} variant="outline" size="sm">
            Log Out
          </Button>
        </div>
      </header>

      <main className="max-w-3xl mx-auto p-6 space-y-6">
        <div className="flex items-center justify-between gap-4">
          <input
            type="text"
            placeholder="Filter by language or code..."
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
            className="flex-1 bg-transparent border border-border rounded px-3 py-2 text-sm outline-none focus:border-foreground/50"
          />
          <Button onClick={() => navigate("/submit")}>New Submission</Button>
        </div>

        {loading && <p className="text-muted-foreground text-sm">Loading submissions...</p>}
        {error && <p className="text-red-400 text-sm">{error}</p>}

        {!loading && filtered.length === 0 && (
          <Card>
            <CardContent className="py-10 text-center">
              <p className="text-muted-foreground text-sm">
                {submissions.length === 0
                  ? "No submissions yet. Create your first one to get started."
                  : "No submissions match your filter."}
              </p>
            </CardContent>
          </Card>
        )}

        <div className="space-y-2">
          {filtered.map((sub) => (
            <Card
              key={sub.id}
              className="hover:border-primary/50 transition-colors cursor-pointer"
              onClick={() => navigate(`/submissions/${sub.id}`)}
            >
              <CardHeader>
                <div className="flex items-center justify-between">
                  <CardTitle className="text-sm font-normal">
                    {sub.language} — #{sub.id}
                  </CardTitle>
                  <span className="text-xs text-muted-foreground">
                    {new Date(sub.createdAt).toLocaleString()}
                  </span>
                </div>
              </CardHeader>
              <CardContent>
                <p className="text-xs text-muted-foreground truncate mb-2">
                  {sub.codePreview}
                </p>
                <div className="flex items-center gap-3 text-xs">
                  {sub.latestStatus ? (
                    <>
                      <span className={scoreColor(sub.latestScore)}>
                        {sub.latestScore ?? "—"}/100
                      </span>
                      <span className="text-muted-foreground">{sub.latestStatus}</span>
                    </>
                  ) : (
                    <span className="text-muted-foreground">Not analyzed yet</span>
                  )}
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      </main>
    </div>
  )
}