/*
 * This file is part of MART.
 * MART is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2, as published
 * by the Free Software Foundation.
 *
 * MART is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MART; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.mart.crs.management.audio.tuning;

import org.apache.log4j.Logger;
import org.mart.crs.config.ExecParams;
import org.mart.crs.core.AudioReader;
import org.mart.crs.core.spectrum.SpectrumImpl;
import org.mart.crs.core.spectrum.reassigned.ReassignedSpectrum;
import org.mart.crs.core.spectrum.reassigned.ReassignedSpectrumHarmonicPart;
import org.mart.crs.logging.CRSLogger;
import org.mart.crs.management.config.Configuration;
import org.mart.crs.utils.helper.Helper;
import org.mart.crs.utils.helper.HelperArrays;

import java.util.ArrayList;
import java.util.List;

import static org.mart.crs.management.config.Configuration.NUMBER_OF_SEMITONES_IN_OCTAVE;
import static org.mart.crs.management.config.Configuration.REFERENCE_FREQUENCY;

/**
 * @version 1.0 23-Jun-2010 14:55:23
 * @author: Hut
 */
public class F0PeaksTuner implements Tuner {

    protected static Logger logger = CRSLogger.getLogger(F0PeaksTuner.class);

    protected float[] semitoneDeviations;
    protected float referenceFreq;
    protected float[] semitoneScale;

    public static float MIN_DISTANCE_BETWEEN_PEAKS_HARMONIC_SEARCH = 0.3f; //In semitone scale. (When searching for peaks)
    public static float MAX_AMP_RELATION_BETWEEN_PEAKS_HARMONIC_SEARCH = 0.2f;
    public static float MIN_AMP_WITH_RESPECT_TO_TOTAL_FRAME_ENERGY = 0.02f;

    public static int NUMBER_OF_PEAKS = 20;

    public static float BIN_STEP = 0.1f;


    protected String audioFilePath;

    protected ReassignedSpectrum spectrum;
    protected float[][] magSpec;

    protected int numberOfBinsInReassignedSpectrogramFullRepresentation;
    protected float spectralResolution;


    public F0PeaksTuner(String audioFilePath) {
        this.audioFilePath = audioFilePath;
        initialize();
    }


    protected void initialize() {
        AudioReader audioReader = new AudioReader(audioFilePath, 0);
        spectrum = new ReassignedSpectrumHarmonicPart(audioReader,  ExecParams._initialExecParameters.windowLength, ExecParams._initialExecParameters.windowType, ExecParams._initialExecParameters.overlapping, ExecParams._initialExecParameters.reassignedSpectrogramThreshold);
        numberOfBinsInReassignedSpectrogramFullRepresentation = 8192;
        spectralResolution = 0.5f * ExecParams._initialExecParameters.samplingRate / numberOfBinsInReassignedSpectrogramFullRepresentation;
        magSpec = spectrum.getMagSpectrumStandardArrayForm(numberOfBinsInReassignedSpectrogramFullRepresentation);
    }


