-- Create the templates table compatible with SQLite
CREATE TABLE IF NOT EXISTS templates (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    code VARCHAR(50),
    name VARCHAR(100),
    organization_name VARCHAR(100),
    layout VARCHAR(50),
    primary_color VARCHAR(50),
    secondary_color VARCHAR(50),
    text_color VARCHAR(50),
    tagline VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS profiles (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    uuid VARCHAR(255),
    registration_number VARCHAR(255),
    type VARCHAR(50) NOT NULL,
    full_name VARCHAR(255),
    department VARCHAR(255),
    title VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(255),
    blood_group VARCHAR(10),
    date_of_birth DATE,
    issue_date DATE,
    expiry_date DATE,
    photo_file_name VARCHAR(255),
    photo_content_type VARCHAR(255),
    barcode_type VARCHAR(50),
    template_id INTEGER,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (template_id) REFERENCES templates(id)
);