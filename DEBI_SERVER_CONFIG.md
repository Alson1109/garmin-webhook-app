# 🔧 Debi Server Configuration Templates

## 📋 Quick Reference for System Administrators

This document contains all the configuration files and commands needed for migrating the Garmin integration to Debi servers.

---

## 🐧 System Requirements

### **Minimum Server Specifications:**
- **OS:** Ubuntu 20.04+ / CentOS 8+ / RHEL 8+
- **RAM:** 2GB minimum, 4GB recommended
- **CPU:** 2 cores minimum
- **Storage:** 10GB minimum
- **Network:** Port 443 accessible from internet

### **Software Dependencies:**
```bash
# Ubuntu/Debian installation commands
sudo apt update
sudo apt install -y openjdk-17-jdk maven nginx postgresql postgresql-contrib certbot python3-certbot-nginx curl wget unzip
```

---

## 🔒 SSL Certificate Setup

### **Option A: Let's Encrypt (Recommended - Free)**
```bash
# Install Certbot
sudo apt install certbot python3-certbot-nginx

# Obtain SSL certificate
sudo certbot --nginx -d garmin-api.debi.com

# Auto-renewal setup
sudo systemctl enable certbot.timer
sudo systemctl start certbot.timer

# Test auto-renewal
sudo certbot renew --dry-run
```

### **Option B: Commercial SSL Certificate**
```bash
# Generate CSR (Certificate Signing Request)
sudo openssl req -new -newkey rsa:2048 -nodes -keyout garmin-api.debi.com.key -out garmin-api.debi.com.csr

# Follow your SSL provider's instructions
# Install certificate files to:
# - /etc/ssl/certs/garmin-api.debi.com.crt
# - /etc/ssl/private/garmin-api.debi.com.key
```

---

## 🗄️ Database Configuration

### **PostgreSQL Setup**
```bash
# Install PostgreSQL
sudo apt install postgresql postgresql-contrib

# Start and enable PostgreSQL
sudo systemctl start postgresql
sudo systemctl enable postgresql

# Create database and user
sudo -u postgres psql << EOF
CREATE DATABASE garmin_data;
CREATE USER garmin_user WITH ENCRYPTED PASSWORD 'SecurePassword123!';
GRANT ALL PRIVILEGES ON DATABASE garmin_data TO garmin_user;
ALTER USER garmin_user CREATEDB;
\q
EOF

# Test database connection
psql -h localhost -U garmin_user -d garmin_data -c "SELECT version();"
```

### **Database Configuration File**
```bash
# Edit PostgreSQL configuration
sudo nano /etc/postgresql/14/main/postgresql.conf
```
```ini
# Add/modify these lines:
listen_addresses = 'localhost'
port = 5432
max_connections = 100
shared_buffers = 256MB
```

```bash
# Edit authentication file
sudo nano /etc/postgresql/14/main/pg_hba.conf
```
```ini
# Add this line:
local   garmin_data     garmin_user                     md5
host    garmin_data     garmin_user     127.0.0.1/32    md5
```

```bash
# Restart PostgreSQL
sudo systemctl restart postgresql
```

---

## 🚀 Application Deployment

### **Directory Structure**
```bash
# Create application directory
sudo mkdir -p /opt/debi/garmin-integration
sudo mkdir -p /opt/debi/garmin-integration/logs
sudo mkdir -p /opt/debi/garmin-integration/config

# Create service user
sudo useradd -r -s /bin/false debi-service
sudo chown -R debi-service:debi-service /opt/debi/garmin-integration
```

### **Application Build Script**
```bash
#!/bin/bash
# File: /opt/debi/garmin-integration/build.sh

set -e

echo "Building Garmin Integration Application..."

# Navigate to source directory
cd /opt/debi/garmin-integration

# Clean and build
mvn clean package -DskipTests

# Backup previous version
if [ -f "current.jar" ]; then
    mv current.jar "backup-$(date +%Y%m%d-%H%M%S).jar"
fi

# Deploy new version
cp target/garmin-data-fetch-*.jar current.jar

# Set permissions
chown debi-service:debi-service current.jar
chmod 755 current.jar

echo "Build completed successfully!"
echo "Restart service with: sudo systemctl restart garmin-api"
```

