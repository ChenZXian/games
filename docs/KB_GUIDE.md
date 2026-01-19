# Knowledge Base Guide (KB)

KB location:
- kb/problems/*.md

Rule:
- One issue per file.
- Filename format:
  - kb/problems/YYYYMMDD-<slug>.md
  - slug: lowercase, digits, underscore only

Minimal required sections:
- Symptom
- Error Log (minimal but sufficient)
- Root Cause
- Fix (exact commands + file paths)
- Prevention (doctor/validator/template improvements)
- References (optional)

Frontmatter required keys:
- id
- date
- tags
- severity
- components
- env (os/jdk/gradle/agp/compileSdk/minSdk/targetSdk)

Search:
- rg -n "<error snippet>" kb/problems