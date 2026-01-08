import numpy as np
from PIL import Image, ImageDraw
from sklearn.cluster import KMeans
import random
import math

class PatternGenerator:
    """Generate patterns using k-means clustering with palette colors"""
    
    def __init__(self):
        pass
    
    def generate(self, pattern_type='geometric', colors=None, k_clusters=5, 
                 width=800, height=600):
        """
        Generate a pattern using k-means clustering
        
        Args:
            pattern_type: Type of pattern ('geometric', 'organic', 'tessellation', 'gradient')
            colors: List of RGB color tuples from palette
            k_clusters: Number of clusters for k-means
            width: Image width
            height: Image height
            
        Returns:
            Dictionary with generated image and colors used
        """
        if colors is None:
            colors = [(255, 0, 0), (0, 255, 0), (0, 0, 255)]
        
        # Ensure we have enough colors
        if len(colors) < k_clusters:
            # Generate additional colors using k-means on existing colors
            colors = self._expand_color_palette(colors, k_clusters)
        
        # Create base image
        img = Image.new('RGB', (width, height), color=(255, 255, 255))
        draw = ImageDraw.Draw(img)
        
        # Generate pattern based on type
        if pattern_type == 'geometric':
            self._generate_geometric(draw, colors, width, height)
        elif pattern_type == 'organic':
            self._generate_organic(draw, colors, width, height)
        elif pattern_type == 'tessellation':
            self._generate_tessellation(draw, colors, width, height)
        elif pattern_type == 'gradient':
            self._generate_gradient(draw, colors, width, height)
        else:
            self._generate_geometric(draw, colors, width, height)
        
        # Apply k-means color quantization to ensure palette colors are used
        img_array = np.array(img)
        quantized_img = self._quantize_colors(img_array, colors, k_clusters)
        
        return {
            'image': Image.fromarray(quantized_img),
            'colors_used': colors[:k_clusters]
        }
    
    def _expand_color_palette(self, colors, target_count):
        """Expand color palette using interpolation and k-means"""
        if len(colors) >= target_count:
            return colors[:target_count]
        
        # Convert to numpy array
        color_array = np.array(colors)
        
        # Generate intermediate colors
        expanded = list(colors)
        while len(expanded) < target_count:
            # Pick two random colors and interpolate
            c1, c2 = random.sample(colors, 2)
            t = random.random()
            new_color = tuple(int(c1[i] * (1-t) + c2[i] * t) for i in range(3))
            expanded.append(new_color)
        
        # Use k-means to refine the palette
        kmeans = KMeans(n_clusters=target_count, random_state=42, n_init=10)
        kmeans.fit(np.array(expanded))
        refined_colors = kmeans.cluster_centers_.astype(int)
        
        return [tuple(c) for c in refined_colors]
    
    def _quantize_colors(self, img_array, palette_colors, k_clusters):
        """Quantize image colors to match palette using k-means"""
        # Reshape image to list of pixels
        pixels = img_array.reshape(-1, 3)
        
        # Use palette colors as initial cluster centers
        initial_centers = np.array(palette_colors[:k_clusters])
        
        # Apply k-means with palette as initialization
        kmeans = KMeans(n_clusters=k_clusters, init=initial_centers, 
                       n_init=1, random_state=42)
        kmeans.fit(pixels)
        
        # Replace pixels with nearest palette color
        labels = kmeans.predict(pixels)
        quantized_pixels = initial_centers[labels]
        
        # Reshape back to image
        quantized_img = quantized_pixels.reshape(img_array.shape)
        
        return quantized_img.astype(np.uint8)
    
    def _generate_geometric(self, draw, colors, width, height):
        """Generate geometric pattern"""
        cell_size = 40
        rows = height // cell_size
        cols = width // cell_size
        
        for row in range(rows + 1):
            for col in range(cols + 1):
                x = col * cell_size
                y = row * cell_size
                
                # Random shape
                shape_type = random.choice(['circle', 'square', 'triangle'])
                color = random.choice(colors)
                
                if shape_type == 'circle':
                    radius = cell_size // 3
                    draw.ellipse([x - radius, y - radius, x + radius, y + radius],
                               fill=color)
                elif shape_type == 'square':
                    size = cell_size // 2
                    draw.rectangle([x - size//2, y - size//2, x + size//2, y + size//2],
                                  fill=color)
                else:  # triangle
                    size = cell_size // 2
                    points = [
                        (x, y - size),
                        (x - size, y + size),
                        (x + size, y + size)
                    ]
                    draw.polygon(points, fill=color)
    
    def _generate_organic(self, draw, colors, width, height):
        """Generate organic/flowing pattern"""
        num_curves = 20
        
        for _ in range(num_curves):
            color = random.choice(colors)
            start_x = random.randint(0, width)
            start_y = random.randint(0, height)
            
            # Create bezier-like curve
            points = []
            for i in range(10):
                t = i / 9.0
                x = start_x + random.randint(-100, 100) * math.sin(t * math.pi * 2)
                y = start_y + random.randint(-100, 100) * math.cos(t * math.pi * 2)
                points.append((int(x), int(y)))
            
            # Draw smooth curve
            for i in range(len(points) - 1):
                draw.line([points[i], points[i+1]], fill=color, width=5)
    
    def _generate_tessellation(self, draw, colors, width, height):
        """Generate tessellating pattern"""
        tile_size = 60
        
        for row in range(0, height, tile_size):
            for col in range(0, width, tile_size):
                # Hexagon pattern
                color = random.choice(colors)
                center_x = col + tile_size // 2
                center_y = row + tile_size // 2
                radius = tile_size // 2
                
                # Draw hexagon
                points = []
                for i in range(6):
                    angle = math.pi / 3 * i
                    x = center_x + radius * math.cos(angle)
                    y = center_y + radius * math.sin(angle)
                    points.append((int(x), int(y)))
                
                draw.polygon(points, fill=color, outline=(0, 0, 0), width=1)
    
    def _generate_gradient(self, draw, colors, width, height):
        """Generate gradient pattern"""
        # Create gradient mesh
        steps = 20
        
        for i in range(steps):
            t = i / (steps - 1)
            
            # Interpolate between colors
            color1 = colors[i % len(colors)]
            color2 = colors[(i + 1) % len(colors)]
            
            color = tuple(int(color1[j] * (1-t) + color2[j] * t) for j in range(3))
            
            y_start = int(height * t)
            y_end = int(height * (t + 1/steps))
            
            draw.rectangle([0, y_start, width, y_end], fill=color)
            
            # Add some geometric elements
            if i % 3 == 0:
                x = random.randint(0, width)
                size = random.randint(20, 60)
                draw.ellipse([x - size, y_start - size, x + size, y_start + size],
                           fill=random.choice(colors))

