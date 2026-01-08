from flask import Flask, render_template, request, jsonify, send_from_directory
import os
from werkzeug.utils import secure_filename
import numpy as np
from PIL import Image
from sklearn.cluster import KMeans
import cv2
from pattern_detector import PatternDetector
from pattern_generator import PatternGenerator
from database_helper import DatabaseHelper

app = Flask(__name__)
app.config['UPLOAD_FOLDER'] = 'uploads'
app.config['OUTPUT_FOLDER'] = 'static/outputs'
app.config['MAX_CONTENT_LENGTH'] = 16 * 1024 * 1024  # 16MB max file size
app.config['ALLOWED_EXTENSIONS'] = {'png', 'jpg', 'jpeg', 'gif', 'webp'}

# Create necessary directories
os.makedirs(app.config['UPLOAD_FOLDER'], exist_ok=True)
os.makedirs(app.config['OUTPUT_FOLDER'], exist_ok=True)

# Initialize helpers
db_helper = DatabaseHelper()
pattern_detector = PatternDetector()
pattern_generator = PatternGenerator()

def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in app.config['ALLOWED_EXTENSIONS']

def android_color_to_rgb(color_int):
    """Convert Android color integer (ARGB) to RGB tuple"""
    # Android colors are stored as ARGB integers
    # Extract RGB components
    r = (color_int >> 16) & 0xFF
    g = (color_int >> 8) & 0xFF
    b = color_int & 0xFF
    return (r, g, b)

@app.route('/')
def index():
    """Main page"""
    return render_template('index.html')

@app.route('/api/palettes', methods=['GET'])
def get_palettes():
    """Get all palettes from Android database"""
    try:
        # Try to load from Android database
        db_path = request.args.get('db_path', 'android_db/color_groups.db')
        palettes = db_helper.get_palettes(db_path)
        return jsonify({'success': True, 'palettes': palettes})
    except Exception as e:
        # If database not found, return sample palettes
        return jsonify({
            'success': False,
            'message': str(e),
            'palettes': get_sample_palettes()
        })

@app.route('/api/palettes/upload', methods=['POST'])
def upload_palette():
    """Upload palette data as JSON"""
    try:
        data = request.json
        if 'name' in data and 'colors' in data:
            # Convert colors to RGB if needed
            colors = []
            for color in data['colors']:
                if isinstance(color, int):
                    colors.append(android_color_to_rgb(color))
                elif isinstance(color, list) and len(color) == 3:
                    colors.append(tuple(color))
            
            palette = {
                'id': len(get_sample_palettes()) + 1,
                'name': data['name'],
                'colors': colors
            }
            return jsonify({'success': True, 'palette': palette})
        return jsonify({'success': False, 'message': 'Invalid data'}), 400
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

def get_sample_palettes():
    """Return sample palettes if database is not available"""
    return [
        {
            'id': 1,
            'name': 'Sample Palette 1',
            'colors': [(255, 0, 0), (0, 255, 0), (0, 0, 255), (255, 255, 0), (255, 0, 255)]
        },
        {
            'id': 2,
            'name': 'Sample Palette 2',
            'colors': [(128, 0, 128), (255, 192, 203), (255, 165, 0), (0, 128, 128), (128, 128, 0)]
        }
    ]

@app.route('/api/detect', methods=['POST'])
def detect_pattern():
    """Detect patterns in uploaded image"""
    try:
        if 'file' not in request.files:
            return jsonify({'success': False, 'message': 'No file provided'}), 400
        
        file = request.files['file']
        if file.filename == '':
            return jsonify({'success': False, 'message': 'No file selected'}), 400
        
        if file and allowed_file(file.filename):
            filename = secure_filename(file.filename)
            filepath = os.path.join(app.config['UPLOAD_FOLDER'], filename)
            file.save(filepath)
            
            # Detect patterns
            result = pattern_detector.detect(filepath)
            
            # Save result image
            output_filename = f"detected_{filename}"
            output_path = os.path.join(app.config['OUTPUT_FOLDER'], output_filename)
            result['image'].save(output_path)
            
            return jsonify({
                'success': True,
                'pattern_type': result['pattern_type'],
                'description': result['description'],
                'colors_detected': result['colors'],
                'output_image': f'/static/outputs/{output_filename}'
            })
        
        return jsonify({'success': False, 'message': 'Invalid file type'}), 400
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/generate', methods=['POST'])
def generate_pattern():
    """Generate pattern using k-means clustering with palette colors"""
    try:
        data = request.json
        pattern_type = data.get('pattern_type', 'geometric')
        palette_id = data.get('palette_id')
        k_clusters = data.get('k_clusters', 5)
        width = data.get('width', 800)
        height = data.get('height', 600)
        
        # Get palette colors
        if palette_id:
            db_path = data.get('db_path', 'android_db/color_groups.db')
            palettes = db_helper.get_palettes(db_path)
            palette = next((p for p in palettes if p['id'] == palette_id), None)
            if not palette:
                return jsonify({'success': False, 'message': 'Palette not found'}), 404
            colors = palette['colors']
        else:
            # Use provided colors or default
            colors = data.get('colors', [(255, 0, 0), (0, 255, 0), (0, 0, 255)])
            if isinstance(colors[0], int):
                # Convert Android color integers to RGB
                colors = [android_color_to_rgb(c) for c in colors]
        
        # Generate pattern
        result = pattern_generator.generate(
            pattern_type=pattern_type,
            colors=colors,
            k_clusters=k_clusters,
            width=width,
            height=height
        )
        
        # Save generated pattern
        output_filename = f"generated_{pattern_type}_{palette_id or 'custom'}.png"
        output_path = os.path.join(app.config['OUTPUT_FOLDER'], output_filename)
        result['image'].save(output_path)
        
        return jsonify({
            'success': True,
            'pattern_type': pattern_type,
            'colors_used': result['colors_used'],
            'output_image': f'/static/outputs/{output_filename}'
        })
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/static/outputs/<filename>')
def output_file(filename):
    """Serve output files"""
    return send_from_directory(app.config['OUTPUT_FOLDER'], filename)

if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=5000)

