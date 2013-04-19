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

package org.mart.crs.management.features.extractor.unused;

import org.apache.log4j.Logger;
import org.mart.crs.analysis.filterbank.FilterBankManagerBandPass;
import org.mart.crs.config.Extensions;
import org.mart.crs.core.AudioReader;
import org.mart.crs.core.pcp.PCP;
import org.mart.crs.core.spectrum.SpectrumImpl;
import org.mart.crs.logging.CRSLogger;
import org.mart.crs.management.config.Configuration;
import org.mart.crs.management.features.CoreElementsInitializer;
import org.mart.crs.management.features.extractor.FeaturesExtractorHTK;
import org.mart.crs.utils.helper.HelperFile;

import java.io.File;

import static org.mart.crs.utils.helper.HelperFile.getNameWithoutExtension;

/**
 * @version 1.0 Mar 3, 2010 2:18:28 PM
 * @author: Maksim Khadkevich
 */
public class FilterBankNCCBased extends FeaturesExtractorHTK {

    protected static Logger logger = CRSLogger.getLogger(FilterBankNCCBased.class);
    public static String SAVED_FEATURES_ALTERNATIVE_FOLDER = "fake";
    public static final String FILTERBANK_CONFIG_ELLIPTIC_PATH = "cfg/ellipticConfig.xml";
    //PQMF-based PCP
    public static float PQMFBasedSpectrumFrameLength; //in sec
    public static boolean isToConsiderHigerPeaks;

    protected SpectrumImpl spectrum;


    public FilterBankNCCBased() {
        super();
        HelperFile.getFile(SAVED_FEATURES_ALTERNATIVE_FOLDER).mkdirs();
    }

    @Override
    public int getVectorSize() {
        return Configuration.NUMBER_OF_SEMITONES_IN_OCTAVE;
    }


    public void initialize(String songFilePath) {
        super.initialize(songFilePath);
        String extractedFeaturesFilePath = SAVED_FEATURES_ALTERNATIVE_FOLDER + File.separator + HelperFile.getShortFileNameWithoutExt(songFilePath) + Extensions.CHANNELS_STORED_EXT;
        if (HelperFile.getFile(extractedFeaturesFilePath).exists()) {
            logger.info(String.format("FilterBank Channels has already been extracted for file %s", songFilePath));
            FilterBankManagerBandPass manager = FilterBankManagerBandPass.importDetectedPeriodicities(extractedFeaturesFilePath);
            spectrum = manager.getSpectrum();
        } else {
            String configFilterBankFilePath = FILTERBANK_CONFIG_ELLIPTIC_PATH;
            FilterBankManagerBandPass manager = new FilterBankManagerBandPass(new AudioReader(songFilePath), configFilterBankFilePath, PQMFBasedSpectrumFrameLength);
            manager.detectPeriodicities();
            spectrum = manager.getSpectrum();
            manager.exportDetectedPeriodicities(SAVED_FEATURES_ALTERNATIVE_FOLDER + File.separator + getNameWithoutExtension(songFilePath) + Extensions.CHANNELS_STORED_EXT);
        }
    }

    

    @Override
    public void extractGlobalFeatures(double refFrequency ) {
        globalVectors.add(producePCP(spectrum, refFrequency).getPCP());
    }

    private PCP producePCP(SpectrumImpl spectrumForPCP, double refFrequency) {
        //TODO averagingFactor calculation may be incorrect
        int averagingFactor = Math.round(((execParams.windowLength * (1 - execParams.overlapping)) / execParams.samplingRate) / PQMFBasedSpectrumFrameLength);
        execParams.pcpAveragingFactor = averagingFactor;
        PCP pcp = CoreElementsInitializer.initializePCPWithExecParamsData(PCP.BASIC_ALG, refFrequency, execParams);
        pcp.initSpectrum(spectrumForPCP);
        return pcp;
    }
}
