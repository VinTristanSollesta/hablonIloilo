import cv2
import numpy as np
from PIL import Image
from sklearn.cluster import KMeans

class PatternDetector:
    """Detect patterns in images"""
    
    def __init__(self):
        pass
    
    def detect(self, image_path):
        """
        Detect patterns in an image
        
        Args:
            image_path: Path to the image file
            
        Returns:
            Dictionary with pattern_type, description, colors, and annotated image
        """
        # Load image
        img = cv2.imread(image_path)
        if img is None:
            raise ValueError(f"Could not load image: {image_path}")
        
        img_rgb = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
        img_gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
        
        # Detect pattern type
        pattern_type, description = self._classify_pattern(img_gray, img_rgb)
        
        # Extract dominant colors
        colors = self._extract_dominant_colors(img_rgb, k=8)
        
        # Create annotated image
        annotated = self._annotate_image(img_rgb.copy(), pattern_type, colors)
        
        return {
            'pattern_type': pattern_type,
            'description': description,
            'colors': colors,
            'image': Image.fromarray(annotated)
        }
    
    def _classify_pattern(self, gray, rgb):
        """Classify the type of pattern in the image"""
        height, width = gray.shape
        
        # Check for geometric patterns
        # Detect lines
        edges = cv2.Canny(gray, 50, 150)
        lines = cv2.HoughLinesP(edges, 1, np.pi/180, threshold=100, 
                                minLineLength=min(width, height)//10, 
                                maxLineGap=20)
        
        # Detect circles
        circles = None
        try:
            detected_circles = cv2.HoughCircles(gray, cv2.HOUGH_GRADIENT, 1, 20,
                                               param1=50, param2=30, minRadius=10, maxRadius=100)
            if detected_circles is not None:
                circles = detected_circles
        except:
            pass
        
        # Check for repeating patterns using FFT
        f_transform = np.fft.fft2(gray)
        f_shift = np.fft.fftshift(f_transform)
        magnitude_spectrum = np.log(np.abs(f_shift) + 1)
        
        # Analyze frequency domain for periodicity
        periodic_score = np.std(magnitude_spectrum)
        
        # Check for color patterns
        color_variance = np.var(rgb.reshape(-1, 3), axis=0).mean()
        
        # Classify based on features
        num_lines = len(lines) if lines is not None else 0
        num_circles = len(circles[0]) if circles is not None else 0
        
        if num_lines > 10:
            if num_circles > 3:
                return "geometric_mixed", "Mixed geometric pattern with lines and circles"
            return "geometric_lines", "Geometric pattern with linear elements"
        
        if num_circles > 3:
            return "geometric_circles", "Geometric pattern with circular elements"
        
        if periodic_score > 3.0:
            return "repeating", "Repeating/tessellating pattern"
        
        if color_variance > 5000:
            return "color_gradient", "Color gradient or smooth color transition"
        
        # Check for texture patterns
        texture_score = self._calculate_texture_score(gray)
        if texture_score > 0.3:
            return "texture", "Textured pattern"
        
        return "abstract", "Abstract or complex pattern"
    
    def _calculate_texture_score(self, gray):
        """Calculate texture complexity score"""
        # Use Local Binary Pattern (LBP) approximation
        kernel = np.array([[-1, -1, -1], [-1, 8, -1], [-1, -1, -1]])
        filtered = cv2.filter2D(gray.astype(np.float32), -1, kernel)
        return np.std(filtered) / 255.0
    
    def _extract_dominant_colors(self, img_rgb, k=8):
        """Extract dominant colors using k-means clustering"""
        # Reshape image to be a list of pixels
        pixels = img_rgb.reshape(-1, 3)
        
        # Sample pixels for faster processing
        sample_size = min(10000, len(pixels))
        indices = np.random.choice(len(pixels), sample_size, replace=False)
        sample_pixels = pixels[indices]
        
        # Apply k-means
        kmeans = KMeans(n_clusters=k, random_state=42, n_init=10)
        kmeans.fit(sample_pixels)
        
        # Get cluster centers (dominant colors)
        colors = kmeans.cluster_centers_.astype(int)
        
        # Sort by frequency
        labels = kmeans.predict(pixels)
        unique, counts = np.unique(labels, return_counts=True)
        sorted_indices = np.argsort(counts)[::-1]
        
        return [tuple(colors[i]) for i in sorted_indices]
    
    def _annotate_image(self, img, pattern_type, colors):
        """Annotate image with pattern information"""
        height, width = img.shape[:2]
        
        # Add text overlay
        font = cv2.FONT_HERSHEY_SIMPLEX
        cv2.putText(img, f"Pattern: {pattern_type}", (10, 30),
                   font, 1, (255, 255, 255), 2, cv2.LINE_AA)
        cv2.putText(img, f"Pattern: {pattern_type}", (10, 30),
                   font, 1, (0, 0, 0), 1, cv2.LINE_AA)
        
        # Draw color swatches
        swatch_size = 40
        margin = 10
        for i, color in enumerate(colors[:5]):  # Show top 5 colors
            x = margin + i * (swatch_size + margin)
            y = height - swatch_size - margin
            cv2.rectangle(img, (x, y), (x + swatch_size, y + swatch_size), 
                         color, -1)
            cv2.rectangle(img, (x, y), (x + swatch_size, y + swatch_size), 
                         (255, 255, 255), 2)
        
        return img

