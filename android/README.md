# Teleconizer Android Application

## Project Structure

```
android/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/teleconizer/app/
│       │   ├── TeleconizerApplication.kt
│       │   ├── data/
│       │   │   ├── api/
│       │   │   │   ├── ApiClient.kt
│       │   │   │   └── TeleconizerApi.kt
│       │   │   ├── database/
│       │   │   │   ├── EmergencyContactDao.kt
│       │   │   │   └── TeleconizerDatabase.kt
│       │   │   ├── model/
│       │   │   │   └── DataModels.kt
│       │   │   └── repository/
│       │   │       └── TeleconizerRepository.kt
│       │   ├── service/
│       │   │   └── AlarmService.kt
│       │   └── ui/
│       │       ├── login/
│       │       │   ├── LoginActivity.kt
│       │       │   └── LoginViewModel.kt
│       │       └── main/
│       │           ├── MainActivity.kt
│       │           └── MainViewModel.kt
│       └── res/
│           ├── layout/
│           │   ├── activity_login.xml
│           │   └── activity_main.xml
│           ├── values/
│           │   ├── strings.xml
│           │   ├── colors.xml
│           │   └── themes.xml
│           └── drawable/
│               ├── ic_email.xml
│               ├── ic_lock.xml
│               ├── ic_person.xml
│               ├── ic_phone.xml
│               └── ic_emergency.xml
├── build.gradle
└── README.md
```

## Setup Instructions

### 1. Prerequisites
- Android Studio Arctic Fox or later
- Android SDK API 24 or higher
- Google Maps API Key

### 2. Google Maps Setup
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing one
3. Enable Maps SDK for Android
4. Create credentials (API Key)
5. Replace `YOUR_GOOGLE_MAPS_API_KEY` in `AndroidManifest.xml` with your actual API key

### 3. Network Configuration
Update the server URL in `ApiClient.kt`:
```kotlin
private const val BASE_URL = "http://YOUR_LAPTOP_IP:5000/"
```

### 4. Build and Run
1. Open project in Android Studio
2. Sync Gradle files
3. Build and run on device or emulator

## Features

### Login Activity
- Email and password authentication
- Input validation
- Simple credential checking (demo purposes)

### Main Dashboard
- Real-time sensor status display
- Google Maps integration with satellite view
- Emergency contact management
- Alarm notification system
- Automatic data polling every 5 seconds

### Google Maps Integration
```kotlin
mapView.getMapAsync { googleMap ->
    val patientLocation = LatLng(lat, lon)
    googleMap.addMarker(MarkerOptions().position(patientLocation).title("Patient Location"))
    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(patientLocation, 16f))
    googleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
}
```

### Room Database
- Stores emergency contact information
- Local data persistence
- Flow-based reactive updates

### Alarm Service
- Emergency notifications
- Vibration alerts
- Persistent notification for danger status

## Permissions Required
- `INTERNET` - API communication
- `ACCESS_FINE_LOCATION` - GPS location access
- `CALL_PHONE` - Emergency phone calls
- `VIBRATE` - Alarm vibrations
- `WAKE_LOCK` - Keep device awake during emergencies

## API Integration
The app communicates with the Flask backend server:
- GET `/api/data` - Fetch latest sensor data
- GET `/api/status` - Server health check

## Testing
1. Start the Flask backend server
2. Configure ESP32 to send data to server
3. Update server IP in Android app
4. Test login and dashboard functionality
5. Verify Google Maps integration
6. Test emergency alarm system

