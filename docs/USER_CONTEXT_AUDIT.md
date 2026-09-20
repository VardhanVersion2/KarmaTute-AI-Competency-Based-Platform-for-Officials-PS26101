# User Context & Authentication Audit

## 1. Current Login Behaviour
- **Frontend (LoginPage.tsx)**: The login is entirely client-side. When the user submits the login form, it triggers a setTimeout for 1.2s, then sets isAuthenticated = true in the React context and demo_fresh_start = true in localStorage.
- **Gati-Login**: Similarly, uploading a .karma file fakes an upload with a 1.8s timeout and authenticates the user locally.
- **Backend**: There is no actual /api/auth/login endpoint or authentication context verification. The backend accepts all requests matching /api/** due to permitAll() in SecurityConfig.java.

## 2. Current Identity Storage
- **Frontend (AppContext.tsx)**: Uses localStorage (karmatute_auth, karmatute_profile, karmatute_onboarding) to keep the user "logged in" across reloads.
- **Backend (User.java)**: The entity is limited to basic fields (id, username, ullName, ole, department, designation, 	argetRole). Crucially, **there is no password field or credential storage**.

## 3. Current User ID Flow
- The frontend hardcodes const DEMO_USER_ID = 1; across multiple screens.
- When making API calls (e.g., getting evidence, competency snapshots, certificates, or exporting KarmaDNA), it appends DEMO_USER_ID to the endpoint (e.g., /api/karmadna/export/1).
- The backend blindly accepts this userId in the URL path variable and returns data without verifying if the caller actually owns that userId.

## 4. Hardcoded User References
- ackend/src/main/java/KarmaTute/KarmaTute/config/DataInitializer.java: Creates a seed user: ajesh.kumar, Rajesh Kumar, with Department of Administrative Reforms & Public Grievances. It seeds multiple competencies, gaps, Next Best Actions, and evidence items directly linked to this user on application startup.
- rontend/src/features/competency-radar/SkillIntelligencePage.tsx: const DEMO_USER_ID = 1;
- rontend/src/features/data-chamber/DataChamberPage.tsx: const DEMO_USER_ID = 1;
- rontend/src/features/growth-proof/GrowthProofPage.tsx: const DEMO_USER_ID = 1;

## 5. Screens Depending on Demo Data
- **Command Center**: Checks localStorage.getItem('demo_fresh_start') and falls back to hardcoded profile data (e.g., department: 'General Administration') if the context profile is missing.
- **Skill Intelligence**: Uses DEMO_USER_ID to fetch competencies.
- **Execution Lab**: Does not pass a userId for fetching assessments, but relies on a global assessment list. Submitting answers assumes a mock context.
- **Growth & Proof**: Uses DEMO_USER_ID to fetch evidence and certificates.
- **Data Chamber (KarmaDNA)**: Uses DEMO_USER_ID to export the package.

## 6. Backend Endpoints Accepting userId from Client
- GET /api/evidence/user/{userId}
- GET /api/competency/user/{userId}
- GET /api/certificate/user/{userId}
- GET /api/karmadna/export/{userId}
- POST /api/execution-lab/start (accepts userId in JSON body)

## 7. Required Migration to "Current User" Context
1. **Remove DEMO_USER_ID = 1** from all React components.
2. **Convert API endpoints** to /api/me/... pattern (e.g., /api/me/evidence, /api/me/competency, /api/me/certificate).
3. **Backend SecurityContext**: Endpoints must extract the authenticated principal (e.g., via SecurityContextHolder) and use its internal userId to query the database.
4. **AppLayout / AppContext**: Remove setUserRole client-side override for ROLE_ADMIN. Admin navigation must be gated by the backend-issued JWT claims or /api/me/bootstrap response.
5. **DataInitializer**: Wrap the Rajesh Kumar seeding logic inside a conditional block checking for KARMATUTE_DEMO_MODE=true.
