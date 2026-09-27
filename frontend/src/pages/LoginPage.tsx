import { useState, type FormEvent } from "react"
import { useNavigate, Link } from "react-router-dom"
import { Button } from "@/components/ui/button"
import PasswordInput from "@/components/PasswordInput"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Card, CardContent, CardHeader } from "@/components/ui/card"
import { loginUser } from "@/lib/authApi"
import { useAuth } from "@/context/AuthContext"
import Logo from "@/components/Logo"
import { LogIn } from "lucide-react"

export default function LoginPage() {
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const { login } = useAuth()
  const navigate = useNavigate()

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setLoading(true)

    try {
      const res = await loginUser(email, password)
      login(res.token, { fullName: res.fullName, email: res.email })
      navigate("/")
    } catch (err) {
      setError(err instanceof Error ? err.message : "Something went wrong")
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-background text-foreground font-mono p-4">
      <div className="mb-8">
        <Logo />
      </div>

      <Card className="w-full max-w-sm">
        <CardHeader>
          <p className="text-sm text-muted-foreground">Log in to your account</p>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
                        <div className="space-y-2">
              <Label htmlFor="email">Email</Label>
              <Input
                id="email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="password">Password</Label>
              <PasswordInput
                id="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </div>

            {error && (
              <p className="text-sm text-red-400">{error}</p>
            )}

            <Button type="submit" className="w-full gap-1.5" disabled={loading}>
              <LogIn size={14} />
              {loading ? "Logging in..." : "Log In"}
            </Button>
          </form>

          <p className="text-sm text-muted-foreground mt-4 text-center">
            Don't have an account?{" "}
            <Link to="/register" className="text-foreground underline underline-offset-2">
              Register
            </Link>
          </p>
        </CardContent>
      </Card>
    </div>
  )
}
