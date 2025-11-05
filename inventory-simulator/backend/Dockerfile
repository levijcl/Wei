# Multi-stage build for Node.js backend

FROM node:18-alpine AS builder

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm install --production

# Copy source code
COPY src/ ./src/

# Production stage
FROM node:18-alpine

WORKDIR /app

# Copy from builder
COPY --from=builder /app /app

# Expose port
EXPOSE 3778

# Start application
CMD ["node", "src/app.js"]
