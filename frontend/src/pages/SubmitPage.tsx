import { useState } from "react"
import { Send } from "lucide-react"
import Logo from "@/components/Logo"
import Editor from "@monaco-editor/react"
import { Button } from "@/components/ui/button"
import { createSubmission } from "@/lib/SubmissionApi"
import { useNavigate } from "react-router-dom"

const DEFAULT_CODE = `public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, CodeLens AI!");
    }
}
`

export default function SubmitPage() {
  const [code, setCode] = useState(DEFAULT_CODE)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const navigate = useNavigate()

  async function handleSubmit() {
    setError(null)
    setSubmitting(true)
    try {
      const result = await createSubmission("JAVA", code)
      navigate(`/submissions/${result.id}`)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Submission failed")
    } finally {
      setSubmitting(false)
    }
  }

  return (
       <div className="h-screen overflow-hidden bg-background text-foreground flex flex-col font-mono">
            <header className="border-b border-border px-6 py-3 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <button onClick={() => navigate("/")} className="cursor-pointer">
            <Logo size="sm" />
          </button>
          <span className="text-sm text-muted-foreground">/ New Submission</span>
        </div>
        <Button onClick={handleSubmit} disabled={submitting} className="gap-1.5">
          <Send size={14} />
          {submitting ? "Submitting..." : "Submit for Analysis"}
        </Button>
      </header>

      {error && (
        <p className="text-sm text-red-400 px-6 py-2">{error}</p>
      )}

        <div className="flex-1 min-h-0">
        <Editor
          height="100%"
          defaultLanguage="java"
          theme="vs-dark"
          value={code}
          onChange={(value) => setCode(value ?? "")}
          options={{
            fontSize: 14,
            fontFamily: "JetBrains Mono Variable, monospace",
            minimap: { enabled: false },
            automaticLayout: true,
          }}
        />
      </div>
    </div>
  )
}