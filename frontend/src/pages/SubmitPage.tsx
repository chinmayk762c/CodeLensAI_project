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
  const [testCode, setTestCode] = useState("")
  const [showTests, setShowTests] = useState(false)

  async function handleSubmit() {
    setError(null)
    setSubmitting(true)
    try {
      const result = await createSubmission("JAVA", code, showTests ? testCode : undefined)
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
        <Button
          variant="outline"
          size="sm"
          onClick={() => setShowTests(!showTests)}
        >
          {showTests ? "Hide Tests" : "+ Add Tests"}
        </Button>
        <Button onClick={handleSubmit} disabled={submitting} className="gap-1.5">
          <Send size={14} />
          {submitting ? "Submitting..." : "Submit for Analysis"}
        </Button>
      </header>

      {error && (
        <p className="text-sm text-red-400 px-6 py-2">{error}</p>
      )}

              <div className="flex-1 min-h-0 flex">
        <div className={showTests ? "w-1/2 border-r border-border" : "w-full"}>
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
        {showTests && (
          <div className="w-1/2">
            <Editor
              height="100%"
              defaultLanguage="java"
              theme="vs-dark"
              value={testCode}
              onChange={(value) => setTestCode(value ?? "")}
              options={{
                fontSize: 14,
                fontFamily: "JetBrains Mono Variable, monospace",
                minimap: { enabled: false },
                automaticLayout: true,
              }}
            />
          </div>
        )}
      </div>
    </div>
  )
}