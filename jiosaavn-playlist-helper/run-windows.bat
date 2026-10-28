@echo off
cd /d "%~dp0"
py -m venv .venv
call .venv\Scripts\activate.bat
python -m pip install --upgrade pip
python -m pip install -r requirements.txt
python -m playwright install chromium
if not exist songs.txt copy songs.example.txt songs.txt
python jiosaavn_playlist.py --file songs.txt --playlist "PRANIT VIBES"
pause
