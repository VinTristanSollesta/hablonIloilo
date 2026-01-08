# Quick Start Guide

## Installation

1. **Install Python 3.8+** (if not already installed)

2. **Install dependencies:**
   ```bash
   cd pattern_webapp
   pip install -r requirements.txt
   ```

## Running the Application

### Option 1: Using run.py
```bash
python run.py
```

### Option 2: Direct execution
```bash
python app.py
```

### Option 3: Using Flask CLI
```bash
export FLASK_APP=app.py
flask run
```

The application will start on `http://localhost:5000`

## Using the Application

### 1. Pattern Detection

1. Open the web interface in your browser
2. Click on "Detect Patterns" tab
3. Upload an image file (PNG, JPG, GIF, WEBP)
4. Wait for analysis - the app will show:
   - Pattern type detected
   - Description
   - Dominant colors extracted using k-means

### 2. Pattern Generation

1. Click on "Generate Patterns" tab
2. Select pattern type (geometric, organic, tessellation, gradient)
3. Choose a color palette (or use custom colors)
4. Set k-clusters (number of color clusters, 2-10)
5. Set image dimensions
6. Click "Generate Pattern"
7. The app creates a pattern using k-means clustering with your palette colors

### 3. Connecting Android Palettes

**Method 1: Direct Database Access**
1. Copy `color_groups.db` from your Android device to `android_db/` folder
2. The app will automatically load palettes on startup

**Method 2: JSON Upload**
1. Export palettes from Android app as JSON
2. Go to "Manage Palettes" tab
3. Click "Upload Palette JSON"
4. Select your JSON file

**Sample JSON format:**
```json
{
  "name": "My Palette",
  "colors": [16711680, 65280, 255]
}
```
(Colors are Android ARGB integers)

## Troubleshooting

### Database Not Found
- If you see "Database file not found", the app will use sample palettes
- Copy your Android database to `android_db/color_groups.db` or upload palettes via JSON

### Import Errors
- Make sure all dependencies are installed: `pip install -r requirements.txt`
- Check Python version: `python --version` (should be 3.8+)

### Image Upload Issues
- Maximum file size: 16MB
- Supported formats: PNG, JPG, JPEG, GIF, WEBP
- Check that `uploads/` directory exists and is writable

### Pattern Generation Not Working
- Ensure you have at least 2 colors in your palette
- K-clusters should be between 2 and 10
- Image dimensions should be reasonable (200-2000 pixels)

## Features Overview

- **Pattern Detection**: Uses computer vision (OpenCV) to detect geometric shapes, repeating patterns, textures
- **K-Means Clustering**: Extracts dominant colors and quantizes patterns to match your palette
- **Pattern Generation**: Creates new patterns using your saved color palettes
- **Modern UI**: Beautiful, responsive web interface

## Next Steps

- Experiment with different k-cluster values
- Try different pattern types
- Upload your own color palettes
- Use detected colors to generate new patterns

