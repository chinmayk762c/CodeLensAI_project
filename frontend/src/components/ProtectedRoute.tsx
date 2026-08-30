import { type ReactNode } from "react"
import { Navigate } from "react-router-dom"
import { useAuth } from "@/context/AuthContext"

export default function ProtectedRoute({ children }: { children: ReactNode }) {
  const { user, isLoading } = useAuth()

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background text-foreground font-mono">
        <p className="text-zinc-400">Loading...</p>
      </div>
    )
  }

  if (!user) {
    return <Navigate to="/login" replace />
  }

  return <>{children}</>
}