    public float getReferenceFrequency() {

        List<Float> extractedPeaks = new ArrayList<Float>();
        List<Float> extractedAmps = new ArrayList<Float>();

        for (float[] spectralFrame : magSpec) {
            int[] peaks = HelperArrays.searchPeakIndexes(spectralFrame, SpectrumImpl.freq2index(60, spectralResolution),
//                    spectrum.freq2index(Helper.getFreqForMIDINote(ExecParams._initialExecParameters.endMidiNote), spectralResolution), NUMBER_OF_PEAKS, MIN_DISTANCE_BETWEEN_PEAKS_HARMONIC_SEARCH, true);
                    spectrum.freq2index(Helper.getFreqForMIDINote(72), spectralResolution), NUMBER_OF_PEAKS, MIN_DISTANCE_BETWEEN_PEAKS_HARMONIC_SEARCH, true);
            float spectralEnergy = HelperArrays.sum(spectralFrame);

            for (int peakCandidate : peaks) {

//                if (spectralFrame[peakCandidate] < spectralEnergy * MIN_AMP_WITH_RESPECT_TO_TOTAL_FRAME_ENERGY) {
//                    continue;
//                }
//
//                boolean isHarmonic = false;
//                for (int i = 2; i <= 4; i++) {
//                    for (int peak : peaks) {
//                        if (peakCandidate != peak && Helper.getSemitoneDistanceAbs((peakCandidate + 0.0f) / i, peak) < MIN_DISTANCE_BETWEEN_PEAKS_HARMONIC_SEARCH) {
//                            float relation = spectralFrame[peak] / spectralFrame[peakCandidate];
//                            if (relation > MAX_AMP_RELATION_BETWEEN_PEAKS_HARMONIC_SEARCH || (1 / relation) < MAX_AMP_RELATION_BETWEEN_PEAKS_HARMONIC_SEARCH) {
//                                isHarmonic = true;
//                                break;
//                            }
//                        }
//                    }
//                    if (isHarmonic) {
//                        break;
//                    }
//                }
//                if (!isHarmonic) {
                    extractedPeaks.add(SpectrumImpl.index2freq(peakCandidate, spectralResolution));
                    extractedAmps.add(spectralFrame[peakCandidate]);
//                }
            }
        }

        float[] arrayData = new float[extractedPeaks.size()];
        float[] arrayDataAmp = new float[extractedPeaks.size()];

        for (int i = 0; i < arrayData.length; i++) {
            arrayData[i] = extractedPeaks.get(i);
            arrayDataAmp[i] = extractedAmps.get(i);
        }

        if (semitoneScale == null) {
            semitoneScale = createSemitoneScale(Helper.getFreqForMIDINote(Configuration.START_NOTE_FOR_PCP_UNWRAPPED),
                    Helper.getFreqForMIDINote(ExecParams._initialExecParameters.endMidiNote));
        }

        semitoneDeviations = getSemitoneDistance(arrayData, semitoneScale);

        int[] hist = new int[Math.round(1 / BIN_STEP)];
        for (int i = 0; i < semitoneDeviations.length; i++) {
            int value = Math.round(semitoneDeviations[i] / BIN_STEP);
            if (value >= 0) {
                hist[value] += arrayData[i];
            } else {
                hist[hist.length + value] += arrayData[i];
            }
        }
        int maxindex = HelperArrays.findIndexWithMaxValue(hist);
        float mistuning = maxindex * BIN_STEP;
        if (mistuning > 0.5) {
            mistuning--;
        }

        referenceFreq = Helper.getFreqForMIDINote(69 + mistuning);
        logger.debug(String.format("Found reference freq %5.2f", referenceFreq));
        return referenceFreq;
    }

    protected float[] createSemitoneScale(float startFreq, float endFreq) {
        int startSemitone = (int) Math.floor(NUMBER_OF_SEMITONES_IN_OCTAVE * Math.log(startFreq / REFERENCE_FREQUENCY) / Math.log(2));
        int endSemitone = (int) Math.floor(NUMBER_OF_SEMITONES_IN_OCTAVE * Math.log(endFreq / REFERENCE_FREQUENCY) / Math.log(2)) + 1;

        float[] semitoneScale = new float[endSemitone - startSemitone + 1];
        for (int i = 0; i < semitoneScale.length; i++) {
            semitoneScale[i] = (float) (REFERENCE_FREQUENCY * Math.pow(2, (i + startSemitone) / 12f));
        }
        return semitoneScale;
    }

    protected float[] getSemitoneDistance(float[] data, float[] semitoneScale) {
        float[] out = new float[data.length];
        //The last bin is the average value
        for (int i = 0; i < data.length; i++) {
            int index = Helper.getClosestLogDistanceIndex(data[i], semitoneScale);
            out[i] = (float) (NUMBER_OF_SEMITONES_IN_OCTAVE * (Math.log(data[i]) - Math.log(semitoneScale[index])) / Math.log(2));
            if(!(out[i] <= 0.5 && out[i] >= -0.5)){
                out[i] = Float.NEGATIVE_INFINITY;
            }
        }

        return out;
    }


    public static void main(String[] args) {
        F0PeaksTuner tuner = new F0PeaksTuner("/home/hut/Beatles/data/wav44100/10_-_Lovely_Rita.wav");
//        F0PeaksTuner tuner = new F0PeaksTuner("/home/hut/Beatles/data/wav44100/03_-_I'm_Only_Sleeping.wav");
        float frequency = tuner.getReferenceFrequency();
        System.out.println(frequency);
    }


}
