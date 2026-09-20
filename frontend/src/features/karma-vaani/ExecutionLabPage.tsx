import React, { useState, useEffect, useRef } from 'react';
import { PageTutorialModal } from '../../components/ui/PageTutorialModal';
import { Button } from '../../components/ui/Button';
import { useToast } from '../../context/ToastContext';
import { useApp } from '../../context/AppContext';
import {
  uploadMaterial,
  startLevel1Session,
  submitLevel1,
  startLevel2,
  submitLevel2,
  demoSubmitLevel2,
  startLevel3,
  submitLevel3,
  demoPassSession,
  getCurrentSession,
  type SessionQuestion,
  type StartSessionResponse,
  type L1SubmitResponse,
  type L2StartResponse,
  type L2SubmitResponse,
  type L3StartResponse,
  type L3SubmitResponse,
  type MaterialUploadResponse,
} from '../../services/executionLabApi';

import {
  FileText,
  CheckCircle2,
  AlertCircle,
  Eye,
  ShieldAlert,
  Cpu,
  Upload,
  Layers,
  ArrowRight,
  FileCheck2,
  Sparkles,
  Loader2,
  XCircle,
} from 'lucide-react';

// ─── Types ───────────────────────────────────────────────────────────────────
type LabPhase =
  | 'UPLOAD_MATERIAL'       // Step 0: Upload PDF material
  | 'MATERIAL_PROCESSING'   // Step 0b: Backend processing
  | 'L1_READY'              // Level 1 ready
  | 'L1_IN_PROGRESS'        // Level 1 questions shown
  | 'L1_RESULT'             // Level 1 passed or failed
  | 'L2_READY'              // Level 2 about to start
  | 'L2_IN_PROGRESS'        // Level 2 question shown, awaiting answer upload
  | 'L2_RESULT'             // Level 2 result shown
  | 'L2_REVIEW_REQUIRED'    // Low OCR confidence
  | 'L3_READY'
  | 'L3_IN_PROGRESS'
  | 'COMPLETED';

