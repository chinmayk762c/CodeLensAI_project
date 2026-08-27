import { useEffect, useState } from 'react'
import './App.css'

interface HealthResponse {
  status: string
  service: string
  timestamp: string
}

function App() {
  const [health, setHealth] = useState<HealthResponse | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    fetch('http://localhost:8080/api/health')
      .then((res) => {
        if (!res.ok) throw new Error(`Backend responded with ${res.status}`)
        return res.json()
      })
      .then((data: HealthResponse) => setHealth(data))
      .catch((err: Error) => setError(err.message))
  }, [])

  return (
    <div style={{ padding: '2rem', fontFamily: 'monospace' }}>
      <h1>CodeLens AI</h1>
      <h2>Milestone 0: Foundation Check</h2>

      {error && (
        <p style={{ color: 'red' }}>
          ❌ Backend unreachable: {error}
        </p>
      )}

      {health && (
        <div style={{ color: 'green' }}>
          <p>✅ Backend Status: {health.status}</p>
          <p>✅ Service: {health.service}</p>
          <p>✅ Timestamp: {health.timestamp}</p>
        </div>
      )}

      {!health && !error && <p>Connecting to backend...</p>}
    </div>
  )
}

export default App