let palettes = [];

// Tab switching
function switchTab(tabName) {
    // Hide all tabs
    document.querySelectorAll('.tab-content').forEach(tab => {
        tab.classList.remove('active');
    });
    
    // Remove active class from all buttons
    document.querySelectorAll('.tab-button').forEach(btn => {
        btn.classList.remove('active');
    });
    
    // Show selected tab
    document.getElementById(`${tabName}-tab`).classList.add('active');
    
    // Add active class to clicked button
    event.target.classList.add('active');
    
    // Load palettes if switching to generate or palettes tab
    if (tabName === 'generate' || tabName === 'palettes') {
        loadPalettes();
    }
}

// File upload handling
function handleFileSelect(event) {
    const file = event.target.files[0];
    if (file) {
        detectPattern(file);
    }
}

// Drag and drop
const uploadArea = document.getElementById('upload-area');
uploadArea.addEventListener('dragover', (e) => {
    e.preventDefault();
    uploadArea.style.borderColor = '#764ba2';
});

uploadArea.addEventListener('dragleave', () => {
    uploadArea.style.borderColor = '#667eea';
});

uploadArea.addEventListener('drop', (e) => {
    e.preventDefault();
    uploadArea.style.borderColor = '#667eea';
    const file = e.dataTransfer.files[0];
    if (file && file.type.startsWith('image/')) {
        document.getElementById('image-input').files = e.dataTransfer.files;
        detectPattern(file);
    }
});

uploadArea.addEventListener('click', () => {
    document.getElementById('image-input').click();
});

// Pattern detection
async function detectPattern(file) {
    const formData = new FormData();
    formData.append('file', file);
    
    const loadingDiv = document.getElementById('detection-loading');
    const resultDiv = document.getElementById('detection-result');
    
    loadingDiv.style.display = 'block';
    resultDiv.style.display = 'none';
    
    try {
        const response = await fetch('/api/detect', {
            method: 'POST',
            body: formData
        });
        
        const data = await response.json();
        
        if (data.success) {
            document.getElementById('result-image').src = data.output_image;
            document.getElementById('pattern-type').textContent = data.pattern_type;
            document.getElementById('pattern-description').textContent = data.description;
            
            // Display color swatches
            const swatchesDiv = document.getElementById('color-swatches');
            swatchesDiv.innerHTML = '';
            data.colors_detected.forEach(color => {
                const swatch = document.createElement('div');
                swatch.className = 'color-swatch';
                swatch.style.backgroundColor = `rgb(${color[0]}, ${color[1]}, ${color[2]})`;
                swatch.title = `RGB(${color[0]}, ${color[1]}, ${color[2]})`;
                swatchesDiv.appendChild(swatch);
            });
            
            resultDiv.style.display = 'block';
        } else {
            showError('Detection failed: ' + data.message);
        }
    } catch (error) {
        showError('Error detecting pattern: ' + error.message);
    } finally {
        loadingDiv.style.display = 'none';
    }
}

// Pattern generation
document.getElementById('generate-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    
    const formData = {
        pattern_type: document.getElementById('pattern-type-select').value,
        palette_id: document.getElementById('palette-select').value || null,
        k_clusters: parseInt(document.getElementById('k-clusters').value),
        width: parseInt(document.getElementById('width').value),
        height: parseInt(document.getElementById('height').value)
    };
    
    const loadingDiv = document.getElementById('generation-loading');
    const resultDiv = document.getElementById('generation-result');
    
    loadingDiv.style.display = 'block';
    resultDiv.style.display = 'none';
    
    try {
        const response = await fetch('/api/generate', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(formData)
        });
        
        const data = await response.json();
        
        if (data.success) {
            document.getElementById('generated-image').src = data.output_image;
            
            // Display colors used
            const colorsDiv = document.getElementById('generated-colors');
            colorsDiv.innerHTML = '';
            data.colors_used.forEach(color => {
                const swatch = document.createElement('div');
                swatch.className = 'color-swatch';
                swatch.style.backgroundColor = `rgb(${color[0]}, ${color[1]}, ${color[2]})`;
                swatch.title = `RGB(${color[0]}, ${color[1]}, ${color[2]})`;
                colorsDiv.appendChild(swatch);
            });
            
            resultDiv.style.display = 'block';
        } else {
            showError('Generation failed: ' + data.message);
        }
    } catch (error) {
        showError('Error generating pattern: ' + error.message);
    } finally {
        loadingDiv.style.display = 'none';
    }
});

// Load palettes
async function loadPalettes() {
    const loadingDiv = document.getElementById('palettes-loading');
    const palettesList = document.getElementById('palettes-list');
    const paletteSelect = document.getElementById('palette-select');
    
    loadingDiv.style.display = 'block';
    palettesList.innerHTML = '';
    
    try {
        const response = await fetch('/api/palettes');
        const data = await response.json();
        
        if (data.success) {
            palettes = data.palettes;
        } else {
            // Use sample palettes if database not found
            palettes = data.palettes || [];
        }
        
        // Update palette select
        paletteSelect.innerHTML = '<option value="">Custom Colors</option>';
        palettes.forEach(palette => {
            const option = document.createElement('option');
            option.value = palette.id;
            option.textContent = palette.name;
            paletteSelect.appendChild(option);
        });
        
        // Display palettes
        palettes.forEach(palette => {
            const card = document.createElement('div');
            card.className = 'palette-card';
            card.innerHTML = `
                <h3>${palette.name}</h3>
                <p>${palette.colors.length} colors</p>
                <div class="palette-colors">
                    ${palette.colors.map(color => 
                        `<div class="palette-color" style="background-color: rgb(${color[0]}, ${color[1]}, ${color[2]})" 
                              title="RGB(${color[0]}, ${color[1]}, ${color[2]})"></div>`
                    ).join('')}
                </div>
            `;
            palettesList.appendChild(card);
        });
        
    } catch (error) {
        showError('Error loading palettes: ' + error.message);
    } finally {
        loadingDiv.style.display = 'none';
    }
}

// Upload palette JSON
function showUploadPalette() {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = '.json';
    input.onchange = async (e) => {
        const file = e.target.files[0];
        if (file) {
            try {
                const text = await file.text();
                const data = JSON.parse(text);
                
                const response = await fetch('/api/palettes/upload', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(data)
                });
                
                const result = await response.json();
                if (result.success) {
                    showSuccess('Palette uploaded successfully!');
                    loadPalettes();
                } else {
                    showError('Upload failed: ' + result.message);
                }
            } catch (error) {
                showError('Error reading file: ' + error.message);
            }
        }
    };
    input.click();
}

// Utility functions
function showError(message) {
    const errorDiv = document.createElement('div');
    errorDiv.className = 'error-message';
    errorDiv.textContent = message;
    document.querySelector('.card').appendChild(errorDiv);
    setTimeout(() => errorDiv.remove(), 5000);
}

function showSuccess(message) {
    const successDiv = document.createElement('div');
    successDiv.className = 'success-message';
    successDiv.textContent = message;
    document.querySelector('.card').appendChild(successDiv);
    setTimeout(() => successDiv.remove(), 5000);
}

// Load palettes on page load
window.addEventListener('load', () => {
    loadPalettes();
});

