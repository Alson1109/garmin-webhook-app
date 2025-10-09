# 🚀 Garmin Developer Portal - URL Update Guide

## 📋 Overview

This guide covers updating the Garmin Developer Portal configuration after migrating from Railway to Debi servers. **This is a critical step** - webhooks will not work until these URLs are updated.

---

## ⚠️ CRITICAL TIMING

**⚠️ IMPORTANT:** Update these URLs only **AFTER** your Debi server is fully deployed and tested. Garmin will immediately start sending webhooks to the new URLs.

**Recommended Timeline:**
1. Deploy and test on Debi servers ✅
2. Verify HTTPS and webhooks work ✅
3. **THEN** update Garmin Developer Portal
4. Monitor for incoming data

---

## 🔐 Accessing Garmin Developer Portal

### **Step 1: Login to Garmin Connect IQ**
1. Go to: **https://developer.garmin.com/**
2. Click **"Sign In"**
3. Use your Garmin developer account credentials

### **Step 2: Navigate to Your Application**
1. Click **"Manage Apps"** or **"My Apps"**
2. Find your Garmin Health API application
3. Click on the application name to edit

---

## 🔗 URL Updates Required

### **Current URLs (Railway):**
```
Webhook URL:
https://garmin-webhook-app-production.up.railway.app/api/garmin/dailies

OAuth Redirect URL:
https://garmin-webhook-app-production.up.railway.app/api/garmin/auth/callback

OAuth Login URL:
https://garmin-webhook-app-production.up.railway.app/api/garmin/auth/login
```

### **New URLs (Debi Server):**
```
Webhook URL:
https://garmin-api.debi.com/api/garmin/dailies

OAuth Redirect URL:
https://garmin-api.debi.com/api/garmin/auth/callback

OAuth Login URL:
https://garmin-api.debi.com/api/garmin/auth/login
```

**⚠️ Replace `garmin-api.debi.com` with your actual Debi domain**

---

## 📋 Step-by-Step Update Process

### **Phase 1: Pre-Update Verification**

#### 1.1 Test Debi Server Endpoints
```bash
# Test webhook endpoint
curl -X POST https://garmin-api.debi.com/api/garmin/dailies \
  -H "Content-Type: application/json" \
  -d '{"test": "webhook"}' \
  -v

# Expected response: {"status":"success","message":"Dailies push processed successfully"}
```

```bash
# Test OAuth login endpoint
curl -I https://garmin-api.debi.com/api/garmin/auth/login

# Expected: HTTP 302 redirect to Garmin OAuth
```

```bash
# Test SSL certificate
curl -I https://garmin-api.debi.com

# Expected: HTTP 200 with valid SSL
```

#### 1.2 Backup Current Configuration
- Take screenshots of current Garmin Developer Portal settings
- Document current webhook and OAuth URLs
- Note application ID and consumer keys

### **Phase 2: Update Garmin Developer Portal**

#### 2.1 Update Webhook Configuration
1. **Navigate to:** Application Settings → Data Fields/Web Services
2. **Find:** "Push URL" or "Webhook URL" field
3. **Change from:** `https://garmin-webhook-app-production.up.railway.app/api/garmin/dailies`
4. **Change to:** `https://garmin-api.debi.com/api/garmin/dailies`
5. **Click:** Save/Update

#### 2.2 Update OAuth Redirect URLs
1. **Navigate to:** Application Settings → OAuth Settings
2. **Find:** "Redirect URIs" or "Callback URLs" section
3. **Remove:** `https://garmin-webhook-app-production.up.railway.app/api/garmin/auth/callback`
4. **Add:** `https://garmin-api.debi.com/api/garmin/auth/callback`
5. **Click:** Save/Update

#### 2.3 Update Application Information (if needed)
1. **Navigate to:** Application Information
2. **Update:** Application URL to point to your Debi domain
3. **Update:** Privacy Policy URL (if applicable)
4. **Update:** Terms of Service URL (if applicable)

### **Phase 3: Verification & Testing**

#### 3.1 Test OAuth Flow
```bash
# 1. Visit login URL
https://garmin-api.debi.com/api/garmin/auth/login

# 2. Should redirect to Garmin OAuth page
# 3. After login, should redirect back to your callback URL
# 4. Should show success message or redirect to your app
```

#### 3.2 Test Webhook Reception
```bash
# Monitor Debi server logs for incoming webhooks
sudo journalctl -u garmin-api -f

# Look for messages like:
# "Received dailies push from Garmin"
# "Webhook processed successfully"
```

#### 3.3 Generate Test Activity (if possible)
- Walk/exercise with Garmin device
- Sync device with Garmin Connect app
- Check if webhook receives data within 30 minutes

---

## 🔄 URL Configuration Examples

### **Example 1: Subdomain Configuration**
```
Domain: garmin-api.debi.com
Webhook: https://garmin-api.debi.com/api/garmin/dailies
OAuth: https://garmin-api.debi.com/api/garmin/auth/callback
```

