# 🏗️ Garmin Integration - Migration to Debi Servers

## 📋 Migration Overview

**Current Status:** Garmin integration working on Railway.app  
**Target:** Move complete system to Debi infrastructure  
**Migration Time:** 1-2 weeks  
**Complexity:** Medium (requires SSL setup and Garmin configuration updates)

---

## 🎯 Migration Objectives

✅ **Full control** over Garmin integration infrastructure  
✅ **No external dependencies** (Railway, third-party hosting)  
✅ **Data stays within Debi ecosystem**  
✅ **Cost optimization** (no ongoing Railway fees)  
✅ **Custom scaling and monitoring**  

---

## ⚠️ Critical Requirements

### **1. HTTPS/SSL Certificate (MANDATORY)**
- Garmin **requires HTTPS on port 443** for webhooks
- SSL certificate must be valid and trusted
- Self-signed certificates will NOT work

### **2. Domain Configuration**
- Dedicated domain or subdomain for Garmin endpoints
- Examples: `garmin-api.debi.com` or `api.debi.com/garmin`

### **3. Port 443 Access**
- Garmin webhooks only work on standard HTTPS port (443)
- Configure reverse proxy if application runs on different port

---

## 📊 Pre-Migration Checklist

### **Infrastructure Requirements**
- [ ] **SSL Certificate** acquired and installed
- [ ] **Domain/subdomain** configured and DNS updated
- [ ] **Port 443** accessible and configured
- [ ] **Java 17+** installed on Debi servers
- [ ] **Maven 3.6+** for building application
- [ ] **Database** (PostgreSQL recommended, H2 for development)
- [ ] **Reverse proxy** (Nginx/Apache) configured if needed

### **Access Requirements**
- [ ] **Garmin Developer Account** access (to update webhook URLs)
- [ ] **Environment variables** from current Railway deployment
- [ ] **Database migration** strategy defined
- [ ] **Backup plan** in case of issues

---

## 🔧 Step-by-Step Migration Process

### **Phase 1: Environment Setup (Days 1-3)**

#### 1.1 Server Prerequisites
```bash
# Install Java 17
sudo apt update
sudo apt install openjdk-17-jdk

# Verify Java installation
java -version
# Should show: openjdk version "17.x.x"

# Install Maven
sudo apt install maven
mvn -version
```

#### 1.2 SSL Certificate Setup
```bash
# Option A: Let's Encrypt (Free)
sudo apt install certbot python3-certbot-nginx
sudo certbot --nginx -d garmin-api.debi.com

# Option B: Commercial SSL Certificate
# Follow your SSL provider's installation guide
```

#### 1.3 Database Setup
```bash
# Install PostgreSQL
sudo apt install postgresql postgresql-contrib

# Create database for Garmin data
sudo -u postgres createdb garmin_data
sudo -u postgres createuser garmin_user --pwprompt
```

### **Phase 2: Application Deployment (Days 4-7)**

#### 2.1 Build Application
```bash
# Clone/copy the source code to Debi servers
cd /opt/debi/garmin-integration

# Build the application
mvn clean package -DskipTests

# Verify JAR file created
ls -la target/garmin-data-fetch-*.jar
```

#### 2.2 Environment Configuration
```bash
# Create environment file
sudo nano /opt/debi/garmin-integration/.env
```

```env
# Garmin API Configuration
GARMIN_CONSUMER_KEY=your_garmin_consumer_key
GARMIN_CONSUMER_SECRET=your_garmin_consumer_secret

# Database Configuration (PostgreSQL)
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/garmin_data
SPRING_DATASOURCE_USERNAME=garmin_user
SPRING_DATASOURCE_PASSWORD=your_secure_password
SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver

# JPA Configuration
SPRING_JPA_HIBERNATE_DDL_AUTO=update
SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.PostgreSQLDialect

# Server Configuration
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=production

# Logging
LOGGING_LEVEL_IO_FERMION_AZ_HEALTH_GARMIN=INFO
```

#### 2.3 Systemd Service Setup
```bash
# Create systemd service file
sudo nano /etc/systemd/system/garmin-api.service
```

```ini
[Unit]
Description=Garmin Data Fetch API
After=network.target

[Service]
Type=simple
User=debi-service
WorkingDirectory=/opt/debi/garmin-integration
ExecStart=/usr/bin/java -jar /opt/debi/garmin-integration/target/garmin-data-fetch-*.jar
Restart=always
RestartSec=10
Environment=SPRING_PROFILES_ACTIVE=production
EnvironmentFile=/opt/debi/garmin-integration/.env

[Install]
WantedBy=multi-user.target
```

```bash
# Enable and start service
sudo systemctl daemon-reload
sudo systemctl enable garmin-api
sudo systemctl start garmin-api
sudo systemctl status garmin-api
```

#### 2.4 Nginx Reverse Proxy Configuration
```bash
# Create Nginx configuration
sudo nano /etc/nginx/sites-available/garmin-api
```

```nginx
server {
    listen 443 ssl http2;
    server_name garmin-api.debi.com;

    ssl_certificate /etc/letsencrypt/live/garmin-api.debi.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/garmin-api.debi.com/privkey.pem;

    # SSL Configuration
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-RSA-AES256-GCM-SHA512:DHE-RSA-AES256-GCM-SHA512:ECDHE-RSA-AES256-GCM-SHA384:DHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers off;

    # Proxy to Spring Boot application
    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # Webhook timeout settings
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Health check endpoint
    location /actuator/health {
        proxy_pass http://localhost:8080/actuator/health;
        access_log off;
    }
}

# Redirect HTTP to HTTPS
server {
    listen 80;
    server_name garmin-api.debi.com;
    return 301 https://$server_name$request_uri;
}
```

