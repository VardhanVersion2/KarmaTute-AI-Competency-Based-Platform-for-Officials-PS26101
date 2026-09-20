# KarmaTute Assessment Diagnosis

## 1. Current Working Parts
- **Authentication & JWT Context:** The backend successfully extracts the user context from the JWT token for `/api/execution-lab/start` via `getAuthenticatedUser()`.
- **Database Schema:** Entities like `Assessment`, `AssessmentAttempt`, `OCRResult`, `EvidenceItem`, and `CompetencyGap` are correctly wired via JPA.
- **Competency Gap Resolution:** When an assessment is passed (e.g. simulated score >= 85), `ExecutionLabService` successfully locates the user's `OPEN` priority gap and changes its status to `RESOLVED`.
- **Vite Dynamic Import:** The React `ExecutionLabPage` successfully lazy-loads `pdfjs-dist` to prevent the previous fatal Vite module evaluation crash.

## 2. Broken Parts & First Actual Failure
- **FIRST ACTUAL FAILURE (The "Failed to initialize execution" Toast):**
  - **Root Cause:** When the user registers a *new* account, `userRepository.count() > 0` becomes true. If the backend restarts, `DataInitializer.java` detects existing users and immediately `return`s, **skipping the seeding of the database's `Assessment` entities**. 
  - **The Chain Reaction:** When the frontend calls `fetchAssessments()`, the backend returns an empty array `[]` (because no assessments exist). The frontend catches this and defaults to a hardcoded fallback with `id: 1` ("SQL & Data Validation Assessment"). The user bypasses Level 1 (since it's mocked, see below) and clicks "Initialize Execution Environment" for Level 2. The frontend sends `POST /start` with `assessmentId: 1`. The backend queries `assessmentRepository.findById(1)`, which throws `RuntimeException("Assessment not found")`, resulting in an HTTP 500. The frontend catches the 500 error and displays the "Failed to initialize execution" toast.

## 3. Fake / Simulated / Mocked Parts
- **Level 1 (Knowledge Verification):** Completely mocked on the frontend. The `Start Verification` button and `handleL1Submit()` do not make any API calls. It uses a `setTimeout` to simulate an MCQ evaluation and locally pushes the user to Level 2.
- **Level 2 (Practical Execution):** 
  - `submitOCR` in `executionLabApi.ts` sends `simulatedExtractedText` and `simulatedConfidence` directly from the frontend to the backend.
  - The backend `ExecutionLabService.java` explicitly accepts these simulated values and uses them to calculate the score and pass/fail the user, rather than independently evaluating the OCR text against an LLM rubric.
- **Level 3 (Judgement Scenario):** Completely mocked on the frontend. `L3_SCENARIO` is hardcoded in the React component. `handleL3Submit()` uses `setTimeout` to simulate an API call and complete the assessment.
- **Evidence & Certificates:** `GrowthProofPage.tsx` falls back to mocked certificates. Command Center uses a mocked Heatmap ("Fake 30 days activity for Heatmap").
- **DEMO_USER_ID:** Various components (e.g., `SkillIntelligencePage`, `DataChamberPage`, `GrowthProofPage`) still contain `const DEMO_USER_ID = 1;` and use it for data fetching, ignoring the actual authenticated user context.

## 4. API Mismatches
- **ExecutionLab Frontend vs Backend:** The frontend allows advancing through Level 1 and Level 3 without ever creating an `AssessmentAttempt` state on the backend for those levels. The backend only records an attempt when Level 2 is initialized.
- **Assessment Payload:** The `Assessment` entity contains collections for `questions` and `rubrics`, but these are totally empty/ignored. The frontend relies on hardcoded `L1_QUESTIONS` instead of fetching them dynamically from the Assessment object.

## 5. Database Problems
- **DataInitializer Logic:** The logic `if (userRepository.count() > 0) return;` prevents crucial application data (Competencies, Assessments, Rules) from being seeded if *any* user exists. System data should be decoupled from Demo User data seeding.
- **Lack of Real Test Data:** Because Level 1 and Level 3 are mocked on the frontend, there are no actual `Question` or `JudgementScenario` records in the database.

## 6. Exact Root Cause of Current Assessment Failure
1. User creates a new account (user count > 0).
2. Backend restarts (or DataInitializer is triggered).
3. `DataInitializer` skips seeding `Assessment` records because a user already exists.
4. User navigates to Execution Lab -> `fetchAssessments()` returns `[]`.
5. Frontend falls back to a mocked `Assessment` object with `id: 1`.
6. User clicks "Initialize Execution Environment" -> frontend sends `POST /api/execution-lab/start` with `{"assessmentId": 1}`.
7. Backend throws `Assessment not found` (HTTP 500).
8. Frontend displays "Failed to initialize execution".

## 7. Recommended Fix Order
1. **Fix Data Seeding:** Separate system data (Competencies, Assessments, Questions, Rubrics) from demo user data in `DataInitializer.java` so that Assessments always exist regardless of registered users.
2. **Remove Fallbacks:** Remove the hardcoded fallback assessments from `ExecutionLabPage.tsx` so missing data fails visibly rather than masking the root cause.
3. **De-Mock Level 1 & 3:** Implement actual backend endpoints for `startAttempt`, `submitMCQ`, and `submitJudgement` that evaluate against real database `Question` entities.
4. **Secure Level 2 OCR:** Move the confidence calculation and text evaluation to the backend (or an LLM integration) instead of trusting `simulatedConfidence` sent by the client.
5. **Purge DEMO_USER_ID:** Strip all remaining hardcoded `DEMO_USER_ID = 1` constants from frontend components and wire them into the `useApp().userProfile` context.
