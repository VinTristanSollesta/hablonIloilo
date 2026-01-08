import sqlite3
import os

class DatabaseHelper:
    """Helper class to read Android SQLite database for color palettes"""
    
    def __init__(self):
        pass
    
    def get_palettes(self, db_path):
        """
        Read palettes from Android SQLite database
        
        Args:
            db_path: Path to the color_groups.db file
            
        Returns:
            List of palette dictionaries with id, name, and colors
        """
        if not os.path.exists(db_path):
            raise FileNotFoundError(f"Database file not found: {db_path}")
        
        palettes = []
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        
        try:
            # Get all color groups
            cursor.execute("""
                SELECT id, name, timestamp 
                FROM color_groups 
                ORDER BY timestamp DESC
            """)
            
            groups = cursor.fetchall()
            
            for group_id, name, timestamp in groups:
                # Get colors for this group
                cursor.execute("""
                    SELECT color 
                    FROM group_colors 
                    WHERE group_id = ?
                    ORDER BY id
                """, (group_id,))
                
                color_rows = cursor.fetchall()
                colors = []
                
                for (color_int,) in color_rows:
                    # Convert Android color integer (ARGB) to RGB tuple
                    r = (color_int >> 16) & 0xFF
                    g = (color_int >> 8) & 0xFF
                    b = color_int & 0xFF
                    colors.append((r, g, b))
                
                if colors:  # Only add palettes with colors
                    palettes.append({
                        'id': group_id,
                        'name': name,
                        'colors': colors,
                        'timestamp': timestamp
                    })
        
        finally:
            conn.close()
        
        return palettes
    
    def export_palettes_to_json(self, db_path, output_path='palettes.json'):
        """Export palettes to JSON file"""
        palettes = self.get_palettes(db_path)
        import json
        with open(output_path, 'w') as f:
            json.dump(palettes, f, indent=2)
        return output_path

