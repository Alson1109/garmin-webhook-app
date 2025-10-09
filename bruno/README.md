# 🧪 Bruno API Testing Guide

## 📋 Overview

This Bruno collection contains all the endpoints for testing your Garmin Health API integration. The collection is designed to test both the current Railway deployment and can be easily modified for your future Debi server deployment.

---

## 🚀 Current Status

- ✅ **Railway Deployment:** https://garmin-webhook-app-production.up.railway.app
- ✅ **Webhook Reception:** Active (2,146 steps confirmed)
- ✅ **OAuth Flow:** Functional
- ⚠️ **Data Retrieval:** Some endpoints may return 500 errors (database configuration issue)

---

## 📦 Bruno Collection Contents

### **1. Health Check** (`1-health-check.bru`)
- **Purpose:** Verify application is running
- **Method:** GET
- **Expected:** 200 OK with `{"status": "UP"}`

### **2. OAuth Login** (`2-oauth-login.bru`)
- **Purpose:** Test OAuth flow initiation
- **Method:** GET
- **Expected:** 302 redirect to Garmin login
- **Note:** Open URL in browser to complete flow

### **3. Test Webhook** (`3-test-webhook.bru`)
- **Purpose:** Test webhook endpoint reception
- **Method:** POST
- **Expected:** 200 OK with success message
- **Body:** Simple test JSON

### **4. Get Step Data** (`4-get-steps.bru`)
- **Purpose:** Retrieve user step data
- **Method:** GET
- **Expected:** Step count and daily metrics
- **Status:** May return 500 error (known issue)

### **5. Get Daily Summary** (`5-get-daily-summary.bru`)
- **Purpose:** Retrieve comprehensive daily data
- **Method:** GET
- **Expected:** Full health metrics
- **Status:** May return 500 error (known issue)

### **6. Real Garmin Webhook Data** (`6-real-webhook-data.bru`)
- **Purpose:** Test with actual Garmin data structure
- **Method:** POST
- **Body:** Real webhook data (2,146 steps)
- **Expected:** 200 OK with success message

---

## 🔧 Setup Instructions

### **Step 1: Install Bruno**
```bash
# Install Bruno API client
# Visit: https://usebruno.com/
# Download and install for your platform
```

### **Step 2: Open Collection**
1. Launch Bruno
2. Click "Open Collection"
3. Navigate to: `C:\Users\FEPLALSO\Desktop\GarminDataFetch\garmin\bruno`
4. Select the `bruno` folder

### **Step 3: Configure Environment (Optional)**
```javascript
// Create environment variables in Bruno
{
  "baseUrl": "https://garmin-webhook-app-production.up.railway.app",
  "testUserId": "7c3af2e0-f55b-4526-b128-e91bbf749049"
}
```

---

## 🧪 Testing Scenarios

### **Scenario 1: Basic Connectivity Test**
```
1. Run "Health Check" → Should return 200 OK
2. Run "Test Webhook" → Should return success message
3. If both pass: ✅ Application is accessible
```

### **Scenario 2: OAuth Flow Test**
```
1. Run "OAuth Login" → Should return 302 redirect
2. Copy the redirect URL
3. Open in browser
4. Complete Garmin login flow
5. Verify callback works
```

### **Scenario 3: Data Reception Test**
```
1. Run "Real Garmin Webhook Data" → Should return 200 OK
2. Check application logs for processing message
3. Verify data was received and processed
```

### **Scenario 4: Data Retrieval Test**
```
1. Run "Get Step Data" → May return 500 error (known)
2. Run "Get Daily Summary" → May return 500 error (known)
3. Note: Webhook reception works, retrieval has database issues
```

---

## ✅ Expected Results

### **Working Endpoints:**
- ✅ **Health Check:** Always returns 200 OK
- ✅ **OAuth Login:** Returns 302 redirect to Garmin
- ✅ **Test Webhook:** Returns 200 OK with success message
- ✅ **Real Webhook Data:** Returns 200 OK with success message

### **Known Issues:**
- ⚠️ **Get Step Data:** May return 500 Internal Server Error
- ⚠️ **Get Daily Summary:** May return 500 Internal Server Error
- **Reason:** Database configuration issue (not critical for webhook reception)

---

## 🔄 Testing for Debi Server Migration

### **Update Base URLs**
When testing your Debi server deployment, update the URLs in each `.bru` file:

**From:**
```
https://garmin-webhook-app-production.up.railway.app
```

**To:**
```
https://garmin-api.debi.com
```

### **Modified Testing Flow**
1. **Deploy to Debi servers**
2. **Update Bruno URLs**
3. **Run complete test suite**
4. **Verify all endpoints work**
5. **Update Garmin Developer Portal**

---

## 📊 Test Results Interpretation

### **✅ Success Indicators:**
- Health check returns `{"status": "UP"}`
- Webhook tests return `{"status": "success"}`
- OAuth login redirects to Garmin (302 status)
- No network connection errors

### **⚠️ Warning Indicators:**
- Step data endpoints return 500 errors
- Database connection issues
- Timeout errors on requests

### **❌ Failure Indicators:**
- Health check fails (application down)
- Webhook returns errors (integration broken)
- OAuth redirects fail (configuration issue)
- SSL certificate errors

---

## 🔧 Troubleshooting

### **Connection Issues:**
```bash
# Test connectivity manually
curl -I https://garmin-webhook-app-production.up.railway.app/actuator/health
```

### **SSL Issues:**
```bash
# Verify SSL certificate
curl --insecure -I https://garmin-webhook-app-production.up.railway.app
```

### **Webhook Issues:**
```bash
# Test webhook manually
curl -X POST https://garmin-webhook-app-production.up.railway.app/api/garmin/dailies \
  -H "Content-Type: application/json" \
  -d '{"test": "data"}'
```

---

## 📞 Usage Tips

### **For Development:**
- Use the collection to test new features
- Modify request bodies to test edge cases
- Monitor application logs while running tests

### **For Integration:**
- Test OAuth flow before deploying to production
- Verify webhook reception before updating Garmin portal
- Use real webhook data structure for accurate testing

### **For Migration:**
- Test each endpoint during Debi server deployment
- Verify SSL certificates work with Bruno
- Confirm database issues are resolved

**Bruno testing will work perfectly for your Garmin API! 🧪**