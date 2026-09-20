
# FINAL QA MATRIX - KarmaTute SIH 2026

| Feature | Expected Behaviour | Endpoint/Component | Test Method | Expected Result | Actual Result | Pass/Fail |
|---|---|---|---|---|---|---|
| Build | Backend compiles with Maven | \pom.xml\ | \mvn clean compile\ | Build success | Build success | Pass |
| Build | Frontend lints without errors | \package.json\ | \
pm run lint\ | No type errors | No type errors | Pass |
| Build | Frontend builds successfully | \ite build\ | \
pm run build\ | Build success | Build success | Pass |
| Auth | Authentication is server-side | \AuthController\ | API check | Requires JWT/Session | TBD | TBD |
| Auth | Admin cannot be faked client-side | \SecurityConfig\ | API Check | HTTP 403 for Learner | TBD | TBD |
| Assessment | PDF Material Validation | \UploadService\ | Upload non-PDF | Rejected | TBD | TBD |
| Assessment | Exactly 15 valid MCQs | \MCQEngine\ | Generate MCQs | Count = 15 | TBD | TBD |
| Assessment | Level 1 actual scoring | \AssessmentService\ | Submit Answers | Actual score returned | TBD | TBD |
| Assessment | Gating enforces Level 1 pass | \AssessmentService\ | Start L2 without L1 | Rejected | TBD | TBD |
| OCR | Actual OCR runs without hardcoding | \OCRService\ | Upload real image | Real extracted text | TBD | TBD |
| Closed Loop | Assessment -> Evidence -> Competency | \CompetencyEngine\ | Complete assessment | Snapshot/Gap updates | TBD | TBD |
| Certificate | Eligibility policy checked | \CertificateService\ | Request below threshold | Ineligible | TBD | TBD |
| Certificate | PDF Generation | \CertificateService\ | Download Certificate | Real PDF byte stream | TBD | TBD |
| KarmaDNA | Authenticated Encryption Export | \KarmaDNAService\ | Export package | Encrypted package | TBD | TBD |
| Voice | STT/TTS Providers with Fallback | \VoiceProvider\ | Toggle providers | Responds truthfully | TBD | TBD |

