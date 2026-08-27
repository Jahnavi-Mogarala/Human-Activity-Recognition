@echo off
rem Activate virtual environment
call ..\venv\Scripts\activate.bat

rem Run training
python scripts\train.py --config configs\experiments\full_train.yaml
if %errorlevel% neq 0 goto :error

rem Run evaluation
python scripts\evaluate.py
if %errorlevel% neq 0 goto :error

rem Export TorchScript model
python scripts\export_torchscript.py
if %errorlevel% neq 0 goto :error

rem Start FastAPI server (in background)
start cmd /c "uvicorn backend.main:app --host 0.0.0.0 --port 8000"

rem Give server a moment to start
timeout /t 5 >nul

rem Test predict endpoint
scripts\test_predict.sh
if %errorlevel% neq 0 goto :error

echo All steps completed successfully.
goto :eof

:error
echo Error occurred in one of the steps. Check logs.
pause
