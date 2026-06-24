/**
 * SoundManager.ts
 * 
 * High-performance, low-latency audio synthesizer using the Web Audio API.
 * Designed to capture bouncy, bright, and charming cartoon-like sounds that are
 * perfect for a kid-friendly Tap interactive game.
 * 
 * Bypasses need for external file requests/assets by synthesizing pure waves on-the-fly!
 */

class SoundManager {
  private ctx: AudioContext | null = null;

  /**
   * Initializes the AudioContext lazily on user gesture to comply with modern browser
   * Autoplay Policies.
   */
  private initCtx(): AudioContext {
    if (!this.ctx) {
      const AudioCtxClass = window.AudioContext || (window as any).webkitAudioContext;
      if (!AudioCtxClass) {
        throw new Error('Web Audio API is not supported in this environment.');
      }
      this.ctx = new AudioCtxClass();
    }
    
    // Resume context if suspended (common browser policy behavior)
    if (this.ctx.state === 'suspended') {
      this.ctx.resume();
    }
    
    return this.ctx;
  }

  /**
   * Plays a quick, bouncy cartoon bubble "pop" sound.
   * Leverages a rapid exponential frequency slide (sweep) from a bass range to a high pop tone.
   */
  public playPop() {
    try {
      const ctx = this.initCtx();
      const now = ctx.currentTime;

      const osc = ctx.createOscillator();
      const gainNode = ctx.createGain();

      osc.connect(gainNode);
      gainNode.connect(ctx.destination);

      // Playful bouncy pop settings
      osc.type = 'sine'; // Pure clean tone
      osc.frequency.setValueAtTime(140, now); // Start with a resonant low punch
      osc.frequency.exponentialRampToValueAtTime(780, now + 0.11); // Sweep up rapidly

      // Linear envelope attack and exponential volume decay
      gainNode.gain.setValueAtTime(0.01, now);
      gainNode.gain.linearRampToValueAtTime(0.35, now + 0.015); // Rapid volume envelope peak
      gainNode.gain.exponentialRampToValueAtTime(0.01, now + 0.14); // Quick snappy fadeout

      osc.start(now);
      osc.stop(now + 0.16);
    } catch (error) {
      console.warn('Web Audio PlayPop failed gracefully:', error);
    }
  }

  /**
   * Plays a triumphant, bright victory chime whenever a sticker/reward is earned.
   * Plays an ascending Major arpeggio (C5 -> E5 -> G5 -> C6) using warm triangle waves,
   * coupled with a gentle pitch wobble (vibrato/glide) to feel magical and satisfying.
   */
  public playVictory() {
    try {
      const ctx = this.initCtx();
      const now = ctx.currentTime;

      // Bright ascending golden-ratio arpeggio chords (C5, E5, G5, C6)
      const notes = [523.25, 659.25, 783.99, 1046.50];

      notes.forEach((frequencyValue, index) => {
        const noteTriggerTime = now + (index * 0.09); // Flowing harp-like delay

        const osc = ctx.createOscillator();
        const vibrato = ctx.createOscillator();
        const vibratoGain = ctx.createGain();
        const gainNode = ctx.createGain();

        // Connect vibrato LFO to modulate oscillator frequency
        vibrato.connect(vibratoGain);
        vibratoGain.connect(osc.frequency);
        
        osc.connect(gainNode);
        gainNode.connect(ctx.destination);

        // Chime configuration
        osc.type = 'triangle'; // Warm, woodwind-like harmonic profile
        osc.frequency.setValueAtTime(frequencyValue, noteTriggerTime);
        
        // Gentle frequency pitch glide
        osc.frequency.exponentialRampToValueAtTime(frequencyValue * 1.015, noteTriggerTime + 0.4);

        // Vibrato configuration for a sparkling shimmer!
        vibrato.frequency.setValueAtTime(6.5, noteTriggerTime); // 6.5Hz rate
        vibratoGain.gain.setValueAtTime(frequencyValue * 0.01, noteTriggerTime); // Shimmer depth

        // Volume envelope
        gainNode.gain.setValueAtTime(0.01, noteTriggerTime);
        gainNode.gain.linearRampToValueAtTime(0.18, noteTriggerTime + 0.04); // Smooth rise
        gainNode.gain.exponentialRampToValueAtTime(0.005, noteTriggerTime + 0.55); // Magical decay

        vibrato.start(noteTriggerTime);
        osc.start(noteTriggerTime);

        vibrato.stop(noteTriggerTime + 0.6);
        osc.stop(noteTriggerTime + 0.6);
      });
    } catch (error) {
      console.warn('Web Audio PlayVictory failed gracefully:', error);
    }
  }
}

export const webAudioSoundManager = new SoundManager();
