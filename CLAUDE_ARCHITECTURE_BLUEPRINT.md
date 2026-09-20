# KarmaTute AI Competency Engine: Architecture & Implementation Blueprint

**Target Audience:** Claude AI (for code verification, debugging, and architectural review)  
**Project Aim:** To build an AI-driven, continuous competency evaluation platform for government officials. It moves away from static exams to dynamic, multimodal assessments (Text, OCR, Voice) that continuously update a user's skill profile ("Karma DNA") and automatically issue cryptographic certificates ("Growth Proof").

---

## 1. High-Level Information Flow (End-to-End)

1. **Ingestion (Data Chamber):** User uploads policy documents (PDFs). The backend extracts text using PDFBox, chunks it, and detects topics via Mistral AI.
2. **Assessment (Execution Lab):** Based on the ingested document, the system generates a 3-level assessment:
   - *Level 1 (Knowledge):* AI-generated MCQs.
   - *Level 2 (Application):* Written answers via OCR (currently simulated for demo).
   - *Level 3 (Articulation):* Voice-based interview via `KarmaVaani` (Web Speech API).
3. **Evaluation (Evidence Service):** Assessment scores are normalized and packaged as an `EvidenceRecord`.
4. **Analytics (Karma DNA):** The `CompetencyEngine` applies an Exponential Weighted Moving Average (EWMA) algorithm to the `EvidenceRecord`, updating the user's `CompetencySnapshot` (Radar Charts).
5. **Certification (Growth Proof):** If the updated EWMA score crosses the `85.0` threshold, the `CertificateService` automatically mints an immutable, SHA-256 hashed certificate.

---

## 2. Tech Stack

### Frontend
- **Framework:** React 18, TypeScript, Vite
- **Styling:** Tailwind CSS, Framer Motion (Animations), Lucide React (Icons)
- **State Management:** React Context (`AppContext`)
- **Voice/Audio:** Web Speech API (`SpeechSynthesis` & `SpeechRecognition`)
- **Charting:** Recharts (Radar charts for Karma DNA)

### Backend
- **Framework:** Java 17, Spring Boot 3.2.4
- **Database:** H2 In-Memory (Dev/Demo) / Ready for PostgreSQL (Prod)
- **AI Integration:** Spring AI, Mistral AI (`spring.ai.mistralai.api-key`)
- **Security:** Spring Security, JWT (JSON Web Tokens)
- **Document Processing:** Apache PDFBox (PDF parsing)
- **Resilience:** Resilience4j (Circuit Breakers, Rate Limiters, Retries for AI Gateway)

---

## 3. Core Modules: "Kya Kar Raha Hai Aur Kaise" (Detailed Mechanics)

### 3.1. Karma Vaani (The AI Voice Assistant)
- **What it does:** Acts as an interactive supervisor. It reads out questions in L3, provides feedback, and guides the user.
- **How it works:** Utilizes the browser's native Web Speech API. 
- **Critical Fix Applied:** React 18 Strict Mode mounts components twice in development. This caused `VoiceProviderFactory` to spawn two identical TTS queues, resulting in an echoing/overlapping voice. Fixed by wrapping the factory instantiation in a `useMemo` hook inside `useKarmaVaaniBrain.ts`. Added a strict `window.speechSynthesis.cancel()` hook on modal close to kill zombie audio processes.

### 3.2. Execution Lab (Assessment System)
- **What it does:** Facilitates multimodal testing. 
- **How it works:** 
  - Generates questions dynamically via `MCQGenerationService` (calling Mistral AI).
  - Maintains state via `AssessmentSessionService`. 
  - **Demo Bypasses:** Contains `/demo-pass` endpoint (triggered by "Skip Assessment" UI buttons) to instantly award a perfect score (100) and transition the session to `CERT_ELIGIBLE` for rapid hackathon presentations.

### 3.3. Karma DNA (EWMA Competency Engine)
- **What it does:** Tracks skill progression over time rather than relying on a single exam score.
- **How it works:** 
  - `AssessmentSessionService` passes an `EvidenceRecord` to `EvidenceService`.
  - `CompetencyEngine` recalculates the `CompetencySnapshot` using:
    `New_Score = (Previous_Score * 0.8) + (New_Evidence_Score * 0.2)`
  - The UI fetches this data to render real-time multidimensional radar charts.

### 3.4. Growth Proof (Certification System)
- **What it does:** Issues verifiable proof of upskilling.
- **How it works:** 
  - Hooked directly into the end of the `EvidenceService` lifecycle. 
  - If `CompetencySnapshot.getCurrent() >= 85.0`, it invokes `issueCertificate()`.
  - Generates an immutable record (`KT-{User}-{Competency}-{Timestamp}`) and a SHA-256 `integrityHash` of the evidence IDs and scores.

---

## 4. Specific Technical Connections (The "Rewiring")

Prior to recent fixes, the architecture was fragmented. The Execution Lab saved results to a legacy `UserCompetency` table, while Karma DNA read from `CompetencySnapshot`. 

**The Current (Corrected) Flow:**
1. `ExecutionLabController` receives assessment completion POST request.
2. `AssessmentSessionService` delegates to `EvidenceService.submitEvidence(record)`.
3. `EvidenceService` calls `CompetencyEngine.calculateNewScore()`.
4. `CompetencyEngine` saves the new `CompetencySnapshot`.
5. `AssessmentSessionService` then calls `CertificateService.issueCertificate(userId, competencyId)`.
6. `CertificateService` validates eligibility (Score >= 85) and saves the `Certificate`.

*Note: A `DevController` (`/api/dev/fill-dummy-data`) exists to manually bypass this entire chain and instantly seed a 100.0 score snapshot and certificate for demo purposes.*

---

## 5. MUST-IMPLEMENT Features & Improvements (For Claude's Review)

**Dear Claude, when verifying this architecture, please evaluate the following planned improvements:**

### A. Security & Production Readiness
1. **Database Migration:** Replace H2 in-memory database with PostgreSQL. Currently, restarting the backend wipes all user progress and certificates.
2. **JWT Hardening:** The JWT secret is currently a hardcoded/weak string in development. Implement environment-variable-based RSA keypairs.
3. **API Key Security:** Ensure `MISTRAL_API_KEY` is strictly managed via server environment variables and never exposed in `.properties` files on public repositories.

### B. Technical Debt & Functional Upgrades
1. **True OCR Integration (Level 2):** The current L2 OCR evaluation is "simulated" for the hackathon demo. Need to integrate actual Tesseract OCR or Google Cloud Vision API to parse handwritten images upload by users.
2. **WebRTC for Voice (Level 3):** The Web Speech API is browser-dependent (works best on Chrome, fails on others) and lacks true streaming capabilities. Upgrade to WebRTC + WebSocket streaming to the backend for real-time AI voice synthesis (e.g., Deepgram + Mistral).
3. **AI Gateway Rate Limiting:** While `Resilience4j` is configured, implement user-level token bucket rate-limiting to prevent API cost overruns on the Mistral AI billing account.
4. **PDF Parsing Robustness:** `PDFBox` struggles with multi-column layouts. Integrate a more robust document parser (like unstructured.io or LlamaParse) for better RAG (Retrieval-Augmented Generation) context.
5. **Role-Based Access Control (RBAC):** Differentiate between `OFFICIAL` (takes tests) and `ADMIN` (creates competencies, views aggregate dashboards).
