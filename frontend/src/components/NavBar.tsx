import { LogOut } from "lucide-react"
import { Button } from "@/components/ui/button"
import { useAuth } from "@/context/AuthContext"
import { useNavigate } from "react-router-dom"
import Logo from "@/components/Logo"

export default function NavBar() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate("/login")
  }

  return (
    <header className="border-b border-border px-6 py-3 flex items-center justify-between bg-background">
      <button onClick={() => navigate("/")} className="cursor-pointer">
        <Logo size="sm" />
      </button>

      {user && (
        <div className="flex items-center gap-4">
          <span className="text-sm text-muted-foreground hidden sm:inline">
            {user.fullName}
          </span>
          <Button
            onClick={handleLogout}
            variant="outline"
            size="sm"
            className="gap-1.5"
          >
            <LogOut size={14} />
            Log Out
          </Button>
        </div>
      )}
    </header>
  )
}