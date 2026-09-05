FROM node:22-alpine AS build
WORKDIR /app
COPY package.json ./
RUN npm install
COPY angular.json tsconfig.json tsconfig.app.json ./
COPY src ./src
RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/dual-write-lab-front/browser /usr/share/nginx/html
EXPOSE 80
