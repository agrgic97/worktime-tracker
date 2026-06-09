# worktime-tracker

Web application for entering monthly work hours and generating a consultant timesheet PDF.

## Stack

- Frontend: Vue 3, Vite, TypeScript
- Backend: Spring Boot 4, Maven, Java 21
- PDF generation: Apache PDFBox

## Run locally

Backend:

```bash
cd backend
mvn spring-boot:run
```

By default the app is protected with HTTP Basic Auth using `admin` /
`change-me`. Override the credentials before running locally or in production:

```bash
BASIC_AUTH_USERNAME=my-user BASIC_AUTH_PASSWORD=my-secret mvn spring-boot:run
```

Frontend:

```bash
cd frontend
npm install
npm run dev
```

The frontend proxies `/api` requests to the backend at `http://localhost:8080`.

For production builds the frontend calls the API through the same origin by
default. To point it at a separate backend during a frontend-only build, set
`VITE_API_BASE_URL`, for example:

```bash
VITE_API_BASE_URL=https://api.example.com npm run build
```

## Production deployment

The recommended production shape is a single container:

- Vue/Vite is built into static files.
- Spring Boot serves those static files and the `/api/...` endpoints.
- The app listens on port `8080`.
- `/actuator/health` is available for platform health checks.

Build and run locally:

```bash
docker compose up --build
```

Then open `http://localhost:8080`.

For a hosted deployment, push this repository to GitHub and connect it to a
container-capable platform such as Fly.io, Render, Railway, or a VPS running
Docker Compose behind Caddy/Traefik for HTTPS.

Set `BASIC_AUTH_USERNAME` and `BASIC_AUTH_PASSWORD` in the deployment
environment. `/actuator/health` remains publicly reachable for platform health
checks.

## PDF behavior

The generated PDF follows the attached DB consultant billing layout:

- one A4 page
- month, company, consultant name, daily hours, project, and total
- consultant and approver signature sections are rendered empty
- no signature images or digital signatures are embedded
