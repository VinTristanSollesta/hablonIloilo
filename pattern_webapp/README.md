# Pattern Detection & Generation Web Application

A Python Flask web application that detects patterns in images and generates new patterns using k-means clustering with colors from saved Android app palettes.

## Features

- **Pattern Detection**: Upload images to detect pattern types (geometric, organic, tessellation, etc.)
- **Pattern Generation**: Generate new patterns using k-means clustering with colors from your saved palettes
- **Palette Management**: View and manage color palettes from your Android app
- **K-Clustering**: Use k-means clustering to create patterns with specified number of color clusters

## Installation

1. Install Python 3.8 or higher

2. Install dependencies:
```bash
pip install -r requirements.txt
```

3. Create necessary directories:
```bash
mkdir -p uploads static/outputs android_db
```

## Usage

### Running the Application

```bash
python app.py
```

The application will start on `http://localhost:5000`

### Connecting to Android Database

To use palettes from your Android app:

1. Copy the `color_groups.db` file from your Android device:
   - Location: `/data/data/com.example.habloniloilo/databases/color_groups.db`
   - You can use ADB: `adb pull /data/data/com.example.habloniloilo/databases/color_groups.db android_db/`

2. The app will automatically detect and load palettes from `android_db/color_groups.db`

3. Alternatively, you can upload palette JSON files through the web interface

### Pattern Detection

1. Go to the "Detect Patterns" tab
2. Upload an image (PNG, JPG, GIF, WEBP up to 16MB)
3. The app will analyze the image and show:
   - Pattern type (geometric, organic, tessellation, etc.)
   - Description
   - Dominant colors extracted using k-means clustering

### Pattern Generation

1. Go to the "Generate Patterns" tab
2. Select:
   - Pattern type (geometric, organic, tessellation, gradient)
   - Color palette from your saved palettes
   - Number of k-clusters (2-10)
   - Image dimensions
3. Click "Generate Pattern"
4. The app will create a new pattern using k-means clustering with your selected palette colors

### Palette Management

1. Go to the "Manage Palettes" tab
2. View all available palettes
3. Upload new palettes as JSON files
4. Palettes are automatically loaded from the Android database if available

## API Endpoints

### GET `/api/palettes`
Get all palettes from the database

**Query Parameters:**
- `db_path` (optional): Path to database file

### POST `/api/palettes/upload`
Upload a new palette

**Request Body:**
```json
{
  "name": "My Palette",
  "colors": [16711680, 65280, 255]  // Android color integers (ARGB)
}
```

### POST `/api/detect`
Detect patterns in an uploaded image

**Request:** Multipart form data with `file` field

**Response:**
```json
{
  "success": true,
  "pattern_type": "geometric_lines",
  "description": "Geometric pattern with linear elements",
  "colors_detected": [[255, 0, 0], [0, 255, 0], ...],
  "output_image": "/static/outputs/detected_image.png"
}
```

### POST `/api/generate`
Generate a pattern using k-means clustering

**Request Body:**
```json
{
  "pattern_type": "geometric",
  "palette_id": 1,
  "k_clusters": 5,
  "width": 800,
  "height": 600
}
```

**Response:**
```json
{
  "success": true,
  "pattern_type": "geometric",
  "colors_used": [[255, 0, 0], [0, 255, 0], ...],
  "output_image": "/static/outputs/generated_geometric_1.png"
}
```

## Project Structure

```
pattern_webapp/
├── app.py                 # Main Flask application
├── database_helper.py     # Android database reader
├── pattern_detector.py    # Pattern detection logic
├── pattern_generator.py   # Pattern generation with k-clustering
├── requirements.txt      # Python dependencies
├── templates/
│   └── index.html        # Web interface
├── static/
│   ├── style.css         # Styles
│   └── script.js         # Client-side JavaScript
├── uploads/              # Uploaded images (created automatically)
└── static/outputs/       # Generated images (created automatically)
```

## Technologies Used

- **Flask**: Web framework
- **OpenCV**: Image processing and pattern detection
- **scikit-learn**: K-means clustering
- **Pillow (PIL)**: Image manipulation
- **NumPy**: Numerical operations

## Notes

- The app uses k-means clustering to extract dominant colors from images
- Patterns are generated using k-means to quantize colors to match your palette
- Android colors are stored as ARGB integers and converted to RGB tuples
- If the Android database is not found, sample palettes are provided

## License

This project is part of the Hablon Iloilo Android application ecosystem.