### **Environment Configuration**
```bash
# File: /opt/debi/garmin-integration/.env
GARMIN_CONSUMER_KEY=your_garmin_consumer_key_here
GARMIN_CONSUMER_SECRET=your_garmin_consumer_secret_here

# Database Configuration
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/garmin_data
SPRING_DATASOURCE_USERNAME=garmin_user
SPRING_DATASOURCE_PASSWORD=SecurePassword123!
SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver

# JPA Configuration
SPRING_JPA_HIBERNATE_DDL_AUTO=update
SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.PostgreSQLDialect
SPRING_JPA_SHOW_SQL=false

# Server Configuration
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=production

# Logging Configuration
LOGGING_LEVEL_IO_FERMION_AZ_HEALTH_GARMIN=INFO
LOGGING_LEVEL_ROOT=WARN
LOGGING_FILE_PATH=/opt/debi/garmin-integration/logs/application.log

# Security
SERVER_ERROR_INCLUDE_STACKTRACE=never
SERVER_ERROR_INCLUDE_MESSAGE=never
```

### **Systemd Service Configuration**
```bash
# File: /etc/systemd/system/garmin-api.service
[Unit]
Description=Garmin Data Fetch API Service
After=network.target postgresql.service
Requires=postgresql.service

[Service]
Type=simple
User=debi-service
Group=debi-service
WorkingDirectory=/opt/debi/garmin-integration
ExecStart=/usr/bin/java -Xmx1g -Xms512m -jar /opt/debi/garmin-integration/current.jar
ExecStop=/bin/kill -SIGTERM $MAINPID
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=garmin-api

# Environment
Environment=SPRING_PROFILES_ACTIVE=production
EnvironmentFile=/opt/debi/garmin-integration/.env

# Security
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ReadWritePaths=/opt/debi/garmin-integration/logs
ProtectHome=true

[Install]
WantedBy=multi-user.target
```

---

## 🌐 Nginx Configuration

### **Main Nginx Configuration**
```bash
# File: /etc/nginx/sites-available/garmin-api.debi.com
upstream garmin_backend {
    server 127.0.0.1:8080;
    keepalive 32;
}

# Rate limiting
limit_req_zone $binary_remote_addr zone=garmin_api:10m rate=10r/s;

server {
    listen 443 ssl http2;
    server_name garmin-api.debi.com;

    # SSL Configuration
    ssl_certificate /etc/letsencrypt/live/garmin-api.debi.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/garmin-api.debi.com/privkey.pem;
    
    # SSL Security
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-RSA-AES256-GCM-SHA512:DHE-RSA-AES256-GCM-SHA512:ECDHE-RSA-AES256-GCM-SHA384:DHE-RSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-SHA384;
    ssl_prefer_server_ciphers off;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;
    
    # Security Headers
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Frame-Options DENY always;
    add_header X-Content-Type-Options nosniff always;
    add_header X-XSS-Protection "1; mode=block" always;

    # Logging
    access_log /var/log/nginx/garmin-api.access.log;
    error_log /var/log/nginx/garmin-api.error.log;

    # Gzip Compression
    gzip on;
    gzip_vary on;
    gzip_min_length 1000;
    gzip_types application/json application/javascript text/css text/javascript text/plain text/xml;

    # Main proxy configuration
    location / {
        limit_req zone=garmin_api burst=20 nodelay;
        
        proxy_pass http://garmin_backend;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
        
        # Timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
        
        # Buffer settings
        proxy_buffer_size 4k;
        proxy_buffers 8 4k;
        proxy_busy_buffers_size 8k;
    }

    # Health check endpoint (no rate limiting)
    location /actuator/health {
        proxy_pass http://garmin_backend/actuator/health;
        access_log off;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Webhook endpoint (higher timeout for Garmin)
    location /api/garmin/dailies {
        limit_req zone=garmin_api burst=5 nodelay;
        
        proxy_pass http://garmin_backend/api/garmin/dailies;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # Extended timeouts for webhooks
        proxy_connect_timeout 120s;
        proxy_send_timeout 120s;
        proxy_read_timeout 120s;
    }
}

# HTTP to HTTPS redirect
server {
    listen 80;
    server_name garmin-api.debi.com;
    return 301 https://$server_name$request_uri;
}
```

### **Enable Nginx Configuration**
```bash
# Enable site
sudo ln -s /etc/nginx/sites-available/garmin-api.debi.com /etc/nginx/sites-enabled/

# Test configuration
sudo nginx -t

# Restart Nginx
sudo systemctl restart nginx
```

---

## 📊 Monitoring & Logging

### **Log Rotation Configuration**
```bash
# File: /etc/logrotate.d/garmin-api
/opt/debi/garmin-integration/logs/*.log {
    daily
    missingok
    rotate 30
    compress
    delaycompress
    notifempty
    create 644 debi-service debi-service
    postrotate
        systemctl reload garmin-api
    endscript
}
```

