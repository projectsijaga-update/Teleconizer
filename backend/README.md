# Teleconizer Backend Server

## Setup Instructions

1. Install Python dependencies:
```bash
pip install -r requirements.txt
```

2. Run the server:
```bash
python server.py
```

3. The server will start on `http://0.0.0.0:5000`

## API Endpoints

### POST /api/data
Receives sensor data from ESP32
- **Headers**: `Content-Type: application/json`
- **Body**:
```json
{
  "status": "danger",
  "lat": -6.2,
  "lon": 106.8,
  "api_key": "SECRET_ESP32_KEY"
}
```

### GET /api/data
Returns latest sensor data to Android app
- **Query Parameters**: `api_key=SECRET_ESP32_KEY`
- **Response**:
```json
{
  "status": "danger",
  "lat": -6.2,
  "lon": 106.8,
  "timestamp": "2024-01-15T10:30:00"
}
```

### GET /api/status
Health check endpoint
- **Response**:
```json
{
  "status": "running",
  "timestamp": "2024-01-15T10:30:00",
  "latest_data": {...}
}
```

## Configuration

- **API Key**: `SECRET_ESP32_KEY`
- **Port**: `5000`
- **Host**: `0.0.0.0` (accessible from local network)

## ESP32 Integration

The ESP32 should send HTTP POST requests to:
```
http://YOUR_LAPTOP_IP:5000/api/data
```

Example ESP32 code snippet:
```cpp
// Replace with your laptop's IP address
const char* serverUrl = "http://192.168.1.100:5000/api/data";

void sendSensorData() {
  HTTPClient http;
  http.begin(serverUrl);
  http.addHeader("Content-Type", "application/json");
  
  String jsonData = "{";
  jsonData += "\"status\":\"danger\",";
  jsonData += "\"lat\":" + String(gpsLat, 6) + ",";
  jsonData += "\"lon\":" + String(gpsLon, 6) + ",";
  jsonData += "\"api_key\":\"SECRET_ESP32_KEY\"";
  jsonData += "}";
  
  int httpResponseCode = http.POST(jsonData);
  http.end();
}
```

