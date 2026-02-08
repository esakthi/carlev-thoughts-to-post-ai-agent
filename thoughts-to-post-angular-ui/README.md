# Thoughts-to-Post Angular UI

Modern Angular 18 frontend for the Thoughts-to-Post AI Agent Platform.

## Features

- **Premium Dark Theme** with glassmorphism and gradient effects
- **Thought Input Form** with platform selection checkboxes
- **Real-time Status Updates** via polling
- **Enriched Content Preview** with approval workflow
- **Responsive Design** for mobile and desktop

## Prerequisites

- Node.js 18+
- npm 9+

## Setup

1. Install dependencies:
   ```bash
   npm install
   ```

2. Start development server:
   ```bash
   npm start
   ```

3. Open browser at `http://localhost:4200`

## Configuration

The API URL is configured in `src/app/services/thoughts.service.ts`:
```typescript
private readonly apiUrl = 'http://localhost:8080/api/thoughts';
```

## Project Structure

```
src/app/
├── components/
│   ├── header/           # Header with navigation
│   ├── thought-input/    # Input form with platform selection
│   └── enriched-content/ # Content display and approval
├── models/               # TypeScript interfaces
├── pages/
│   └── thoughts-page/    # Main page component
└── services/
    └── thoughts.service  # API communication service
```

## TODO

- [ ] Run `npm install` to install dependencies
- [ ] Configure API URL if different from localhost:8080
- [ ] Add user authentication
- [ ] Add history page
- [ ] Add Facebook/Instagram support (once backend supports it)
