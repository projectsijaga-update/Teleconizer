# Teleconizer - Patient Monitoring System

A comprehensive patient monitoring system consisting of an ESP32 sensor module, Python Flask backend server, and Android Kotlin application for real-time patient monitoring with emergency alerts.

## System Architecture

```
ESP32 (MPU6050 + GPS Neo-7) 
    ↓ HTTP POST
Python Flask Server (Laptop/PC)
    ↓ HTTP GET
Android App (Teleconizer)
```

## Components

### 1. ESP32 Sensor Module
- **MPU6050**: Accelerometer and gyroscope for motion detection
- **GPS Neo-7**: Location tracking
- **WiFi**: HTTP communication with backend server
- **Sends**: JSON data with status, coordinates, and API key

### 2. Backend Server (Python Flask)
- **Location**: Runs on laptop/PC
- **Port**: 5000
- **Features**:
  - Receives ESP32 sensor data via HTTP POST
  - Stores latest patient data
  - Provides API endpoints for Android app
  - API key validation
  - CORS enabled for mobile access

### 3. Android Application (Teleconizer)
- **Language**: Kotlin
- **Architecture**: MVVM with Jetpack components
- **Features**:
  - Login authentication
  - Real-time sensor status display
  - Google Maps integration with satellite view
  - Emergency contact management
  - Alarm notification system
  - Automatic data polling

## Project Structure

```
Teleconizer/
├── backend/
│   ├── server.py              # Flask server
│   ├── requirements.txt       # Python dependencies
│   └── README.md             # Backend documentation
├── android/
│   ├── app/
│   │   ├── build.gradle       # App-level dependencies
│   │   └── src/main/
│   │       ├── AndroidManifest.xml
│   │       ├── java/com/teleconizer/app/
│   │       │   ├── data/       # API, Database, Models
│   │       │   ├── service/    # Alarm service
│   │       │   └── ui/         # Activities and ViewModels
│   │       └── res/            # Layouts, strings, drawables
│   ├── build.gradle           # Project-level configuration
│   └── README.md              # Android documentation
└── README.md                  # This file
```

## Data Flow

### 1. ESP32 → Server
```json
POST http://192.168.x.x:5000/api/data
{
  "status": "danger",
  "lat": -6.2,
  "lon": 106.8,
  "api_key": "SECRET_ESP32_KEY"
}
```

### 2. Server → Android App
```json
GET http://192.168.x.x:5000/api/data?api_key=SECRET_ESP32_KEY
{
  "status": "danger",
  "lat": -6.2,
  "lon": 106.8,
  "timestamp": "2024-01-15T10:30:00"
}
```

### 3. Android App Response
- **Normal Status**: Green indicator, "Status: Safe"
- **Danger Status**: 
  - Red indicator, "Status: Immediate Help Needed"
  - Emergency alarm notification
  - Device vibration
  - Google Maps zoom to patient location
  - Option to call emergency contact

## Key Features

### Backend Server
- ✅ RESTful API endpoints
- ✅ API key authentication
- ✅ CORS support for mobile access
- ✅ Error handling and logging
- ✅ In-memory data storage

### Android Application
- ✅ Material Design UI
- ✅ MVVM architecture with ViewModel and LiveData
- ✅ Room database for local storage
- ✅ Retrofit for API communication
- ✅ Google Maps SDK integration
- ✅ Satellite view with patient location marker
- ✅ Emergency notification system
- ✅ Phone call integration
- ✅ Real-time data polling

### Google Maps Integration
```kotlin
mapView.getMapAsync { googleMap ->
    val patientLocation = LatLng(lat, lon)
    googleMap.addMarker(MarkerOptions().position(patientLocation).title("Patient Location"))
    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(patientLocation, 16f))
    googleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
}
```

## Setup Instructions

### Backend Server Setup
1. Install Python dependencies:
   ```bash
   cd backend
   pip install -r requirements.txt
   ```

2. Run the server:
   ```bash
   python server.py
   ```

3. Server will start on `http://0.0.0.0:5000`

### Android App Setup
1. Open `android/` folder in Android Studio
2. Get Google Maps API key from [Google Cloud Console](https://console.cloud.google.com/)
3. Replace `YOUR_GOOGLE_MAPS_API_KEY` in `AndroidManifest.xml`
4. Update server IP in `ApiClient.kt`
5. Build and run on device/emulator

### ESP32 Integration
Update ESP32 code to send data to your laptop's IP:
```cpp
const char* serverUrl = "http://192.168.1.100:5000/api/data";
```

## API Endpoints

### POST /api/data
- **Purpose**: Receive sensor data from ESP32
- **Headers**: `Content-Type: application/json`
- **Body**: JSON with status, lat, lon, api_key
- **Response**: Success/error message

### GET /api/data
- **Purpose**: Return latest data to Android app
- **Query**: `api_key=SECRET_ESP32_KEY`
- **Response**: Latest sensor data JSON

### GET /api/status
- **Purpose**: Health check endpoint
- **Response**: Server status and latest data

## Security Notes

- API key validation on all requests
- CORS enabled for mobile access
- Input validation and error handling
- No sensitive data stored permanently

## Testing

1. Start Flask backend server
2. Configure ESP32 with correct server IP
3. Launch Android app
4. Test login (any email/password with 6+ chars)
5. Verify real-time data updates
6. Test emergency alarm system
7. Verify Google Maps integration

## Future Enhancements

- Database persistence for historical data
- User authentication with JWT tokens
- Push notifications via FCM
- Multiple patient support
- Data analytics dashboard
- Mobile app for caregivers

## Troubleshooting

### Common Issues
1. **Network Connection**: Ensure all devices are on same WiFi network
2. **Google Maps**: Verify API key and enable Maps SDK
3. **Permissions**: Grant location and phone permissions
4. **Server Access**: Check firewall settings on laptop

### Debug Tips
- Check server logs for API requests
- Use Android Studio logcat for app debugging
- Verify ESP32 serial output for data transmission
- Test API endpoints with Postman/curl

## License

This project is created for educational and demonstration purposes.

# Teleconizer
