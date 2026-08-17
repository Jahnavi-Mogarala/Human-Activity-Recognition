#!/usr/bin/env python
import zipfile
from pathlib import Path

def main():
    zip_path = Path('data/raw/UCI-HAR/UCI_HAR_Dataset.zip')
    extract_dir = Path('data/raw/UCI-HAR')
    with zipfile.ZipFile(zip_path, 'r') as z:
        z.extractall(extract_dir)
    print('Extraction completed')

if __name__ == '__main__':
    main()
