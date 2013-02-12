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

package org.mart.crs.management.features;

import org.mart.crs.config.ExecParams;
import org.mart.crs.config.Settings;
import org.mart.crs.core.pcp.PCP;
import org.mart.crs.core.spectrum.SpectrumImpl;
import org.mart.crs.core.spectrum.reassigned.ReassignedSpectrum;
import org.mart.crs.core.spectrum.reassigned.ReassignedSpectrumHarmonicPart;
import org.mart.crs.core.spectrum.reassigned.ReassignedSpectrumPercussivePart;
import org.mart.crs.management.features.extractor.SpectrogramType;

/**
 * Performs initialization of core elements, such as Spectrum and Chroma using ExecParams configuration
 *
 * @version 1.0 3/30/12 6:27 PM
 * @author: Hut
 */
public class CoreElementsInitializer {

    public static PCP initializePCPWithExecParamsData(int pcpType, double refFrequency, ExecParams execParams) {
        PCP pcp = PCP.getPCP(PCP.BASIC_ALG, refFrequency, execParams.pcpAveragingFactor, execParams.numberOfSemitonesPerBin, execParams.isToNormalizeFeatureVectors, execParams.startMidiNote, execParams.endMidiNote, execParams.spectrumMagnitudeRateForChromaCalculation);
        pcp.setToUSEPCPLogTransform(Settings.isToUSEPCPLogTransform);
        pcp.setToUSESpectralLogTransform(Settings.isToUSESpectralLogTransform);
        return pcp;
    }

    public static PCP initializeBassPCPWithExecParamsData(int pcpType, double refFrequency, ExecParams execParams) {
        PCP pcp = PCP.getPCP(PCP.BASIC_ALG, refFrequency, execParams.pcpAveragingFactor, execParams.numberOfSemitonesPerBin, execParams.isToNormalizeFeatureVectors, execParams.startMidiNoteBass, execParams.endMidiNoteBass, execParams.spectrumMagnitudeRateForChromaCalculation);
        pcp.setToUSEPCPLogTransform(Settings.isToUSEPCPLogTransform);
        pcp.setToUSESpectralLogTransform(Settings.isToUSESpectralLogTransform);
        return pcp;
    }


    public static SpectrumImpl initializeSpectrumWithExecParamsData(SpectrogramType spectrogramType, float[] samples, float sampleRate, ExecParams execParams) {
        switch (spectrogramType) {
            case FFT_BASED:
                return new SpectrumImpl(samples, sampleRate, execParams.windowLength, execParams.windowType, execParams.overlapping);
            case STANDARD:
                return new ReassignedSpectrum(samples, sampleRate, execParams.windowLength, execParams.windowType, execParams.overlapping);
            case HARMONIC:
                return new ReassignedSpectrumHarmonicPart(samples, sampleRate, execParams.windowLength, execParams.windowType, execParams.overlapping, execParams.reassignedSpectrogramThreshold);
            case PERCUSSIVE:
                return new ReassignedSpectrumPercussivePart(samples, sampleRate, execParams.windowLength, execParams.windowType, execParams.overlapping, execParams.reassignedSpectrogramThreshold);
        }
        throw new IllegalArgumentException(String.format("Cannot initialize spectrum of type %s", spectrogramType));
    }


}
