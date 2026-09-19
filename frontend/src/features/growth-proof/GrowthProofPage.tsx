import React, { useState, useEffect, useCallback } from 'react';
import { API_BASE_URL } from '../../services/apiConfig';
import { PageTutorialModal } from '../../components/ui/PageTutorialModal';
import { GlassCore, type CoreState } from '../../components/glass-core/GlassCore';
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '../../components/ui/Table';
import { Button } from '../../components/ui/Button';
import { useToast } from '../../context/ToastContext';
import { useApp } from '../../context/AppContext';
import { RefreshCw, ShieldCheck, Download, History, ArrowRight, Lock, Hash } from 'lucide-react';
import {
  getUserEvidence,
  getCompetencySnapshots,
  type EvidenceRecord,
} from '../../services/evidenceApi';

const DEMO_USER_ID = 1;

export function GrowthProofPage() {
  const toast = useToast();
  const [evidence, setEvidence] = useState<EvidenceRecord[]>([]);
  const [certificates, setCertificates] = useState<any[]>([]); // Mocked or fetched certificates
  const [coreState, setCoreState] = useState<CoreState>('idle');
  const [isLoading, setIsLoading] = useState(true);

  const loadData = useCallback(async () => {
    setIsLoading(true);
    setCoreState('processing');
    try {
      const isFresh = localStorage.getItem('demo_fresh_start') === 'true';
      if (isFresh) {
        setEvidence([]);
        setCertificates([]);
        setCoreState('success');
        setIsLoading(false);
        return;
      }
      const evData = await getUserEvidence(DEMO_USER_ID);
      
      // Fetch certificates from the actual endpoint if wired up, else mock for UI completeness
      try {
        const certRes = await fetch(`${API_BASE_URL}/api/certificate/user/${DEMO_USER_ID}`);
        if(certRes.ok) {
           const certs = await certRes.json();
           setCertificates(certs);
        }
      } catch(e) {
        // Fallback demo certificate if backend certificate controller isn't seeded for this user
        setCertificates([{
          id: 'CERT-2026-MOSPI-8819A',
          competencyId: 2,
          competencyName: 'DPDP Act & Digital Privacy',
          status: 'ISSUED',
          issuedAt: new Date().toISOString(),
          integrityHash: '8f434346648f6b96df89dda901c5176b10a6d83961dd3c1ac88b59b2dc327aa4'
        }]);
      }

      setEvidence(evData.sort((a,b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()));
      setCoreState('success');
    } catch (err: any) {
      toast.error('Failed to load capability records');
      setCoreState('error');
    } finally {
      setIsLoading(false);
    }
  }, [toast]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  if (isLoading && evidence.length === 0) {
    return (
      <div className="flex flex-col gap-6 p-6 animate-pulse">
         <div className="h-10 bg-gov-border rounded w-1/3" />
         <div className="h-48 bg-gov-surface-muted rounded" />
      </div>
    );
  }

  return (
    <>
      <PageTutorialModal 
        pageId="growth-proof"
        title="Proof of Work"
        what="Your evidence-backed capability record."
        why="See how evidence changed your competency state."
        how="Review trajectory and evidence."
        psAsk="continuous assessment and learning footprint."
        karmaTuteBuild="Immutable evidence ledger and certificate issuance."
        differentiator="Authoritative evidence model ensures no competency score exists without strict provenance."
      />

      <div className="flex flex-col gap-8 max-w-[1540px] mx-auto pb-12 animate-in fade-in">
        
        {/* HEADER */}
        <div className="flex flex-col md:flex-row md:items-end justify-between gap-4 border-b border-gov-border pb-4">
          <div>
            <h1 className="text-2xl font-extrabold text-gov-primary tracking-tight">Proof of Work</h1>
            <p className="text-sm text-gov-text-secondary mt-1 font-medium">Immutable capability verification & audit ledger</p>
          </div>
          <Button variant="outline" size="sm" onClick={loadData} leftIcon={<RefreshCw size={14} />} className="bg-white border-gov-border text-gov-text-primary hover:bg-gov-surface-muted">
            Refresh Ledger
          </Button>
        </div>

        {/* CERTIFICATE VERIFICATION AREA */}
        <div className="flex flex-col gap-4">
          <div className="flex items-center gap-2 border-b border-gov-border pb-2">
            <ShieldCheck size={20} className="text-gov-success" />
            <h3 className="text-sm font-bold text-gov-text-primary uppercase tracking-wide">Issued Certifications</h3>
          </div>
          
          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
            {certificates.length === 0 ? (
               <div className="col-span-full p-8 border border-gov-border border-dashed text-center text-gov-text-muted text-sm bg-gov-surface-muted rounded-sm">
                 No official certificates issued yet. Resolve high-priority competency gaps to trigger eligibility.
               </div>
            ) : (
              certificates.map((cert, i) => (
                <div key={i} className="bg-gov-surface border border-gov-border rounded-sm shadow-sm flex flex-col relative overflow-hidden">
                  <div className="absolute top-0 right-0 p-4 opacity-5 pointer-events-none">
                    <Award size={100} />
                  </div>
                  <div className="p-6 pb-4 border-b border-gov-border bg-gov-surface-muted flex items-start justify-between">
                    <div>
                      <span className="text-[10px] font-bold text-gov-success bg-gov-success-bg px-2 py-0.5 rounded-sm border border-gov-success-border mb-2 inline-block">
                        VERIFIED · {cert.status}
                      </span>
                      <h4 className="text-lg font-bold text-gov-primary leading-tight">{cert.competencyName || `Competency ID ${cert.competencyId}`}</h4>
                    </div>
                  </div>
                  <div className="p-6 flex flex-col gap-4">
                    <div className="flex justify-between items-center text-xs">
                      <span className="text-gov-text-secondary font-bold uppercase">Issued On</span>
                      <span className="font-medium text-gov-text-primary">{new Date(cert.issuedAt).toLocaleDateString()}</span>
                    </div>
                    <div className="flex justify-between items-center text-xs">
                      <span className="text-gov-text-secondary font-bold uppercase">Ledger ID</span>
                      <span className="font-mono text-gov-text-primary bg-gov-surface-muted px-1.5 py-0.5 border border-gov-border">{cert.id}</span>
                    </div>
                    <div className="flex flex-col gap-1 mt-2">
                      <span className="text-[10px] text-gov-text-secondary font-bold uppercase flex items-center gap-1"><Lock size={10}/> Integrity Hash (SHA-256)</span>
                      <span className="text-[10px] font-mono text-gov-text-muted truncate" title={cert.integrityHash}>{cert.integrityHash}</span>
                    </div>
                  </div>
                  <div className="mt-auto p-4 border-t border-gov-border bg-gov-bg flex justify-end">
                    <Button size="sm" variant="outline" leftIcon={<Download size={14}/>} className="bg-white border-gov-border text-gov-text-primary hover:bg-gov-surface-muted w-full justify-center">
                      Download PDF
                    </Button>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        {/* EVIDENCE LEDGER */}
        <div className="flex flex-col gap-4 mt-8">
          <div className="flex items-center gap-2 border-b border-gov-border pb-2">
            <History size={20} className="text-gov-primary" />
            <h3 className="text-sm font-bold text-gov-text-primary uppercase tracking-wide">Evidence Provenance Ledger</h3>
          </div>

          <div className="bg-gov-surface border border-gov-border rounded-sm shadow-sm overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-gov-surface-muted border-b border-gov-border text-xs text-gov-text-secondary uppercase tracking-wider">
                  <th className="p-4 font-bold w-48">Timestamp</th>
                  <th className="p-4 font-bold">Provenance Source</th>
                  <th className="p-4 font-bold">Evidence Type</th>
                  <th className="p-4 font-bold text-center">Score</th>
                  <th className="p-4 font-bold text-center">Confidence</th>
                  <th className="p-4 font-bold text-center">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gov-border text-sm">
                {evidence.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="p-8 text-center text-gov-text-muted bg-gov-bg font-medium">
                      No evidence records exist. Visit the Execution Lab to generate capability proofs.
                    </td>
                  </tr>
                ) : (
                  evidence.map((ev, i) => (
                    <tr key={ev.id || i} className="hover:bg-gov-surface-muted transition-colors">
                      <td className="p-4 text-gov-text-secondary font-mono text-xs">
                        {new Date(ev.timestamp).toLocaleString()}
                      </td>
                      <td className="p-4">
                        <div className="font-bold text-gov-primary">{ev.sourceRef || 'System Assessment'}</div>
                        <div className="text-xs text-gov-text-secondary mt-0.5 flex items-center gap-1">
                          <Hash size={10} /> ID: {ev.id?.substring(0,8) || '...'}
                        </div>
                      </td>
                      <td className="p-4 text-xs font-bold text-gov-text-secondary">
                        <span className="bg-gov-bg px-2 py-1 rounded border border-gov-border inline-block">
                          {ev.source || 'EXECUTION_LAB'}
                        </span>
                      </td>
                      <td className="p-4 text-center font-mono font-bold text-gov-primary">
                        {ev.normalizedScore ? (ev.normalizedScore * 100).toFixed(1) : (ev.rawScore || 0).toFixed(1)}
                      </td>
                      <td className="p-4 text-center">
                        <span className="font-mono text-xs font-bold bg-gov-info-bg text-gov-info px-2 py-1 rounded-sm border border-gov-info-border inline-block">
                          {(ev.confidence * 100).toFixed(0)}%
                        </span>
                      </td>
                      <td className="p-4 text-center">
                        <span className={`text-[10px] font-bold uppercase px-2 py-1 rounded-sm border inline-block ${
                          ev.reviewStatus === 'VERIFIED' ? 'bg-gov-success-bg text-gov-success border-gov-success-border' :
                          ev.reviewStatus === 'REVIEW_REQUIRED' ? 'bg-gov-warning-bg text-gov-warning border-gov-warning-border' :
                          'bg-gov-surface-muted text-gov-text-secondary border-gov-border'
                        }`}>
                          {ev.reviewStatus || 'PROCESSED'}
                        </span>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>

      </div>
    </>
  );
}
