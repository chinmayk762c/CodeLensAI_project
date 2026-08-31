import { useAuth } from "@/context/AuthContext"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { useNavigate } from "react-router-dom"
import NavBar from "@/components/NavBar"
import { Code2, ArrowRight } from "lucide-react"

export default function HomePage() {
  const { user } = useAuth()
  const navigate = useNavigate()

  return (
    <div className="min-h-screen bg-background text-foreground flex flex-col font-mono">
      <NavBar />

      <main className="flex-1 flex items-center justify-center p-6">
        <div className="w-full max-w-md">
          <p className="text-sm text-muted-foreground mb-1">Welcome back,</p>
          <h1 className="text-2xl font-semibold mb-8">{user?.fullName ?? "there"}</h1>

          <Card className="hover:border-primary/50 transition-colors cursor-pointer" onClick={() => navigate("/submit")}>
            <CardHeader>
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="bg-primary/10 text-primary rounded-md p-2">
                    <Code2 size={20} />
                  </div>
                  <CardTitle className="text-base font-normal">New Submission</CardTitle>
                </div>
                <ArrowRight size={16} className="text-muted-foreground" />
              </div>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-muted-foreground">
                Paste or write Java code and submit it for review.
              </p>
            </CardContent>
          </Card>

          <div className="mt-6">
            <Button
              variant="outline"
              className="w-full justify-center"
              onClick={() => navigate("/submit")}
            >
              Start a Submission
            </Button>
          </div>
        </div>
      </main>
    </div>
  )
}