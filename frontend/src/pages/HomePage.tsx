import { useAuth } from "@/context/AuthContext"
import { Button } from "@/components/ui/button"
import { useNavigate } from "react-router-dom"

export default function HomePage() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate("/login")
  }

  return (
        <div className="min-h-screen bg-background text-foreground p-8 font-mono">
      <h1 className="text-2xl mb-2">CodeLens AI</h1>
      {user ? (
        <>
          <p className="text-zinc-400 mb-4">
            Logged in as {user.fullName} ({user.email})
          </p>
                    <div className="flex gap-2">
            <Button onClick={() => navigate("/submit")}>
              New Submission
            </Button>
            <Button onClick={handleLogout} variant="outline">
              Log Out
            </Button>
          </div>
        </>
      ) : (
        <p className="text-zinc-400">Not logged in.</p>
      )}
    </div>
  )
}
