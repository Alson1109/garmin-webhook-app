# 🔗 API Reference - Garmin Integration

## Base URL
```
https://garmin-webhook-app-production.up.railway.app
```

## 🔐 Authentication Endpoints

### 1. Initiate Garmin OAuth
```http
GET /api/garmin/auth/login
```
**Purpose:** Start Garmin OAuth flow
**Response:** Redirects to Garmin login page
**Usage:** Direct users here to connect their Garmin account

### 2. OAuth Callback
```http
GET /api/garmin/auth/callback?code={code}&state={state}
```
**Purpose:** Handles Garmin OAuth response (automatic)
**Response:** Success/error page

## 📊 Data Endpoints

### 3. Get Step Data
```http
GET /api/garmin/steps?testUserId={userId}
```

**Parameters:**
- `testUserId` (required): User identifier

**Response Example:**
```json
{
  "totalSteps": 2146,
  "records": [
    {
      "userId": "7c3af2e0-f55b-4526-b128-e91bbf749049",
      "date": "2025-10-07",
      "steps": 2146,
      "activeKilocalories": 71,
      "bmrKilocalories": 1282,
      "distanceInMeters": 1709.0,
      "durationInSeconds": 50100,
      "activeTimeInSeconds": 2667
    }
  ]
}
```

### 4. Get Daily Summary
```http
GET /api/garmin/dailies?testUserId={userId}
```

**Parameters:**
- `testUserId` (required): User identifier

**Response:** Full daily health metrics including heart rate, stress, etc.

## 🔔 Webhook Endpoints (Internal)

### 5. Receive Garmin Webhooks
```http
POST /api/garmin/dailies
```
**Purpose:** Receives real-time data from Garmin (internal use)
**Content-Type:** `application/json`

## 📱 JavaScript Integration Examples

### Connect to Garmin
```javascript
function connectGarmin() {
  // Redirect user to Garmin OAuth
  window.location.href = 'https://garmin-webhook-app-production.up.railway.app/api/garmin/auth/login';
}
```

### Fetch Step Data
```javascript
async function getSteps(userId) {
  try {
    const response = await fetch(
      `https://garmin-webhook-app-production.up.railway.app/api/garmin/steps?testUserId=${userId}`
    );
    const data = await response.json();
    return data.totalSteps;
  } catch (error) {
    console.error('Error fetching steps:', error);
    return 0;
  }
}
```

### Real-time Step Display
```javascript
async function displayGarminSteps(userId) {
  const steps = await getSteps(userId);
  document.getElementById('step-count').textContent = `${steps} steps today`;
}
```

## 🐍 Python Integration Example
```python
import requests

def get_garmin_steps(user_id):
    url = f"https://garmin-webhook-app-production.up.railway.app/api/garmin/steps"
    params = {"testUserId": user_id}
    
    response = requests.get(url, params=params)
    if response.status_code == 200:
        data = response.json()
        return data.get("totalSteps", 0)
    return 0
```

## ☁️ React/Next.js Integration
```jsx
import { useState, useEffect } from 'react';

function GarminSteps({ userId }) {
  const [steps, setSteps] = useState(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchSteps = async () => {
      try {
        const response = await fetch(
          `/api/proxy/garmin/steps?testUserId=${userId}`
        );
        const data = await response.json();
        setSteps(data.totalSteps);
      } catch (error) {
        console.error('Failed to fetch steps:', error);
      } finally {
        setLoading(false);
      }
    };

    if (userId) {
      fetchSteps();
    }
  }, [userId]);

  if (loading) return <div>Loading steps...</div>;

  return (
    <div className="garmin-steps">
      <h3>{steps.toLocaleString()} Steps Today</h3>
      <p>Data from Garmin Connect</p>
    </div>
  );
}
```

## 🔄 Error Handling

### Common Responses
```json
// Success
{
  "totalSteps": 2146,
  "records": [...],
  "status": "success"
}

// User not found
{
  "error": "User not found",
  "status": "error"
}

// Server error
{
  "error": "Internal server error",
  "status": "error"
}
```

## 🚦 Rate Limits
- No specific rate limits currently
- Recommended: Cache responses for 5-10 minutes
- Garmin data updates every 15-30 minutes

## 🔒 Security Notes
- API currently uses test user IDs
- For production: Implement proper user authentication
- Consider API keys for server-to-server calls
- Webhook endpoint validates Garmin signatures (internal)