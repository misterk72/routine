FROM alpine:latest

# Install SQLite
RUN apk add --no-cache sqlite

# Create data directory
RUN mkdir -p /data

# Set working directory
WORKDIR /data

# Expose port for SQLite server
EXPOSE 3306

# Command to run SQLite server
CMD ["sqlite3", "-line", "/data/health.db"]
