import os
import subprocess
import sys
from pathlib import Path

def find_adb():
    # Try common locations
    possible = [
        Path(os.getenv('ANDROID_HOME', '')) / 'platform-tools' / 'adb.exe',
        Path('C:/Users/mogar/AppData/Local/Android/Sdk/platform-tools/adb.exe'),
    ]
    for p in possible:
        if p.is_file():
            return str(p)
    # fallback to system PATH
    return 'adb'

def run_cmd(cmd):
    result = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, shell=True)
    return result.stdout.strip(), result.stderr.strip(), result.returncode

def main():
    adb_path = find_adb()
    stdout, stderr, code = run_cmd(f'"{adb_path}" devices')
    if code != 0:
        # adb not functional
        validation = "ANDROID RUNTIME VALIDATION PENDING: adb not available or failed to run."
    else:
        lines = stdout.splitlines()
        devices = [l for l in lines if '\tdevice' in l]
        if not devices:
            validation = "ANDROID RUNTIME VALIDATION PENDING: No device or emulator detected via adb."
        else:
            # Use the first device
            device = devices[0].split('\t')[0]
            apk_path = Path('frontend/android/app/build/outputs/apk/debug/app-debug.apk').resolve()
            if not apk_path.is_file():
                validation = f"ANDROID RUNTIME VALIDATION FAILED: APK not found at {apk_path}"
            else:
                # Install APK
                install_out, install_err, install_code = run_cmd(f'"{adb_path}" -s {device} install -r "{apk_path}"')
                # Launch main activity (package name assumed from manifest; adjust if needed)
                launch_cmd = f'"{adb_path}" -s {device} shell am start -n com.example.humanactivity/.MainActivity'
                launch_out, launch_err, launch_code = run_cmd(launch_cmd)
                # Capture logcat for recent logs
                log_out, log_err, log_code = run_cmd(f'"{adb_path}" -s {device} logcat -d')
                # Simple heuristics to detect expected messages
                msgs = []
                for keyword in [
                    'Sensor registration', 'Window formed', 'Scaler loaded',
                    'Model loaded', 'Inference', 'Predicted activity'
                ]:
                    if keyword.lower() in log_out.lower():
                        msgs.append(f"Detected: {keyword}")
                if len(msgs) >= 5:
                    validation = "ANDROID RUNTIME VALIDATION SUCCESSFUL. Detected logs: \n" + "\n".join(msgs)
                else:
                    validation = "ANDROID RUNTIME VALIDATION PARTIAL: APK installed and launched, but required runtime logs not found.\nLog excerpt:\n" + log_out[:500]
    # Write result to report file
    report_path = Path('reports/android_validation.md')
    report_path.parent.mkdir(parents=True, exist_ok=True)
    with open(report_path, 'w', encoding='utf-8') as f:
        f.write('# Android Runtime Validation\n\n')
        f.write(validation + '\n')

if __name__ == '__main__':
    main()
