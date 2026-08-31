import { Terminal } from "lucide-react"

export default function Logo({ size = "md" }: { size?: "sm" | "md" }) {
  const iconSize = size === "sm" ? 18 : 22
  const textSize = size === "sm" ? "text-sm" : "text-lg"

  return (
    <div className="flex items-center gap-2">
      <div className="bg-primary text-primary-foreground rounded-md p-1.5 flex items-center justify-center">
        <Terminal size={iconSize} strokeWidth={2.5} />
      </div>
      <span className={`${textSize} font-mono font-semibold tracking-tight`}>
        CodeLens<span className="text-muted-foreground">AI</span>
      </span>
    </div>
  )
}