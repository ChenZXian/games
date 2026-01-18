# Cursor Rules (KB + Baseline)

## Mandatory preflight
Always run:
- powershell -ExecutionPolicy Bypass -File tools/env/doctor.ps1
If it fails, stop and fix environment first.

## When any error happens
1) Search KB first:
- rg -n "<key error snippet>" kb/problems
2) If found:
- Apply the Fix steps.
- Improve Prevention if applicable.
3) If not found:
- Create a new KB entry:
  - powershell -ExecutionPolicy Bypass -File tools/kb/new_kb_entry.ps1 -Slug "<short_slug>"
- Fill sections: Symptom, Error Log, Root Cause, Fix, Prevention.
- If preventable, update doctor/validator/templates accordingly.

## Commit rules
- If a new KB entry was created, commit KB + code fix together.
