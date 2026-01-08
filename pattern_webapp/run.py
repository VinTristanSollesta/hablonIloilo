#!/usr/bin/env python3
"""
Simple script to run the Flask application
"""
from app import app

if __name__ == '__main__':
    print("Starting Pattern Detection & Generation Web Application...")
    print("Open your browser and navigate to: http://localhost:5000")
    print("Press Ctrl+C to stop the server")
    app.run(debug=True, host='0.0.0.0', port=5000)

