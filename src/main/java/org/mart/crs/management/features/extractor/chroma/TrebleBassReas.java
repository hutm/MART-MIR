/*
 * Copyright (c) 2008-2013 Maksim Khadkevich and Fondazione Bruno Kessler.
 *
 * This file is part of MART.
 * MART is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2, as published
 * by the Free Software Foundation.
 *
 * MART is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with MART; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.mart.crs.management.features.extractor.chroma;

import org.mart.crs.core.pcp.PCP;
import org.mart.crs.core.spectrum.reassigned.ReassignedSpectrum;
import org.mart.crs.management.config.Configuration;
import org.mart.crs.management.features.CoreElementsInitializer;
import org.mart.crs.management.features.extractor.FeaturesExtractorHTK;
import org.mart.crs.management.features.extractor.SpectrogramType;

/**
 * Use one stream of 2 chroma vectors: bass and treble
 *
 * @version 1.0 15-Oct-2010 00:38:48
 * @author: Hut
 */
public class TrebleBassReas extends FeaturesExtractorHTK {

    @Override
    public int getVectorSize() {
        return Configuration.NUMBER_OF_SEMITONES_IN_OCTAVE * 2;
    }


    public void extractGlobalFeatures(double refFrequency) {
        float[] samples = audioReader.getSamples();
        addCombinedVectors(samples, refFrequency);
    }


    protected void addCombinedVectors(float[] samples, double refFrequency){
        ReassignedSpectrum spectrumBass = (ReassignedSpectrum) CoreElementsInitializer.initializeSpectrumWithExecParamsData(SpectrogramType.values()[execParams.reassignedSpectrogramType], samples, audioReader.getSampleRate(), execParams);
        PCP pcpBass = CoreElementsInitializer.initializePCPWithExecParamsData(PCP.BASIC_ALG, refFrequency, execParams);
        pcpBass.initReassignedSpectrum(spectrumBass);
        globalVectors.add(pcpBass.getPCP());

        ReassignedSpectrum spectrumTreble = (ReassignedSpectrum) CoreElementsInitializer.initializeSpectrumWithExecParamsData(SpectrogramType.values()[execParams.reassignedSpectrogramType], samples, audioReader.getSampleRate(), execParams);
        PCP pcpTreble = CoreElementsInitializer.initializeBassPCPWithExecParamsData(PCP.BASIC_ALG, refFrequency, execParams);
        pcpTreble.initReassignedSpectrum(spectrumTreble);
        globalVectors.add(pcpTreble.getPCP());
    }


}
