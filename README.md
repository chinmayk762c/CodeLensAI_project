# CodeLens AI

CodeLens AI is a web-based code review and optimization tool for Java. Users write or paste code into an in-browser editor, run it, and receive a combined analysis from static analysis tools and an AI model, including a quality score, a list of issues, a complexity-aware optimized version of the code, and test coverage reporting.

## Features

- **In-browser code editor and runner** — write Java code and execute it directly, with console output streamed back in real time.
- **Static analysis** — every submission is checked with PMD, Checkstyle, and SpotBugs.
- **AI code review** — a Google Gemini-powered review provides a plain-English summary, time/space complexity analysis, and additional issues the static tools miss.
- **Quality scoring** — a 100-point score weighted by issue severity and category, checked against a configurable pass/fail threshold.
- **Test coverage** — optional JUnit test submission with line-coverage reporting via JaCoCo.
- **Automatic documentation** — generates Javadoc comments for submitted code on demand.
- **Submission history** — a dashboard of past submissions with status, score, search, and delete.
- **Report exports** — download any analysis report as CSV or PDF.
- **Repository-wide analysis** — analyze every Java file in a public GitHub repository in one pass.
- **GitHub pull request integration** — a GitHub App that automatically analyzes pull requests and posts a pass/fail check with a summary.
- **Containerized deployment** — the full stack runs via Docker Compose.

## Tech Stack

**Backend:** Java 21, Spring Boot, Spring Security (JWT authentication), Spring AI, PostgreSQL
**Static analysis tools:** PMD, Checkstyle, SpotBugs, JaCoCo
**AI:** Google Gemini, via Spring AI
**Frontend:** React, TypeScript, Vite, Tailwind CSS, shadcn/ui, Monaco Editor
**Infrastructure:** Docker, Docker Compose, nginx, GitHub Apps API

## Architecture

```
React (Vite)  <-- nginx proxy -->  Spring Boot API  <-->  PostgreSQL
                                        |
                                        |--> PMD / Checkstyle / SpotBugs
                                        |--> JaCoCo (test coverage)
                                        |--> Google Gemini (AI review)
                                        |--> GitHub Apps API (PR checks)
```

Every submission — whether from the editor, a repository scan, or a pull request — goes through the same pipeline: static analysis, compilation, optional coverage, AI review, and scoring, then is persisted as a report.

## Getting Started

### Prerequisites

- Java 21 JDK (a full JDK is required, as the application compiles submitted code at runtime)
- Node.js 20+
- PostgreSQL 16 (or use Docker Compose, which provisions it automatically)
- A Google AI Studio API key (free, from https://aistudio.google.com/apikey)

### Environment Variables

| Variable | Description |
|---|---|
| `DB_PASSWORD` | PostgreSQL password |
| `JWT_SECRET` | Secret key for signing authentication tokens (32+ characters) |
| `GEMINI_API_KEY` | Google AI Studio API key |
| `GITHUB_APP_ID` | GitHub App ID (required only for pull request integration) |
| `GITHUB_WEBHOOK_SECRET` | GitHub App webhook secret (required only for pull request integration) |

### Running with Docker

1. Create a `.env` file in the project root with the variables listed above.
2. From the project root, run:
   ```
   docker compose up --build
   ```
3. Open `http://localhost` in a browser.

### Running Locally

Backend:
```
cd backend
./mvnw spring-boot:run
```
Runs on `http://localhost:8080`. Requires a local PostgreSQL instance with a `codelens_db` database.

Frontend:
```
cd frontend
npm install
npm run dev
```
Runs on `http://localhost:5173` and proxies API requests to the backend.

## GitHub Pull Request Integration (Optional)

1. Register a GitHub App with a webhook pointing to `/api/github/webhook`, repository permissions for Contents (read), Pull Requests (read), and Checks (read/write), and a subscription to the "Pull request" event.
2. Download the generated private key and place it at `backend/github-app-private-key.pem`.
3. Install the app on a repository. Opening a pull request will trigger automatic analysis of the changed files.

## Project Structure

```
CodeLensAI/
├── backend/            Spring Boot API
│   └── src/main/java/com/codelens/backend/
│       ├── entity/        Data models
│       ├── repository/    Data access layer
│       ├── service/       Business logic
│       ├── analysis/      Static analysis and compiler integrations
│       ├── ai/            AI review integration
│       ├── github/        GitHub App authentication and webhook handling
│       ├── security/      Authentication and authorization
│       └── controller/    REST API endpoints
├── frontend/           React application
│   └── src/
│       ├── pages/          Application pages
│       ├── components/     Shared UI components
│       ├── lib/             API client functions
│       └── context/         Application state
└── docker-compose.yml
```

## License

This is a personal project and is not currently licensed for reuse.
