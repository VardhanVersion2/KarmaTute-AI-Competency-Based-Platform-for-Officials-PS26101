import React, { useState, useEffect, useRef } from 'react';
import { Mic, MicOff, X, HelpCircle, MessageSquare } from 'lucide-react';
import { useVoice } from '../../features/karma-vaani/useVoice';
import { useKarmaVaaniBrain } from './useKarmaVaaniBrain';
import { useApp } from '../../context/AppContext';

export function KarmaVaaniAssistant() {
  const [isOpen, setIsOpen] = useState(false);
  const [hasOpenedOnce, setHasOpenedOnce] = useState(false);
  const [assistantMessage, setAssistantMessage] = useState("Hello! I am KarmaVaani, your Helping Hand. Click the microphone and tell me where you want to go or what you want to learn about the platform.");
  const [isSpeaking, setIsSpeaking] = useState(false);
  const [isAutoMode, setIsAutoMode] = useState(false);
  
  const [currentEmotion, setCurrentEmotion] = useState('calm');
  const { isListening, transcript, error, startListening, stopListening } = useVoice();
  const { speak, stop, processCommand } = useKarmaVaaniBrain();
  
  // Ref to track the last processed transcript to avoid double-processing
  const lastProcessedTranscript = useRef('');
  const silenceTimeoutRef = useRef<NodeJS.Timeout | null>(null);

  // Auto-recommendation on first load
  useEffect(() => {
    if (hasOpenedOnce) return;
    const timer = setTimeout(() => {
      setIsOpen(true);
      setHasOpenedOnce(true);
      setIsSpeaking(true);
      speak("[encouraging] Welcome! I am Karma Vaani, your AI Helping Hand. Kahiye kaise madad karoon?", 'hinglish', () => setIsSpeaking(false));
    }, 5000);
    return () => clearTimeout(timer);
  }, [speak, hasOpenedOnce]);


  // Handle incoming voice transcript
  useEffect(() => {
    if (error) {
      setAssistantMessage(`Error: ${error}`);
      return;
    }

    if (transcript && transcript !== lastProcessedTranscript.current) {
      if (isSpeaking) {
          stopListening();
          return;
      }
      setAssistantMessage(`You: "${transcript}"`);
      
      if (silenceTimeoutRef.current) clearTimeout(silenceTimeoutRef.current);
      
      // Wait for user to stop talking
      silenceTimeoutRef.current = setTimeout(() => {
        stopListening();
        lastProcessedTranscript.current = transcript;
        
        const { response, lang } = processCommand(transcript);
        
        let cleanResponse = response;
        let emotionMatch = response.match(/^\[(.*?)\]\s*(.*)/);
        if (emotionMatch) {
          setCurrentEmotion(emotionMatch[1].toLowerCase());
          cleanResponse = emotionMatch[2];
        }

        setAssistantMessage(cleanResponse);
        
        setIsSpeaking(true);
        speak(response, lang, () => {
           setIsSpeaking(false);
           setCurrentEmotion('calm');
           if (isAutoMode) {
              setAssistantMessage("Listening...");
              setTimeout(startListening, 300);
           }
        });
      }, 800);
    }
    
    return () => {
      if (silenceTimeoutRef.current) clearTimeout(silenceTimeoutRef.current);
    }
  }, [transcript, error, stopListening, processCommand, speak, isAutoMode, startListening, isSpeaking]);

  // Robust Auto-Mode Recovery
  useEffect(() => {
    if (isAutoMode && !isListening && !isSpeaking) {
       // If mic dies quietly (e.g. no-speech), turn it back on after a brief delay
       const t = setTimeout(() => {
          if (!isListening && !isSpeaking) {
             setAssistantMessage("Listening...");
             startListening();
          }
       }, 1000);
       return () => clearTimeout(t);
    }
  }, [isAutoMode, isListening, isSpeaking, startListening]);

  const toggleVoice = () => {
    if (isListening) {
      stopListening();
      if (silenceTimeoutRef.current) clearTimeout(silenceTimeoutRef.current);
    } else {
      if (isSpeaking) {
          stop();
          setIsSpeaking(false);
      }
      lastProcessedTranscript.current = ''; 
      setAssistantMessage("Listening... Speak your command.");
      startListening();
    }
  };

  const handleClose = () => {
    if (isOpen) {
       stop();
       setIsSpeaking(false);
       stopListening();
       setIsAutoMode(false);
    }
    setIsOpen(false);
    if (!hasOpenedOnce) setHasOpenedOnce(true);
  };

  return (
    <>
      <button 
        onClick={handleClose}
        className={`fixed bottom-6 right-6 z-50 p-4 rounded-full shadow-2xl transition-all duration-300 ${isOpen ? 'bg-gov-surface text-gov-primary border border-gov-border' : 'bg-gov-primary text-white hover:bg-gov-primary-hover hover:scale-105'}`}
        aria-label="Toggle KarmaVaani Assistant"
      >
        {isOpen ? <X size={24} /> : <MessageSquare size={24} />}
        
        {!isOpen && !hasOpenedOnce && (
            <span className="absolute -top-10 right-0 bg-gov-accent text-white text-xs px-3 py-1.5 rounded-sm whitespace-nowrap shadow-lg animate-bounce font-bold tracking-wide">
                Helping Hand Available
            </span>
        )}
      </button>

      <div 
        className={`fixed bottom-24 right-6 z-50 w-80 sm:w-96 bg-gov-surface border border-gov-border rounded-lg shadow-[0_10px_40px_rgba(0,51,102,0.15)] transition-all duration-500 overflow-hidden ${isOpen ? 'opacity-100 translate-y-0 scale-100' : 'opacity-0 translate-y-10 scale-95 pointer-events-none'}`}
      >
        <div className="bg-gov-primary p-4 flex justify-between items-center text-white">
            <div className="flex items-center gap-3">
               <div className={`w-8 h-8 rounded-full flex items-center justify-center relative overflow-hidden transition-colors duration-500
                 ${currentEmotion === 'celebratory' ? 'bg-gov-success text-white shadow-[0_0_15px_rgba(22,163,74,0.6)]' :
                   currentEmotion === 'urgent' ? 'bg-orange-500 text-white shadow-[0_0_15px_rgba(249,115,22,0.6)]' :
                   currentEmotion === 'concerned' ? 'bg-blue-300 text-white' :
                   currentEmotion === 'encouraging' ? 'bg-pink-500 text-white' :
                   currentEmotion === 'firm' ? 'bg-gov-primary-hover text-white' :
                   'bg-gov-primary text-white'
                 }`}>
                  <div className="flex gap-1 items-end h-4 relative z-10">
                      <div className={`w-1 bg-white rounded-t-sm transition-all duration-300 ${isSpeaking ? 'animate-[wave_1s_infinite_0.1s]' : isListening ? 'h-3 animate-pulse bg-white/80' : 'h-1'}`} />
                      <div className={`w-1 bg-white rounded-t-sm transition-all duration-300 ${isSpeaking ? 'animate-[wave_1s_infinite_0.3s]' : isListening ? 'h-4 animate-pulse bg-white/80' : 'h-2'}`} />
                      <div className={`w-1 bg-white rounded-t-sm transition-all duration-300 ${isSpeaking ? 'animate-[wave_1s_infinite_0.5s]' : isListening ? 'h-2 animate-pulse bg-white/80' : 'h-1'}`} />
                  </div>
               </div>
               <div className="flex flex-col">
                  <span className="text-sm font-bold text-white tracking-wide flex items-center gap-2">
                    KARMA VAANI 
                    <span className="text-[9px] uppercase bg-white/20 px-1.5 py-0.5 rounded opacity-80">{currentEmotion}</span>
                  </span>
                  <span className="text-[10px] text-white/70 flex flex-wrap items-center gap-1">National AI Helper <span className="bg-white/10 px-1 py-0.5 rounded mt-0.5">Bilingual (EN & HI)</span></span>
               </div>
            </div>
          <HelpCircle size={18} className="text-white/50" />
        </div>

        <div className="p-6 min-h-[120px] max-h-[250px] overflow-y-auto bg-gov-bg flex flex-col justify-center">
            <p className={`text-sm leading-relaxed font-medium text-center ${isListening ? 'text-gov-accent animate-pulse' : 'text-gov-text-primary'}`}>
                {isListening && transcript ? `"${transcript}"` : assistantMessage}
            </p>
        </div>

        <div className="p-4 border-t border-gov-border bg-gov-surface flex flex-col items-center justify-center gap-3 relative">
            <button
                onClick={toggleVoice}
                className={`relative flex items-center justify-center w-14 h-14 rounded-full transition-all duration-300 ${isListening ? 'bg-gov-danger text-white shadow-[0_0_20px_rgba(220,38,38,0.4)] scale-110' : 'bg-gov-primary text-white hover:bg-gov-primary-hover'}`}
            >
                {isListening ? <MicOff size={24} className="animate-pulse" /> : <Mic size={24} />}
                {isListening && (
                    <span className="absolute -bottom-8 text-[10px] font-bold text-gov-danger uppercase tracking-widest whitespace-nowrap">
                        Listening...
                    </span>
                )}
            </button>
            
            <label className="flex items-center gap-2 mt-4 cursor-pointer">
              <input 
                type="checkbox" 
                checked={isAutoMode} 
                onChange={(e) => setIsAutoMode(e.target.checked)} 
                className="w-3.5 h-3.5 accent-gov-primary"
              />
              <span className="text-xs font-bold text-gov-text-secondary uppercase tracking-wider">Continuous Conversation</span>
            </label>
        </div>
        
        {/* Style for custom animations */}
        <style dangerouslySetInnerHTML={{__html: `
          @keyframes wave {
            0%, 100% { height: 4px; }
            50% { height: 16px; }
          }
        `}} />
      </div>
    </>
  );
}
