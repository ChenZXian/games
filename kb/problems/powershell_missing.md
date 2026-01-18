---
id: powershell-missing
date: 2026-01-18
tags: [environment, tooling]
severity: high
components: [doctor, powershell]
env: linux
---

## Symptom
Running tools/env/doctor.ps1 fails because PowerShell is not available.

## Error Log
bash: command not found: pwsh
bash: command not found: powershell

## Root Cause
The environment does not have PowerShell installed, so required .ps1 tooling cannot run.

## Fix
Install PowerShell or provide a shim so that pwsh or powershell is available in PATH, then rerun:
- powershell -ExecutionPolicy Bypass -File tools/env/doctor.ps1

## Prevention
Provide a platform-appropriate doctor script or add a preflight check that reports missing PowerShell with instructions to install it.

## References
- tools/env/doctor.ps1
