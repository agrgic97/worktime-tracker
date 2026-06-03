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

Frontend:

```bash
cd frontend
npm install
npm run dev
```

The frontend expects the backend at `http://localhost:8080`.

## PDF behavior

The generated PDF follows the attached DB consultant billing layout:

- one A4 page
- month, company, consultant name, daily hours, project, and total
- consultant and approver signature sections are rendered empty
- no signature images or digital signatures are embedded
