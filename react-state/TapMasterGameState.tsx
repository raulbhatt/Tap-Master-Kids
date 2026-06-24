import React, { createContext, useContext, useReducer, useEffect, ReactNode } from 'react';
import { webAudioSoundManager } from './SoundManager';

/**
 * Tap Master Kids - React Game State Management System
 * 
 * A robust, reliable, and modern React state context & custom hook utilizing
 * the useReducer pattern. It tracks player statistics, dynamic difficulty levels, 
 * combos, and handles the high-intensity "Fever Mode" state transitions.
 */

// --- 1. STATE DEFINITIONS ---

export interface GameState {
  currentScore: number;
  activeGameLevel: number;
  comboStreak: number;
  isFeverActive: boolean;
  feverMultiplier: number;
  highScore: number;
}

const INITIAL_STATE: GameState = {
  currentScore: 0,
  activeGameLevel: 1,
  comboStreak: 0,
  isFeverActive: false,
  feverMultiplier: 1,
  highScore: 0,
};

// --- 2. ACTION TYPES ---

type GameAction =
  | { type: 'TAP_SUCCESS'; payload: { basePoints: number } }
  | { type: 'TAP_FAIL' }
  | { type: 'TRIGGER_FEVER' }
  | { type: 'END_FEVER' }
  | { type: 'RESET_GAME' }
  | { type: 'LOAD_HIGH_SCORE'; payload: number };

// --- 3. REDUCER FUNCTION ---

function gameReducer(state: GameState, action: GameAction): GameState {
  switch (action.type) {
    case 'TAP_SUCCESS': {
      // Points are doubled when Fever Mode is active!
      const pointMultiplier = state.isFeverActive ? 2 : 1;
      const gainedPoints = action.payload.basePoints * pointMultiplier;
      const nextScore = state.currentScore + gainedPoints;
      
      // Keep track of high combo streak
      const nextStreak = state.comboStreak + 1;
      
      // Auto-calculate level advancement: Level increases every 100 points
      const nextLevel = Math.floor(nextScore / 100) + 1;

      // Update high score if player exceeds current record
      const nextHighScore = Math.max(state.highScore, nextScore);

      return {
        ...state,
        currentScore: nextScore,
        comboStreak: nextStreak,
        activeGameLevel: nextLevel,
        highScore: nextHighScore,
      };
    }

    case 'TAP_FAIL': {
      // Failing a tap instantly breaks the combo streak and turns off current Fever Mode
      return {
        ...state,
        comboStreak: 0,
        isFeverActive: false,
        feverMultiplier: 1,
      };
    }

    case 'TRIGGER_FEVER': {
      return {
        ...state,
        isFeverActive: true,
        feverMultiplier: 2,
      };
    }

    case 'END_FEVER': {
      return {
        ...state,
        isFeverActive: false,
        feverMultiplier: 1,
        comboStreak: 0, // Reset combo when hot streak cools down
      };
    }

    case 'RESET_GAME': {
      return {
        ...INITIAL_STATE,
        highScore: state.highScore, // Retain high score across game plays
      };
    }

    case 'LOAD_HIGH_SCORE': {
      return {
        ...state,
        highScore: action.payload,
      };
    }

    default:
      return state;
  }
}

// --- 4. CONTEXT CREATION ---

interface GameContextProps {
  state: GameState;
  onTapSuccess: (basePoints?: number) => void;
  onTapFailure: () => void;
  toggleFeverMode: (isActive: boolean) => void;
  resetGame: () => void;
  playVictorySound: () => void;
}

const GameContext = createContext<GameContextProps | undefined>(undefined);

// --- 5. COMPONENT PROVIDER ---

interface GameProviderProps {
  children: ReactNode;
}

export const GameProvider: React.FC<GameProviderProps> = ({ children }) => {
  const [state, dispatch] = useReducer(gameReducer, INITIAL_STATE);

  // Trigger Fever Mode when a combo streak of 5 is achieved
  useEffect(() => {
    if (state.comboStreak >= 5 && !state.isFeverActive) {
      dispatch({ type: 'TRIGGER_FEVER' });
      webAudioSoundManager.playVictory();

      // Automatically terminate fever mode after 5 seconds of extreme fun!
      const timer = setTimeout(() => {
        dispatch({ type: 'END_FEVER' });
      }, 5000);

      return () => clearTimeout(timer);
    }
  }, [state.comboStreak, state.isFeverActive]);

  // Handle local persistence of the child's High Score
  useEffect(() => {
    const savedHighScore = localStorage.getItem('tap_master_high_score');
    if (savedHighScore) {
      dispatch({ type: 'LOAD_HIGH_SCORE', payload: parseInt(savedHighScore, 10) });
    }
  }, []);

  useEffect(() => {
    if (state.currentScore > 0) {
      localStorage.setItem('tap_master_high_score', state.highScore.toString());
    }
  }, [state.highScore, state.currentScore]);

  // -- ACTION CREATORS ---

  const onTapSuccess = (basePoints: number = 10) => {
    webAudioSoundManager.playPop();
    dispatch({ type: 'TAP_SUCCESS', payload: { basePoints } });
  };

  const onTapFailure = () => {
    dispatch({ type: 'TAP_FAIL' });
  };

  const toggleFeverMode = (isActive: boolean) => {
    if (isActive) {
      dispatch({ type: 'TRIGGER_FEVER' });
      webAudioSoundManager.playVictory();
    } else {
      dispatch({ type: 'END_FEVER' });
    }
  };

  const resetGame = () => {
    dispatch({ type: 'RESET_GAME' });
  };

  const playVictorySound = () => {
    webAudioSoundManager.playVictory();
  };

  return (
    <GameContext.Provider
      value={{
        state,
        onTapSuccess,
        onTapFailure,
        toggleFeverMode,
        resetGame,
        playVictorySound,
      }}
    >
      {children}
    </GameContext.Provider>
  );
};

// --- 6. REACT CONSUMPTION HOOK ---

export const useTapMasterGame = (): GameContextProps => {
  const context = useContext(GameContext);
  if (!context) {
    throw new Error('useTapMasterGame must be used within a GameProvider');
  }
  return context;
};