### **Health Check Script**
```bash
#!/bin/bash
# File: /opt/debi/garmin-integration/health-check.sh

HEALTH_URL="https://garmin-api.debi.com/actuator/health"
LOG_FILE="/opt/debi/garmin-integration/logs/health-check.log"

echo "[$(date)] Checking application health..." >> $LOG_FILE

# Check HTTP status
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" $HEALTH_URL)

if [ $HTTP_STATUS -eq 200 ]; then
    echo "[$(date)] ✅ Application is healthy (HTTP $HTTP_STATUS)" >> $LOG_FILE
else
    echo "[$(date)] ❌ Application is unhealthy (HTTP $HTTP_STATUS)" >> $LOG_FILE
    # Optional: Send alert notification
    systemctl status garmin-api >> $LOG_FILE
fi
```

### **Crontab for Health Checks**
```bash
# Add to crontab with: sudo crontab -e
# Check application health every 5 minutes
*/5 * * * * /opt/debi/garmin-integration/health-check.sh

# Restart application if unhealthy (every hour)
0 * * * * /opt/debi/garmin-integration/auto-restart.sh
```

---

## 🔧 Maintenance Scripts

### **Backup Script**
```bash
#!/bin/bash
# File: /opt/debi/garmin-integration/backup.sh

BACKUP_DIR="/opt/debi/backups/garmin-$(date +%Y%m%d)"
mkdir -p $BACKUP_DIR

# Backup database
sudo -u postgres pg_dump garmin_data > $BACKUP_DIR/database.sql

# Backup application files
cp -r /opt/debi/garmin-integration $BACKUP_DIR/application

# Backup nginx configuration
cp /etc/nginx/sites-available/garmin-api.debi.com $BACKUP_DIR/nginx.conf

# Create archive
cd /opt/debi/backups
tar -czf garmin-backup-$(date +%Y%m%d).tar.gz garmin-$(date +%Y%m%d)
rm -rf garmin-$(date +%Y%m%d)

echo "Backup completed: garmin-backup-$(date +%Y%m%d).tar.gz"
```

### **Deployment Script**
```bash
#!/bin/bash
# File: /opt/debi/garmin-integration/deploy.sh

set -e

echo "Starting Garmin API deployment..."

# Stop application
sudo systemctl stop garmin-api

# Backup current version
/opt/debi/garmin-integration/backup.sh

# Build new version
/opt/debi/garmin-integration/build.sh

# Start application
sudo systemctl start garmin-api

# Wait for startup
sleep 30

# Health check
if curl -f https://garmin-api.debi.com/actuator/health > /dev/null 2>&1; then
    echo "✅ Deployment successful!"
else
    echo "❌ Deployment failed - check logs"
    sudo journalctl -u garmin-api --no-pager -n 50
    exit 1
fi
```

---

## 🚨 Troubleshooting Commands

### **Service Management**
```bash
# Check service status
sudo systemctl status garmin-api

# View logs
sudo journalctl -u garmin-api -f

# Restart service
sudo systemctl restart garmin-api

# Check service configuration
sudo systemctl cat garmin-api
```

### **Network Troubleshooting**
```bash
# Check if port 443 is open
sudo netstat -tlnp | grep :443

# Test SSL certificate
curl -I https://garmin-api.debi.com

# Check Nginx configuration
sudo nginx -t

# Test application directly (bypass Nginx)
curl http://localhost:8080/actuator/health
```

### **Database Troubleshooting**
```bash
# Connect to database
sudo -u postgres psql -d garmin_data

# Check database connections
sudo -u postgres psql -c "SELECT * FROM pg_stat_activity WHERE datname='garmin_data';"

# Check table structure
sudo -u postgres psql -d garmin_data -c "\dt"
```

---

## ✅ Deployment Verification Checklist

- [ ] **SSL Certificate:** Valid and auto-renewing
- [ ] **Database:** PostgreSQL running and accessible
- [ ] **Application:** Spring Boot service running on port 8080
- [ ] **Nginx:** Reverse proxy configured and running on port 443
- [ ] **Firewall:** Port 443 open for HTTPS traffic
- [ ] **Health Check:** `/actuator/health` returns 200 OK
- [ ] **Webhook Test:** POST to `/api/garmin/dailies` returns success
- [ ] **OAuth Test:** GET `/api/garmin/auth/login` redirects properly
- [ ] **Monitoring:** Logs rotating and health checks scheduled
- [ ] **Backup:** Automated backup system in place

**All configuration templates are production-ready!** 🚀