### **Example 2: Path-based Configuration**
```
Domain: api.debi.com
Webhook: https://api.debi.com/garmin/api/garmin/dailies
OAuth: https://api.debi.com/garmin/api/garmin/auth/callback
```

### **Example 3: Main Domain Configuration**
```
Domain: debi.com
Webhook: https://debi.com/api/garmin/dailies
OAuth: https://debi.com/api/garmin/auth/callback
```

---

## 🚨 Troubleshooting Common Issues

### **Issue 1: Webhook URL Validation Fails**
**Symptoms:** Garmin Developer Portal shows "Invalid webhook URL"

**Solutions:**
```bash
# Check HTTPS is working
curl -I https://garmin-api.debi.com

# Check webhook endpoint responds
curl -X POST https://garmin-api.debi.com/api/garmin/dailies \
  -H "Content-Type: application/json" \
  -d '{}'

# Verify SSL certificate is valid
openssl s_client -connect garmin-api.debi.com:443 -servername garmin-api.debi.com
```

### **Issue 2: OAuth Redirect Fails**
**Symptoms:** Users get "Invalid redirect URI" error

**Solutions:**
- Verify exact URL spelling in Garmin Developer Portal
- Check for trailing slashes (should NOT have trailing slash)
- Ensure HTTPS is used (HTTP will not work)

### **Issue 3: Webhooks Not Received**
**Symptoms:** No webhook data arriving at Debi server

**Solutions:**
```bash
# Check application logs
sudo journalctl -u garmin-api -f

# Verify webhook endpoint responds
curl -X POST https://garmin-api.debi.com/api/garmin/dailies \
  -H "Content-Type: application/json" \
  -d '{"test":"data"}'

# Check firewall allows port 443
sudo ufw status
```

### **Issue 4: SSL Certificate Issues**
**Symptoms:** Garmin cannot connect to webhook URL

**Solutions:**
```bash
# Check SSL certificate validity
curl -I https://garmin-api.debi.com

# Test SSL configuration
openssl s_client -connect garmin-api.debi.com:443

# Verify certificate chain
curl --verbose https://garmin-api.debi.com 2>&1 | grep -i certificate
```

---

## 📊 Monitoring After URL Update

### **Immediate Checks (First Hour)**
```bash
# Monitor application logs for webhook activity
sudo journalctl -u garmin-api -f | grep -i "dailies\|webhook\|garmin"

# Check for OAuth attempts
sudo tail -f /var/log/nginx/garmin-api.access.log | grep "auth"

# Monitor error logs
sudo tail -f /var/log/nginx/garmin-api.error.log
```

### **Daily Monitoring (First Week)**
```bash
# Check webhook success rate
grep "Received dailies push" /opt/debi/garmin-integration/logs/application.log | wc -l

# Check OAuth success rate
grep "OAuth callback successful" /opt/debi/garmin-integration/logs/application.log | wc -l

# Monitor SSL certificate expiry
sudo certbot certificates
```

---

## 🔄 Rollback Plan

### **If New URLs Don't Work:**

#### Immediate Rollback (5 minutes)
1. **Revert Garmin Developer Portal URLs** back to Railway
2. **Keep Debi server running** for debugging
3. **Users continue with Railway** while fixing issues

#### Gradual Migration
1. **Update only webhook URL first** (keep OAuth on Railway)
2. **Test webhook reception** on Debi
3. **Update OAuth URLs** only after webhooks work
4. **Monitor both systems** during transition

---

## ✅ URL Update Verification Checklist

**Before URL Update:**
- [ ] Debi server deployed and running
- [ ] HTTPS working with valid SSL certificate
- [ ] Webhook endpoint returns success response
- [ ] OAuth login endpoint redirects properly
- [ ] Database connection working
- [ ] Application logs showing no errors

**During URL Update:**
- [ ] Screenshot current Garmin Developer Portal settings
- [ ] Update webhook URL in Garmin portal
- [ ] Update OAuth redirect URL in Garmin portal
- [ ] Save/apply changes in Garmin portal
- [ ] Test OAuth flow immediately

**After URL Update:**
- [ ] OAuth flow works end-to-end
- [ ] Webhook endpoint receiving test data
- [ ] Application logs show successful webhook processing
- [ ] No SSL or HTTPS errors in logs
- [ ] Real user activity data flowing within 30 minutes

---

## 📞 Support Information

### **Garmin Developer Support:**
- **Developer Forums:** https://forums.garmin.com/developer/
- **Support Email:** ConnectIQSupport@garmin.com
- **Documentation:** https://developer.garmin.com/health-api/

### **Critical Information for Support:**
- **Application ID:** [Your Garmin app ID]
- **Consumer Key:** [Your consumer key - first 4 characters only]
- **Webhook URL:** Your new Debi server URL
- **Issue Description:** Specific error messages or behavior

**URL updates are critical for webhook functionality - test thoroughly!** 🎯