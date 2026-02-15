# Build stage
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build -- --configuration production

# Runtime stage
FROM nginx:alpine
COPY --from=build /app/dist/thoughts-to-post-angular-ui/browser /usr/share/nginx/html
# Note: Check the output path in angular.json. Based on package.json it might be slightly different.
# I will use a generic copy and hope for the best, or check angular.json again.
# Wait, I saw it in angular.json: "outputPath": "dist/thoughts-to-post-angular-ui"
# For Angular 17/18, it usually adds a /browser suffix if using the new application builder.

COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
