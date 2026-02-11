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
   The development server will start on `http://localhost:4200` and automatically open in your browser.

   **Note**: If the browser doesn't open automatically, manually navigate to `http://localhost:4200` after the server starts.

3. Stop the development server:
   - Press `Ctrl+C` in the terminal where the server is running
   - On Mac, use `Cmd+C`
   - The server will gracefully shut down and release port 4200

## Build

### Development Build
```bash
npm run build
```
This creates a production-ready build in the `dist/` directory.

### Production Build
```bash
npm run build -- --configuration production
```
This creates an optimized production build with minification and tree-shaking.

### Watch Mode (for development)
```bash
npm run watch
```
This builds the project and watches for file changes, automatically rebuilding when files are modified.

## Troubleshooting

### localhost:4200 Not Opening

If `http://localhost:4200` is not opening, try these steps:

1. **Check if the server is running**:
   - Look for a message in your terminal saying "Application bundle generation complete" or "Compiled successfully"
   - You should see: `✔ Compiled successfully` followed by the local and network URLs

2. **Check for compilation errors**:
   - Look for error messages in the terminal
   - Fix any TypeScript or build errors before the server can start

3. **Port 4200 might be in use**:
   - Try using a different port: `npm start -- --port 4201`
   - Or kill the process using port 4200:
     - Windows: `netstat -ano | findstr :4200` then `taskkill /PID <PID> /F`
     - Or use: `npx kill-port 4200`

4. **Manually open the browser**:
   - Even if auto-open doesn't work, copy the URL from the terminal and paste it in your browser
   - The terminal will show: `Local: http://localhost:4200/`

5. **Check firewall/antivirus**:
   - Some firewalls block localhost connections
   - Try temporarily disabling to test

6. **Clear cache and reinstall**:
   ```bash
   rm -rf node_modules package-lock.json
   npm install
   npm start
   ```

7. **Check Node.js version**:
   ```bash
   node --version  # Should be 18 or higher
   npm --version   # Should be 9 or higher
   ```

## Viewing Logs

### Server/Compilation Logs
The terminal where you ran `npm start` shows:
- **Compilation status**: Success messages, errors, and warnings
- **Build progress**: File compilation and bundling information
- **Server URL**: The local and network URLs where the app is running
- **File changes**: Automatic rebuild notifications when you save files

### Browser Console Logs
To view runtime logs and errors:
1. **Open Developer Tools**:
   - Press `F12` or `Ctrl+Shift+I` (Windows/Linux)
   - Press `Cmd+Option+I` (Mac)
   - Or right-click the page → "Inspect"

2. **Console Tab**: Shows:
   - JavaScript errors and warnings
   - `console.log()` outputs
   - Runtime exceptions
   - Angular error messages

3. **Network Tab**: Shows:
   - API calls to the backend
   - Request/response details
   - HTTP status codes
   - Request timing and errors

### Common Log Locations
- **TypeScript/Build Errors**: Terminal (where `npm start` is running)
- **Runtime Errors**: Browser Console (F12 → Console tab)
- **API Errors**: Browser Network tab (F12 → Network tab) or Console
- **Angular Errors**: Browser Console (usually prefixed with `NG`)

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
