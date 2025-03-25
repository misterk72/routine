import pandas as pd
import sqlite3
import json
from datetime import datetime

def import_excel_to_sqlite(excel_file, db_file):
    # Read Excel file
    df = pd.read_excel(excel_file)
    
    # Connect to SQLite database
    conn = sqlite3.connect(db_file)
    cursor = conn.cursor()
    
    # Create table if it doesn't exist
    with open('schema.sql', 'r') as f:
        schema = f.read()
    cursor.executescript(schema)
    
    # Insert data
    for _, row in df.iterrows():
        try:
            # Prepare custom data as JSON
            custom_data = {
                'source': 'excel_import',
                'import_date': datetime.now().isoformat()
            }
            
            cursor.execute('''
                INSERT INTO workouts (
                    date_time, program, duration_minutes, average_speed,
                    distance_km, calories, calories_per_km, average_heart_rate,
                    max_heart_rate, min_heart_rate, weight_kg, fat_mass_kg,
                    fat_percentage, waist_circumference_cm, background_music,
                    observations, custom_data, tags, notes
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ''', (
                row['Date et Heure'],
                row['Programme'],
                row['Durée (min)'],
                row['Vitesse Moyenne (km/h)'],
                row['Distance parcourue (km)'],
                row['Calories'],
                row['Calories/km'],
                row['Moyenne pulsations/min'],
                row['Max pulsations/min'],
                row['Min pulsations/min'],
                row['Masse (kg)'],
                row['Masse graisseuse (kg)'],
                row['Taux de graisse'],
                row['Tour de taille (cm)'],
                row['Fond sonore'],
                row['Observations'],
                json.dumps(custom_data),  # Store custom data as JSON
                '',  # Empty tags for now
                ''  # Empty notes for now
            ))
        except Exception as e:
            print(f"Error inserting row: {e}")
            continue
    
    # Commit changes and close connection
    conn.commit()
    conn.close()

if __name__ == "__main__":
    import_excel_to_sqlite('Entrainement velo elliptique.xlsx', 'health.db')
