@echo off
rem Activate virtual environment (explicit path)
set VENV_PATH=%~dp0..\venv\Scripts\python.exe

rem Verify torch import
%VENV_PATH% -c "import torch, sys; echo Torch version: %torch.__version__%"
if %errorlevel% neq 0 (
    echo Failed to import torch. Exiting.
    exit /b 1
)

rem Run training
%VENV_PATH% scripts\train.py --config configs\experiments\full_train.yaml
if %errorlevel% neq 0 goto :error

rem Run evaluation
%VENV_PATH% scripts\evaluate.py
if %errorlevel% neq 0 goto :error

rem Export TorchScript model
%VENV_PATH% scripts\export_torchscript.py
if %errorlevel% neq 0 goto :error

rem Start FastAPI server (in background)
start cmd /c "uvicorn backend.main:app --host 0.0.0.0 --port 8000"

rem Wait a bit for server to start
timeout /t 5 >nul

rem Test predict endpoint
scripts\test_predict.sh
if %errorlevel% neq 0 goto :error

echo All steps completed successfully.
goto :eof

:error
echo An error occurred. Check logs.
pause