// ─── Component ───────────────────────────────────────────────────────────────
export function ExecutionLabPage() {
  const { userProfile } = useApp();
  const toast = useToast();

  const [phase, setPhase] = useState<LabPhase>('UPLOAD_MATERIAL');
  const [loading, setLoading] = useState(false);
  const [sessionId, setSessionId] = useState<number | null>(null);

  // Material upload
  const [material, setMaterial] = useState<MaterialUploadResponse | null>(null);
  const materialInputRef = useRef<HTMLInputElement>(null);

  // L1
  const [l1Data, setL1Data] = useState<StartSessionResponse | null>(null);
  const [l1CurrentQ, setL1CurrentQ] = useState(0);
  const [l1Answers, setL1Answers] = useState<Record<number, string>>({});
  const [l1Result, setL1Result] = useState<L1SubmitResponse | null>(null);

  // L2
  const [l2Data, setL2Data] = useState<L2StartResponse | null>(null);
  const [l2AnswerFile, setL2AnswerFile] = useState<File | null>(null);
  const [l2Result, setL2Result] = useState<L2SubmitResponse | null>(null);
  const answerFileRef = useRef<HTMLInputElement>(null);

  // L3
  const [l3Data, setL3Data] = useState<L3StartResponse | null>(null);
  const [l3Answer, setL3Answer] = useState('');
  const [l3Result, setL3Result] = useState<L3SubmitResponse | null>(null);

  // Resume session on mount
  useEffect(() => {
    (async () => {
      try {
        const session = await getCurrentSession();
        if (session) {
          setSessionId(session.sessionId);
          // Resume to appropriate phase
          const stateMap: Record<string, LabPhase> = {
            L1_IN_PROGRESS: 'L1_IN_PROGRESS',
            L1_PASSED: 'L2_READY',
            L1_FAILED: 'L1_RESULT',
            L2_IN_PROGRESS: 'L2_IN_PROGRESS',
            L2_PASSED: 'L3_READY',
            L2_FAILED: 'L2_RESULT',
            L2_REVIEW_REQUIRED: 'L2_REVIEW_REQUIRED',
            L3_IN_PROGRESS: 'L3_IN_PROGRESS',
            COMPLETED: 'COMPLETED',
          };
          const resumePhase = stateMap[session.state];
          if (resumePhase) {
            setPhase(resumePhase);
            // Show material info from session
            setMaterial({
              materialId: session.materialId,
              filename: session.materialName,
              status: 'READY',
              detectedTopic: session.topic,
              pageCount: 0,
              error: '',
            });
            // Set scores if available
            if (session.l1Score !== null) {
              setL1Result({ sessionId: session.sessionId, score: session.l1Score, correct: 0, total: 15, passed: session.l1Score >= 70, state: session.state, topic: session.topic });
            }
            if (session.l2Score !== null) {
              setL2Result({ sessionId: session.sessionId, score: session.l2Score, passed: session.l2Score >= 60, state: session.state, ocrConfidence: session.l2OcrConfidence || 0, feedback: '', topic: session.topic });
            }
          }
        }
      } catch (err) {
        // No session, start fresh
      }
    })();
  }, []);

  // ─── Handlers ──────────────────────────────────────────────────────────────

  const handleMaterialUpload = async (file: File) => {
    if (!file.name.toLowerCase().endsWith('.pdf')) {
      toast.error('Only PDF files are accepted. Please upload a .pdf file.');
      return;
    }
    setLoading(true);
    setPhase('MATERIAL_PROCESSING');
    try {
      const result = await uploadMaterial(file);
      setMaterial(result);
      if (result.status === 'FAILED') {
        toast.error(result.error || 'Material processing failed. Please upload a clearer PDF.');
        setPhase('UPLOAD_MATERIAL');
      } else if (result.status === 'READY') {
        toast.success(`Material ready! Topic detected: ${result.detectedTopic}`);
        setPhase('L1_READY');
      } else {
        toast.error('Material processing failed. Please try again.');
        setPhase('UPLOAD_MATERIAL');
      }
    } catch (err: any) {
      toast.error(`Upload failed: ${err.message}`);
      setPhase('UPLOAD_MATERIAL');
    } finally {
      setLoading(false);
    }
  };


  const handleDemoSkip = async () => {
    if (!sessionId) return;
    setLoading(true);
    try {
      await demoPassSession(sessionId);
      toast.success('Demo mode: Successfully passed assessment with full marks!');
      setCurrentPage('growth-proof');
    } catch (err: any) {
      toast.error('Demo skip failed: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleStartL1 = async () => {
    if (!material) return;
    setLoading(true);
    try {
      const data = await startLevel1Session(material.materialId);
      setL1Data(data);
      setSessionId(data.sessionId);
      setL1CurrentQ(0);
      setL1Answers({});
      setPhase('L1_IN_PROGRESS');
      toast.success(`Level 1 started — ${data.questionCount} questions on "${data.topic}"`);
    } catch (err: any) {
      toast.error(`Failed to start Level 1: ${err.message}`);
    } finally {
      setLoading(false);
    }
  };

  const handleL1Answer = (questionId: number, selectedOption: string) => {
    setL1Answers(prev => ({ ...prev, [questionId]: selectedOption }));
  };

  const handleSubmitL1 = async () => {
    if (!sessionId || !l1Data) return;
    const unanswered = l1Data.questions.filter(q => !l1Answers[q.questionId]);
    if (unanswered.length > 0) {
      toast.error(`Please answer all questions. ${unanswered.length} remaining.`);
      return;
    }
    setLoading(true);
    try {
      const result = await submitLevel1(sessionId, l1Answers);
      setL1Result(result);
      setPhase('L1_RESULT');
      if (result.passed) {
        toast.success(`Level 1 Passed! Score: ${result.score}% (${result.correct}/${result.total} correct)`);
      } else {
        toast.error(`Level 1 not passed. Score: ${result.score}%. You need 70% to proceed.`);
      }
    } catch (err: any) {
      toast.error(`Submission failed: ${err.message}`);
    } finally {
      setLoading(false);
    }
  };

  const handleStartL2 = async () => {
    if (!sessionId) return;
    setLoading(true);
    try {
      const data = await startLevel2(sessionId);
      setL2Data(data);
      setL2AnswerFile(null);
      setPhase('L2_IN_PROGRESS');
      toast.success('Level 2 started — Read the question and write your answer, then upload as PDF');
    } catch (err: any) {
      toast.error(`Failed to start Level 2: ${err.message}`);
    } finally {
      setLoading(false);
    }
  };


  const handleDemoSubmitL2 = async () => {
    if (!sessionId) return;
    setLoading(true);
    try {
      const result = await demoSubmitLevel2(sessionId);
      setL2Result(result);
      setPhase('L2_RESULT');
      if (result.passed) {
        toast.success('Level 2 Passed! Score: ' + result.score + '/100');
      } else {
        toast.error('Level 2 Failed. Score: ' + result.score + '/100');
      }
    } catch (err: any) {
      toast.error('Failed to submit Level 2 demo: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmitL2 = async () => {
    if (!sessionId || !l2AnswerFile) {
      toast.error('Please upload your written answer as a PDF first.');
      return;
    }
    setLoading(true);
    try {
      const result = await submitLevel2(sessionId, l2AnswerFile);
      setL2Result(result);
      if (result.state === 'L2_REVIEW_REQUIRED') {
        setPhase('L2_REVIEW_REQUIRED');
        toast.error(result.message || 'Answer PDF quality is too low. Please upload a clearer scan.');
      } else {
        setPhase('L2_RESULT');
        if (result.passed) {
          toast.success(`Level 2 Passed! Score: ${result.score}/100`);
        } else {
          toast.error(`Level 2 not passed. Score: ${result.score}/100. You need 60 to proceed.`);
        }
      }
    } catch (err: any) {
      toast.error(`Submission failed: ${err.message}`);
    } finally {
      setLoading(false);
    }
  };

  const handleStartL3 = async () => {
    if (!sessionId) return;
    setLoading(true);
    try {
      const data = await startLevel3(sessionId);
      setL3Data(data);
      setL3Answer('');
      setPhase('L3_IN_PROGRESS');
      toast.success('Level 3 started — Read the scenario and compose your response');
    } catch (err: any) {
      toast.error(`Failed to start Level 3: ${err.message}`);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmitL3 = async () => {
    if (!sessionId || l3Answer.trim().length < 30) {
      toast.error('Please provide a substantive response (at least 30 characters).');
      return;
    }
    setLoading(true);
    try {
      const result = await submitLevel3(sessionId, l3Answer);
      setL3Result(result);
      setPhase('COMPLETED');
      toast.success(`Assessment Complete! Overall Score: ${result.overallScore}/100`);
    } catch (err: any) {
      toast.error(`Submission failed: ${err.message}`);
    } finally {
      setLoading(false);
    }
  };

  // ─── Stepper progress ──────────────────────────────────────────────────────
  const getActiveLevel = () => {
    if (['UPLOAD_MATERIAL', 'MATERIAL_PROCESSING', 'L1_READY', 'L1_IN_PROGRESS', 'L1_RESULT'].includes(phase)) return 1;
    if (['L2_READY', 'L2_IN_PROGRESS', 'L2_RESULT', 'L2_REVIEW_REQUIRED'].includes(phase)) return 2;
    return 3;
  };

  const activeLevel = getActiveLevel();

  // ─── Render helpers ────────────────────────────────────────────────────────
  const currentQuestion = l1Data?.questions[l1CurrentQ];

  return (
    <>
      <PageTutorialModal
        pageId="execution-lab"
        title="Execution Lab"
        what="Your capability verification workspace — grounded in material you upload."
        why="We test execution from your actual learning material, not generic questions."
        how="Upload your PDF study material → AI generates questions → Complete Level 1 (MCQ), Level 2 (Written), Level 3 (Scenario)."
        psAsk="AI-grounded assessment from uploaded material."
        karmaTuteBuild="Multi-level assessment (MCQ, Written Answer, Scenario) — all backed by real AI."
        differentiator="Questions are generated from YOUR uploaded material, not hardcoded content."
      />

      <div className="flex flex-col gap-6 max-w-[1200px] mx-auto pb-36 animate-in fade-in">

        {/* HEADER */}
        <div className="flex flex-col md:flex-row md:items-end justify-between gap-4 border-b border-gov-border pb-4">
          <div>
            <h1 className="text-2xl font-extrabold text-gov-primary tracking-tight">Execution Lab</h1>
            <p className="text-sm text-gov-text-secondary mt-1 font-medium">
              AI-grounded practical capability assessment
              {userProfile?.designation && ` · ${userProfile.designation}`}
              {userProfile?.department && ` · ${userProfile.department}`}
            </p>
          </div>
          {material?.detectedTopic && (
            <div className="text-right">
              <span className="text-xs font-bold text-gov-text-muted uppercase">Material Topic</span>
              <div className="text-gov-primary font-bold">{material.detectedTopic}</div>
            </div>
          )}
        </div>

        {/* STEPPER */}
        <div className="bg-gov-surface border border-gov-border rounded-md shadow-sm p-4">
          <div className="flex flex-col md:flex-row items-center justify-between gap-4">
            {[
              { level: 1, icon: <FileText size={18} />, label: 'Knowledge Verification', sublabel: 'Level 1' },
              { level: 2, icon: <Cpu size={18} />, label: 'Practical Execution', sublabel: 'Level 2' },
              { level: 3, icon: <Layers size={18} />, label: 'Judgement Scenario', sublabel: 'Level 3' },
            ].map((step, idx) => (
              <React.Fragment key={step.level}>
                <div className={`flex-1 flex items-center gap-3 p-3 rounded-md border transition-all ${
                  activeLevel === step.level
                    ? 'bg-gov-primary text-white border-gov-primary shadow-md ring-4 ring-gov-primary/10'
                    : 'bg-gov-surface border-gov-border text-gov-text-primary'
                }`}>
                  <div className={`w-9 h-9 rounded-full flex items-center justify-center font-bold shrink-0 ${
                    activeLevel === step.level ? 'bg-white text-gov-primary' : 'bg-gov-primary/10 text-gov-primary'
                  }`}>
                    {step.icon}
                  </div>
                  <div className="flex flex-col text-left">
                    <span className={`text-xs font-bold uppercase tracking-wider ${activeLevel === step.level ? 'text-white/80' : 'text-gov-text-secondary'}`}>
                      {step.sublabel}
                    </span>
                    <span className={`text-sm font-bold ${activeLevel === step.level ? 'text-white' : 'text-gov-text-primary'}`}>
                      {step.label}
                    </span>
                  </div>
                </div>
                {idx < 2 && <ArrowRight className="hidden md:block text-gov-border shrink-0" size={20} />}
              </React.Fragment>
            ))}
          </div>
        </div>

        {/* CONTENT AREA */}
        <div className="bg-gov-surface border border-gov-border shadow-sm p-6 md:p-8 rounded-md">

          {/* ── PHASE: UPLOAD MATERIAL ── */}
          {(phase === 'UPLOAD_MATERIAL' || phase === 'MATERIAL_PROCESSING') && (
            <div className="flex flex-col items-center justify-center py-10 max-w-xl mx-auto text-center gap-6">
              <div className="bg-gov-primary/10 rounded-full p-5">
                <FileText size={40} className="text-gov-primary" />
              </div>
              <div>
                <h3 className="text-xl font-bold text-gov-primary mb-2">Upload Your Study Material</h3>
                <p className="text-sm text-gov-text-secondary">
                  Upload the PDF document you have studied.
                  The AI will extract its content and generate a personalized assessment for{' '}
                  <strong>{userProfile?.designation || 'you'}</strong>
                  {userProfile?.department ? ` in ${userProfile.department}` : ''}.
                </p>
              </div>

              {phase === 'MATERIAL_PROCESSING' ? (
                <div className="flex flex-col items-center gap-3">
                  <Loader2 size={32} className="animate-spin text-gov-primary" />
                  <span className="text-sm font-bold text-gov-primary">Processing PDF — Extracting text and detecting topics…</span>
                </div>
              ) : (
                <div
                  className="w-full border-2 border-dashed border-gov-border hover:border-gov-primary bg-gov-surface-muted/60 transition-all rounded-md p-8 flex flex-col items-center justify-center cursor-pointer relative group"
                  onClick={() => materialInputRef.current?.click()}
                >
                  <input
                    ref={materialInputRef}
                    type="file"
                    accept=".pdf"
                    className="hidden"
                    onChange={(e) => {
                      const file = e.target.files?.[0];
                      if (file) handleMaterialUpload(file);
                    }}
                  />
                  <div className="w-14 h-14 rounded-full bg-gov-primary/10 text-gov-primary flex items-center justify-center mb-3 group-hover:scale-110 transition-transform">
                    <Upload size={24} />
                  </div>
                  <span className="text-base font-bold text-gov-primary">Click to Browse or Drag &amp; Drop</span>
                  <span className="text-xs text-gov-text-secondary mt-1">Only PDF files accepted · Max 50 MB</span>
                </div>
              )}

              {material?.status === 'FAILED' && (
                <div className="flex items-start gap-3 bg-red-50 border border-red-200 rounded-md p-4 text-left w-full">
                  <XCircle size={20} className="text-red-500 shrink-0 mt-0.5" />
                  <div>
                    <div className="text-sm font-bold text-red-700">Processing Failed</div>
                    <div className="text-xs text-red-600 mt-0.5">{material.error}</div>
                  </div>
                </div>
              )}
            </div>
          )}

          {/* ── PHASE: L1 READY ── */}
          {phase === 'L1_READY' && material && (
            <div className="flex flex-col items-center justify-center py-10 max-w-xl mx-auto text-center gap-6">
              <CheckCircle2 size={48} className="text-gov-success" />
              <div>
                <h3 className="text-xl font-bold text-gov-primary mb-2">Material Ready</h3>
                <p className="text-sm text-gov-text-secondary">
                  Your PDF <strong>{material.filename}</strong> has been processed.
                  {material.pageCount > 0 && ` (${material.pageCount} pages)`}
                </p>
                <p className="text-sm font-bold text-gov-primary mt-2">Topic: {material.detectedTopic}</p>
                <p className="text-xs text-gov-text-secondary mt-2">
                  Level 1 will present <strong>15 AI-generated MCQ questions</strong> grounded in your uploaded material,
                  tailored to your role as{' '}
                  <strong>{userProfile?.designation || 'Officer'}</strong>
                  {userProfile?.department ? ` in ${userProfile.department}` : ''}.
                  You need <strong>70%</strong> to proceed.
                </p>
              </div>
              <Button
                className="bg-gov-accent hover:bg-gov-accent-hover text-white font-bold px-8"
                onClick={handleStartL1}
                disabled={loading}
              >
                {loading ? <><Loader2 size={16} className="mr-2 animate-spin" />Generating Questions…</> : 'Begin Level 1 — Knowledge Verification'}
              </Button>
            </div>
          )}

          {/* ── PHASE: L1 IN PROGRESS ── */}
          {phase === 'L1_IN_PROGRESS' && l1Data && currentQuestion && (
            <div className="flex flex-col items-center py-4 max-w-2xl mx-auto w-full gap-6">
              <div className="w-full">
                {/* Progress */}
                <div className="flex justify-between items-center mb-4">
                  <span className="text-sm font-bold text-gov-primary">
                    Question {l1CurrentQ + 1} of {l1Data.questions.length}
                  </span>
                  <span className="text-xs text-gov-text-secondary">
                    Topic: {l1Data.topic}
                  </span>
                </div>
                <div className="w-full bg-gov-surface-muted h-2 rounded-full overflow-hidden mb-6">
                  <div
                    className="bg-gov-primary h-full transition-all"
                    style={{ width: `${((l1CurrentQ + 1) / l1Data.questions.length) * 100}%` }}
                  />
                </div>

                {/* Question card */}
                <div className="bg-white p-6 rounded-lg border border-gov-border shadow-sm">
                  <div className="flex items-center gap-2 mb-3">
                    <span className="text-[10px] font-bold text-gov-text-muted uppercase bg-gov-surface-muted px-2 py-0.5 rounded">
                      {currentQuestion.difficulty}
                    </span>
                    {currentQuestion.subtopic && (
                      <span className="text-[10px] font-bold text-gov-primary bg-gov-primary/10 px-2 py-0.5 rounded">
                        {currentQuestion.subtopic}
                      </span>
                    )}
                  </div>
                  <p className="text-base font-semibold text-gov-text-primary mb-6">
                    {currentQuestion.questionText}
                  </p>
                  <div className="flex flex-col gap-3 mb-6">
                    {currentQuestion.options.map((opt, idx) => {
                      const isSelected = l1Answers[currentQuestion.questionId] === opt;
                      return (
                        <div
                          key={idx}
                          onClick={() => handleL1Answer(currentQuestion.questionId, opt)}
                          className={`p-3 border rounded-md cursor-pointer transition-colors ${
                            isSelected
                              ? 'border-gov-accent bg-gov-accent/5'
                              : 'border-gray-200 hover:bg-gray-50'
                          }`}
                        >
                          <div className="flex items-center gap-3">
                            <div className={`w-4 h-4 rounded-full border flex shrink-0 items-center justify-center ${
                              isSelected ? 'border-gov-accent' : 'border-gray-300'
                            }`}>
                              {isSelected && <div className="w-2 h-2 rounded-full bg-gov-accent" />}
                            </div>
                            <span className="text-sm font-medium leading-tight">{opt}</span>
                          </div>
                        </div>
                      );
                    })}
                  </div>

                  {/* Navigation */}
                  <div className="flex gap-3">
                    {l1CurrentQ > 0 && (
                      <Button
                        variant="outline"
                        className="flex-1"
                        onClick={() => setL1CurrentQ(q => q - 1)}
                      >
                        Previous
                      </Button>
                    )}
                    {l1CurrentQ < l1Data.questions.length - 1 ? (
                      <Button
                        className="flex-1 bg-gov-primary text-white font-bold"
                        disabled={!l1Answers[currentQuestion.questionId]}
                        onClick={() => {
                          setL1CurrentQ(q => q + 1);
                        }}
                      >
                        Next Question
                      </Button>
                    ) : (
                      <Button
                        className="flex-1 bg-gov-accent text-white font-bold"
                        disabled={loading || Object.keys(l1Answers).length < l1Data.questions.length}
                        onClick={handleSubmitL1}
                      >
                        {loading ? <><Loader2 size={16} className="mr-2 animate-spin" />Evaluating…</> : 'Submit All Answers'}
                      </Button>
                    )}
                  </div>

                  {/* Answered count */}
                  <div className="mt-3 text-center text-xs text-gov-text-muted">
                    {Object.keys(l1Answers).length} of {l1Data.questions.length} questions answered
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* ── PHASE: L1 RESULT ── */}
          {phase === 'L1_RESULT' && l1Result && (
            <div className="flex flex-col items-center py-10 max-w-xl mx-auto text-center gap-6">
              {l1Result.passed ? (
                <>
                  <div className="bg-gov-success-bg border border-gov-success-border rounded-full p-5">
                    <CheckCircle2 size={48} className="text-gov-success" />
                  </div>
                  <div>
                    <h3 className="text-xl font-bold text-gov-success mb-2">Level 1 Passed!</h3>
                    <p className="text-sm text-gov-text-secondary">
                      Score: <strong>{l1Result.score}%</strong> ({l1Result.correct}/{l1Result.total} correct)
                    </p>
                    <p className="text-sm text-gov-text-secondary mt-1">Topic: {l1Result.topic}</p>
                    <p className="text-sm text-gov-text-secondary mt-2">
                      You have demonstrated foundational knowledge. Proceed to Level 2 — Practical Execution.
                    </p>
                  </div>
                  <Button className="bg-gov-success text-white font-bold border-none hover:bg-green-700" onClick={() => setPhase('L2_READY')}>Proceed to Level 2</Button><Button className="bg-yellow-500 text-white font-bold border-none hover:bg-yellow-600 ml-4" onClick={handleDemoSkip}>Skip Assessment (Demo)</Button>
                </>
              ) : (
                <>
                  <div className="bg-red-50 border border-red-200 rounded-full p-5">
                    <XCircle size={48} className="text-red-500" />
                  </div>
                  <div>
                    <h3 className="text-xl font-bold text-red-600 mb-2">Level 1 Not Passed</h3>
                    <p className="text-sm text-gov-text-secondary">
                      Score: <strong>{l1Result.score}%</strong> ({l1Result.correct}/{l1Result.total} correct).
                      You need <strong>70%</strong> to proceed.
                    </p>
                    <p className="text-sm text-gov-text-secondary mt-2">
                      Review your material and try again.
                    </p>
                  </div>
                  <Button
                    className="bg-gov-primary text-white font-bold"
                    onClick={() => setPhase('UPLOAD_MATERIAL')}
                  >
                    Upload New Material &amp; Retry
                  </Button>
                </>
              )}
            </div>
          )}

          {/* ── PHASE: L2 READY ── */}
          {phase === 'L2_READY' && (
            <div className="flex flex-col items-center py-10 max-w-xl mx-auto text-center gap-6">
              <div className="bg-gov-primary/10 rounded-full p-5">
                <Cpu size={40} className="text-gov-primary" />
              </div>
              <div>
                <h3 className="text-xl font-bold text-gov-primary mb-2">Level 2: Practical Execution</h3>
                <p className="text-sm text-gov-text-secondary">
                  An AI-generated open-ended reasoning question will be displayed from your uploaded material.
                  Write your answer on paper or type it in a document, then upload it as a PDF.
                  Your answer will be evaluated by the AI rubric engine.
                </p>
              </div>
              <Button
                className="bg-gov-accent hover:bg-gov-accent-hover text-white font-bold px-8"
                onClick={handleStartL2}
                disabled={loading}
              >
                {loading ? <><Loader2 size={16} className="mr-2 animate-spin" />Generating Question…</> : 'Begin Level 2'}
              </Button>
            </div>
          )}

          {/* ── PHASE: L2 IN PROGRESS ── */}
          {phase === 'L2_IN_PROGRESS' && l2Data && (
            <div className="flex flex-col lg:flex-row gap-8 items-start">
              {/* Question Panel */}
              <div className="flex-1 flex flex-col gap-4 bg-white p-6 border border-gov-border rounded-md shadow-sm">
                <div className="flex items-center gap-2 border-b border-gov-border pb-3">
                  <FileCheck2 size={18} className="text-gov-primary" />
                  <h3 className="text-sm font-bold text-gov-text-primary uppercase tracking-wide">
                    Level 2 — Practical Reasoning Question
                  </h3>
                </div>
                <div className="bg-gov-info-bg border-l-4 border-gov-info p-4 rounded-r-md">
                  <span className="text-xs font-bold text-gov-info uppercase tracking-wider block mb-1">
                    Topic: {l2Data.topic} {l2Data.subtopic && `· ${l2Data.subtopic}`}
                  </span>
                  <span className="text-xs text-gov-text-secondary">Source: {l2Data.materialName}</span>
                </div>
                <p className="text-base font-semibold text-gov-text-primary leading-relaxed">
                  {l2Data.question}
                </p>
                <div className="bg-gov-surface-muted border border-gov-border rounded-md p-3 text-xs text-gov-text-secondary">
                  <span className="font-bold">Instructions:</span> Write your answer on paper or in a document (Word/text editor),
                  save/export as PDF, then upload below. Duration: approximately {l2Data.durationMinutes} minutes.
                </div>
              </div>

              {/* Upload Panel */}
              <div className="flex-1 flex flex-col gap-4 bg-white p-6 border border-gov-border rounded-md shadow-sm">
                <div className="flex items-center justify-between border-b border-gov-border pb-3">
                  <h3 className="text-sm font-bold text-gov-text-primary uppercase tracking-wide">
                    Upload Your Written Answer
                  </h3>
                  <span className="text-[11px] font-bold text-gov-accent bg-gov-accent/10 px-2.5 py-1 rounded-full border border-gov-accent/30 flex items-center gap-1">
                    <Sparkles size={12} /> AI RUBRIC
                  </span>
                </div>

                <div
                  className="border-2 border-dashed border-gov-border hover:border-gov-primary bg-gov-surface-muted/60 transition-all rounded-md p-6 flex flex-col items-center justify-center text-center cursor-pointer relative group"
                  onClick={() => answerFileRef.current?.click()}
                >
                  <input
                    ref={answerFileRef}
                    type="file"
                    accept=".pdf"
                    className="hidden"
                    onChange={(e) => {
                      const file = e.target.files?.[0];
                      if (file) {
                        if (!file.name.toLowerCase().endsWith('.pdf')) {
                          toast.error('Only PDF files accepted for answer upload.');
                          return;
                        }
                        setL2AnswerFile(file);
                        toast.success(`Answer loaded: ${file.name}`);
                      }
                    }}
                  />
                  <div className="w-12 h-12 rounded-full bg-gov-primary/10 text-gov-primary flex items-center justify-center mb-2 group-hover:scale-110 transition-transform">
                    <Upload size={22} />
                  </div>
                  <span className="text-sm font-bold text-gov-primary">
                    {l2AnswerFile ? `✓ ${l2AnswerFile.name}` : 'Click to upload answer PDF'}
                  </span>
                  <span className="text-xs text-gov-text-secondary mt-1">Only .pdf accepted</span>
                </div>

                {l2AnswerFile && (
                  <div className="bg-gov-success-bg border border-gov-success-border rounded-md p-3 text-xs text-gov-success font-medium">
                    File ready: {l2AnswerFile.name} ({(l2AnswerFile.size / 1024).toFixed(0)} KB)
                  </div>
                )}

                <Button
                  className="w-full bg-gov-primary hover:bg-gov-primary-hover text-white font-bold py-3 rounded-md"
                  disabled={!l2AnswerFile || loading}
                  onClick={handleSubmitL2}
                >
                  {loading
                    ? <><Loader2 size={16} className="mr-2 animate-spin" />Evaluating Answer…</>
                    : <><Cpu size={18} className="mr-2" />Evaluate with AI Rubric</>
                  }
                </Button>
                <Button className="w-full bg-indigo-500 hover:bg-indigo-600 text-white font-bold py-3 rounded-md mt-2" disabled={loading} onClick={handleDemoSubmitL2}>
                  Simulate OCR Written Answer (Demo)
                </Button>
              </div>
            </div>
          )}

          {/* ── PHASE: L2 REVIEW REQUIRED ── */}
          {phase === 'L2_REVIEW_REQUIRED' && l2Result && (
            <div className="flex flex-col items-center py-10 max-w-xl mx-auto text-center gap-6">
              <div className="bg-yellow-50 border border-yellow-200 rounded-full p-5">
                <ShieldAlert size={48} className="text-yellow-600" />
              </div>
              <div>
                <h3 className="text-xl font-bold text-yellow-700 mb-2">Answer PDF Quality Too Low</h3>
                <p className="text-sm text-gov-text-secondary">
                  OCR Confidence: <strong>{(l2Result.ocrConfidence * 100).toFixed(1)}%</strong>
                  (minimum 75% required)
                </p>
                <p className="text-sm text-gov-text-secondary mt-2">
                  {l2Result.message || 'Please upload a clearer, high-quality scan of your written answer.'}
                </p>
              </div>
              <Button
                className="bg-gov-primary text-white font-bold"
                onClick={() => {
                  setL2AnswerFile(null);
                  setPhase('L2_IN_PROGRESS');
                }}
              >
                Upload a Better Scan
              </Button>
            </div>
          )}

          {/* ── PHASE: L2 RESULT ── */}
          {phase === 'L2_RESULT' && l2Result && (
            <div className="flex flex-col items-center py-10 max-w-xl mx-auto text-center gap-6">
              {l2Result.passed ? (
                <>
                  <div className="bg-gov-success-bg border border-gov-success-border rounded-full p-5">
                    <CheckCircle2 size={48} className="text-gov-success" />
                  </div>
                  <div>
                    <h3 className="text-xl font-bold text-gov-success mb-2">Level 2 Passed!</h3>
                    <p className="text-sm text-gov-text-secondary">
                      Rubric Score: <strong>{l2Result.score}/100</strong>
                      {' · '}OCR Confidence: <strong>{(l2Result.ocrConfidence * 100).toFixed(1)}%</strong>
                    </p>
                    {l2Result.feedback && (
                      <p className="text-sm text-gov-text-primary mt-3 bg-gov-surface-muted border border-gov-border rounded-md p-3 text-left">
                        <span className="font-bold text-gov-primary block mb-1">Evaluator Feedback:</span>
                        {l2Result.feedback}
                      </p>
                    )}
                  </div>
                  <Button className="bg-gov-success text-white font-bold border-none hover:bg-green-700" onClick={() => setPhase('L3_READY')}>Proceed to Level 3</Button><Button className="bg-yellow-500 text-white font-bold border-none hover:bg-yellow-600 ml-4" onClick={handleDemoSkip}>Skip Assessment (Demo)</Button>
                </>
              ) : (
                <>
                  <div className="bg-red-50 border border-red-200 rounded-full p-5">
                    <XCircle size={48} className="text-red-500" />
                  </div>
                  <div>
                    <h3 className="text-xl font-bold text-red-600 mb-2">Level 2 Not Passed</h3>
                    <p className="text-sm text-gov-text-secondary">
                      Rubric Score: <strong>{l2Result.score}/100</strong>. You need 60 to proceed.
                    </p>
                    {l2Result.feedback && (
                      <p className="text-sm text-gov-text-primary mt-3 bg-gov-surface-muted border border-gov-border rounded-md p-3 text-left">
                        <span className="font-bold text-gov-primary block mb-1">Evaluator Feedback:</span>
                        {l2Result.feedback}
                      </p>
                    )}
                  </div>
                  <Button
                    className="bg-gov-primary text-white font-bold"
                    onClick={() => setPhase('L2_READY')}
                  >
                    Retry Level 2
                  </Button>
                </>
              )}
            </div>
          )}

          {/* ── PHASE: L3 READY ── */}
          {phase === 'L3_READY' && (
            <div className="flex flex-col items-center py-10 max-w-xl mx-auto text-center gap-6">
              <div className="bg-gov-primary/10 rounded-full p-5">
                <Layers size={40} className="text-gov-primary" />
              </div>
              <div>
                <h3 className="text-xl font-bold text-gov-primary mb-2">Level 3: Mega Reasoning Scenario</h3>
                <p className="text-sm text-gov-text-secondary">
                  An AI-generated comprehensive scenario combining multiple concepts from your uploaded material.
                  You will compose a detailed written response demonstrating deep judgement and decision-making.
                </p>
              </div>
              <Button
                className="bg-gov-accent hover:bg-gov-accent-hover text-white font-bold px-8"
                onClick={handleStartL3}
                disabled={loading}
              >
                {loading ? <><Loader2 size={16} className="mr-2 animate-spin" />Generating Scenario…</> : 'Begin Level 3'}
              </Button>
            </div>
          )}

          {/* ── PHASE: L3 IN PROGRESS ── */}
          {phase === 'L3_IN_PROGRESS' && l3Data && (
            <div className="flex flex-col gap-6 max-w-3xl mx-auto w-full">
              <div className="bg-gov-info-bg border-l-4 border-gov-info p-4 rounded-r-md">
                <span className="text-xs font-bold text-gov-info uppercase tracking-wider block mb-1">
                  Level 3 — Mega Scenario · {l3Data.topic}
                </span>
                {l3Data.subtopic && <span className="text-xs text-gov-text-secondary">{l3Data.subtopic}</span>}
              </div>

              <div className="bg-white p-6 border border-gov-border rounded-md shadow-sm">
                <p className="text-base font-semibold text-gov-text-primary leading-relaxed whitespace-pre-wrap">
                  {l3Data.question}
                </p>
              </div>

              <div className="flex flex-col gap-2">
                <label className="text-xs font-bold text-gov-text-secondary uppercase">
                  Your Response
                </label>
                <textarea
                  className="w-full h-48 p-4 bg-gov-surface-muted border border-gov-border rounded-md text-sm text-gov-text-primary focus:border-gov-primary focus:outline-none resize-none leading-relaxed"
                  placeholder="Compose your detailed response here — demonstrate deep reasoning and judgement based on the material concepts…"
                  value={l3Answer}
                  onChange={(e) => setL3Answer(e.target.value)}
                />
                <div className="text-right text-xs text-gov-text-muted">
                  {l3Answer.trim().length} characters (minimum 30)
                </div>
              </div>

              <Button
                className="w-full bg-gov-primary text-white font-bold py-3 rounded-md"
                disabled={l3Answer.trim().length < 30 || loading}
                onClick={handleSubmitL3}
              >
                {loading
                  ? <><Loader2 size={16} className="mr-2 animate-spin" />Evaluating with AI Rubric…</>
                  : <><Sparkles size={18} className="mr-2" />Submit Final Response</>
                }
              </Button>
            </div>
          )}

          {/* ── PHASE: COMPLETED ── */}
          {phase === 'COMPLETED' && l3Result && (
            <div className="flex flex-col items-center py-10 max-w-2xl mx-auto text-center gap-6">
              <div className="bg-gov-primary border border-gov-primary rounded-full p-6">
                <Sparkles size={48} className="text-gov-accent animate-pulse" />
              </div>
              <div className="text-white bg-gov-primary rounded-xl p-8 w-full">
                <h3 className="text-2xl font-bold mb-2">Assessment Complete!</h3>
                <p className="text-white/80 text-sm mb-6">
                  Material: {material?.detectedTopic}
                </p>

                <div className="grid grid-cols-3 gap-4 mb-6">
                  <div className="bg-white/10 rounded-lg p-4">
                    <div className="text-2xl font-bold">{l3Result.l1Score}%</div>
                    <div className="text-xs text-white/70 mt-1">Level 1 MCQ</div>
                  </div>
                  <div className="bg-white/10 rounded-lg p-4">
                    <div className="text-2xl font-bold">{l3Result.l2Score}</div>
                    <div className="text-xs text-white/70 mt-1">Level 2 Written</div>
                  </div>
                  <div className="bg-white/10 rounded-lg p-4">
                    <div className="text-2xl font-bold">{l3Result.l3Score}</div>
                    <div className="text-xs text-white/70 mt-1">Level 3 Scenario</div>
                  </div>
                </div>

                <div className="bg-gov-accent rounded-lg p-4 mb-6">
                  <div className="text-3xl font-bold">{l3Result.overallScore}/100</div>
                  <div className="text-sm text-white/90 mt-1">Overall Score</div>
                </div>

                {l3Result.feedback && (
                  <div className="bg-white/10 rounded-lg p-4 text-left text-sm text-white/90">
                    <div className="font-bold mb-1 text-white">Evaluator Feedback:</div>
                    {l3Result.feedback}
                  </div>
                )}
              </div>

              <p className="text-sm text-gov-text-secondary">
                Your evidence has been logged to your profile. Competency scores updated.
              </p>

              <Button
                className="bg-gov-primary text-white font-bold"
                onClick={() => window.location.href = '/'}
              >
                Return to Command Center
              </Button>
            </div>
          )}

        </div>
      </div>
    </>
  );
}