```bash
# Enable site and restart Nginx
sudo ln -s /etc/nginx/sites-available/garmin-api /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
```

### **Phase 3: Garmin Configuration Update (Days 8-10)**

#### 3.1 Update Garmin Developer Portal
```
1. Login to Garmin Developer Portal
2. Navigate to your application
3. Update Webhook URLs:
   
   OLD: https://garmin-webhook-app-production.up.railway.app/api/garmin/dailies
   NEW: https://garmin-api.debi.com/api/garmin/dailies

4. Update OAuth Redirect URLs:
   
   OLD: https://garmin-webhook-app-production.up.railway.app/api/garmin/auth/callback
   NEW: https://garmin-api.debi.com/api/garmin/auth/callback

5. Save changes and test configuration
```

#### 3.2 Test Webhook Connectivity
```bash
# Test webhook endpoint from external
curl -X POST https://garmin-api.debi.com/api/garmin/dailies \
  -H "Content-Type: application/json" \
  -d '{"test": "webhook"}'

# Should return: {"status":"success","message":"Dailies push processed successfully"}
```

### **Phase 4: Data Migration & Testing (Days 11-14)**

#### 4.1 Database Migration (if needed)
```sql
-- Export data from Railway/H2 database
-- Import into PostgreSQL on Debi servers
-- Verify data integrity
```

#### 4.2 End-to-End Testing
```bash
# Test OAuth flow
curl https://garmin-api.debi.com/api/garmin/auth/login

# Test step data endpoint
curl "https://garmin-api.debi.com/api/garmin/steps?testUserId=7c3af2e0-f55b-4526-b128-e91bbf749049"

# Monitor logs
sudo journalctl -u garmin-api -f
```

#### 4.3 User Acceptance Testing
- [ ] Users can connect Garmin accounts
- [ ] Step data flows correctly
- [ ] Webhooks receive real-time updates
- [ ] OAuth redirects work properly
- [ ] Data persists in Debi database

---

## 🔄 Rollback Plan

### **If Migration Issues Occur:**

#### Immediate Rollback (< 30 minutes)
```bash
# Revert Garmin Developer Portal URLs back to Railway
# Users continue using Railway endpoints while fixing Debi issues
```

#### Gradual Migration
```bash
# Option: Run both systems in parallel
# Gradually move users from Railway to Debi
# Monitor both systems until migration complete
```

---

## 📊 Post-Migration Monitoring

### **Health Checks**
```bash
# Application health
curl https://garmin-api.debi.com/actuator/health

# SSL certificate expiry
sudo certbot certificates

# Service status
sudo systemctl status garmin-api

# Database connectivity
sudo -u postgres psql -d garmin_data -c "SELECT COUNT(*) FROM garmin_user_dailies_summary;"
```

### **Log Monitoring**
```bash
# Application logs
sudo journalctl -u garmin-api -f

# Nginx access logs
sudo tail -f /var/log/nginx/access.log

# SSL certificate renewal
sudo systemctl status certbot.timer
```

---

## 💰 Cost Analysis

### **Railway Costs (Current):**
- Monthly hosting: ~$5-10
- Ongoing operational dependency

### **Debi Server Migration:**
- SSL certificate: $0 (Let's Encrypt) or $50-200/year (commercial)
- Infrastructure: Existing Debi servers (no additional cost)
- Development time: 40-80 hours
- **Long-term savings:** $60-120/year

---

## 🚨 Common Migration Issues & Solutions

### **Issue 1: SSL Certificate Problems**
```bash
# Symptoms: Garmin webhooks fail with SSL errors
# Solution: Verify SSL configuration
sudo nginx -t
curl -I https://garmin-api.debi.com
```

### **Issue 2: Port 443 Not Accessible**
```bash
# Symptoms: Connection refused errors
# Solution: Check firewall rules
sudo ufw allow 443/tcp
sudo netstat -tlnp | grep :443
```

### **Issue 3: Database Connection Issues**
```bash
# Symptoms: Application fails to start
# Solution: Verify database configuration
sudo -u postgres psql -d garmin_data -c "\dt"
```

### **Issue 4: Garmin OAuth Redirect Fails**
```bash
# Symptoms: Users get OAuth errors
# Solution: Verify redirect URLs in Garmin Developer Portal match exactly
```

---

## ✅ Migration Success Criteria

**Migration Complete When:**
- [ ] Application runs on Debi servers with HTTPS
- [ ] Garmin webhooks receive data successfully
- [ ] OAuth flow works end-to-end
- [ ] Step data API returns correct information
- [ ] Database persists data properly
- [ ] SSL certificate auto-renewal configured
- [ ] Monitoring and alerting in place
- [ ] Railway dependency eliminated

**Timeline:** 14 days from start to full production migration

---

## 📞 Migration Support

### **Pre-Migration Questions:**
1. What is the target domain for Garmin API? (e.g., `garmin-api.debi.com`)
2. Which SSL certificate provider will be used?
3. What database system should be used? (PostgreSQL recommended)
4. Who has access to Garmin Developer Portal for URL updates?

### **Critical Migration Windows:**
- **Garmin Developer Portal Update:** Coordinate with Garmin team
- **DNS Changes:** Allow 24-48 hours for propagation
- **SSL Certificate:** Ensure valid before updating Garmin URLs

**The migration is complex but achievable. Full control over infrastructure is worth the effort!** 🚀