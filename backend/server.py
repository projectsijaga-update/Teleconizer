from flask import Flask, request, jsonify
from flask_cors import CORS
import json
import os
from datetime import datetime

app = Flask(__name__)
CORS(app)  # Enable CORS for Android app access

# Configuration
API_KEY = "SECRET_ESP32_KEY"
PORT = 5000

# In-memory storage for latest sensor data
latest_data = {
    "status": "normal",
    "lat": -6.2,
    "lon": 106.8,
    "timestamp": None
}

@app.route('/api/data', methods=['POST'])
def receive_esp32_data():
    """Receive sensor data from ESP32"""
    try:
        data = request.get_json()
        
        # Validate API key
        if not data or data.get('api_key') != API_KEY:
            return jsonify({"error": "Invalid API key"}), 401
        
        # Validate required fields
        required_fields = ['status', 'lat', 'lon']
        for field in required_fields:
            if field not in data:
                return jsonify({"error": f"Missing field: {field}"}), 400
        
        # Update latest data
        global latest_data
        latest_data = {
            "status": data['status'],
            "lat": float(data['lat']),
            "lon": float(data['lon']),
            "timestamp": datetime.now().isoformat()
        }
        
        print(f"Received ESP32 data: {latest_data}")
        
        return jsonify({
            "message": "Data received successfully",
            "timestamp": latest_data['timestamp']
        }), 200
        
    except Exception as e:
        print(f"Error processing ESP32 data: {str(e)}")
        return jsonify({"error": "Internal server error"}), 500

@app.route('/api/data', methods=['GET'])
def get_latest_data():
    """Return latest sensor data to Android app"""
    try:
        # Validate API key from query parameter
        api_key = request.args.get('api_key')
        if api_key != API_KEY:
            return jsonify({"error": "Invalid API key"}), 401
        
        return jsonify(latest_data), 200
        
    except Exception as e:
        print(f"Error retrieving data: {str(e)}")
        return jsonify({"error": "Internal server error"}), 500

@app.route('/api/status', methods=['GET'])
def get_server_status():
    """Health check endpoint"""
    return jsonify({
        "status": "running",
        "timestamp": datetime.now().isoformat(),
        "latest_data": latest_data
    }), 200

if __name__ == '__main__':
    print(f"Starting Teleconizer Backend Server on port {PORT}")
    print(f"API Key: {API_KEY}")
    print(f"Access endpoints:")
    print(f"  POST http://localhost:{PORT}/api/data")
    print(f"  GET  http://localhost:{PORT}/api/data?api_key={API_KEY}")
    print(f"  GET  http://localhost:{PORT}/api/status")
    
    app.run(host='0.0.0.0', port=PORT, debug=True)

