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

package org.mart.crs.management.features.manager;

import org.apache.log4j.Logger;
import org.mart.crs.config.ExecParams;
import org.mart.crs.config.Extensions;
import org.mart.crs.config.Settings;
import org.mart.crs.core.AudioReader;
import org.mart.crs.logging.CRSLogger;
import org.mart.crs.management.features.extractor.FeaturesExtractorHTK;
import org.mart.crs.management.features.extractor.chroma.SpectrumBased;
import org.mart.crs.management.features.extractor.unused.EllisBassReas;
import org.mart.crs.management.label.LabelsSource;
import org.mart.crs.model.htk.parser.chord.ChordHTKParser;
import org.mart.crs.utils.helper.Helper;

import java.util.ArrayList;
import java.util.List;

/**
 * @version 1.0 4/11/12 5:05 PM
 * @author: Hut
 */
public class FeaturesManager {

    protected static Logger logger = CRSLogger.getLogger(FeaturesManagerChord.class);

    public static final String packageExtractors = "org.mart.crs.management.features.extractor.";


    protected ExecParams execParams;

    protected List<FeaturesExtractorHTK> featureExtractorList;
    public static int[] featureSizes;
    public int chrSamplingPeriod;



    protected String outDirPath;
    protected boolean isForTraining;

    /**
     * The song filePath to be processed
     */
    protected String songFilePath;

    protected float songDuration;

    /**
     * This is a copy of the static list to process
     */
    protected List<FeaturesExtractorHTK> featureExtractorToWorkWith;



    protected LabelsSource chordLabelSource;
    protected LabelsSource beatLabelsSource;


    public FeaturesManager(String songFilePath, String outDirPath, boolean isForTraining, ExecParams execParams) {
        this.execParams = execParams;
        this.outDirPath = outDirPath;
        this.isForTraining = isForTraining;
        this.songFilePath = songFilePath;
        this.beatLabelsSource = new LabelsSource(Settings.beatLabelsGroundTruthDir, true, "beatGT", Extensions.BEAT_EXTENSIONS);
        this.chordLabelSource = new LabelsSource(Settings.chordLabelsGroundTruthDir, true, "chordGT", Extensions.LABEL_EXT);
        reinitializeFeaturesManager();
        initializeWithSong(songFilePath);
    }


    public void reinitializeFeaturesManager() {
        chrSamplingPeriod = getChrSamplingPeriod(execParams);

        featureExtractorList = new ArrayList<FeaturesExtractorHTK>();
        featureSizes = new int[execParams.featureExtractors.length];

        int counter = 0;
        for (String extractor : execParams.featureExtractors) {
            try {
                Class featureExtractorClass = Class.forName(packageExtractors + extractor);
                FeaturesExtractorHTK newExtractor = (FeaturesExtractorHTK) featureExtractorClass.newInstance();
                featureExtractorList.add(newExtractor);
                featureSizes[counter] = newExtractor.getVectorSize();
                if (execParams.extractDeltaCoefficients) {
                    featureSizes[counter] *= 2;
                }
                counter++;
            } catch (Exception e) {
                logger.error(String.format("Cannon instantiate class %s", extractor));
                logger.error(Helper.getStackTrace(e));
            }
        }
    }


    public static void initializeFeatureSize() {
        int counter = 0;
        featureSizes = new int[ExecParams._initialExecParameters.featureExtractors.length];
        for (String extractor : ExecParams._initialExecParameters.featureExtractors) {
            try {
                Class featureExtractorClass = Class.forName(packageExtractors + extractor);
                FeaturesExtractorHTK newExtractor = (FeaturesExtractorHTK) featureExtractorClass.newInstance();
                featureSizes[counter] = newExtractor.getVectorSize();
                if (ExecParams._initialExecParameters.extractDeltaCoefficients) {
                    featureSizes[counter] *= 2;
                }
                counter++;
            } catch (Exception e) {
                logger.error(String.format("Cannon instantiate class %s", extractor));
                logger.error(Helper.getStackTrace(e));
            }
        }
    }



    public void initializeWithSong(String songFilePath) {
        AudioReader audioReader = new AudioReader(songFilePath, execParams.samplingRate);
        this.songDuration = audioReader.getDuration();
        featureExtractorToWorkWith = new ArrayList<FeaturesExtractorHTK>();
        for (FeaturesExtractorHTK featuresExtractor : featureExtractorList) {
            FeaturesExtractorHTK anExtractorToWork = null;
            try {
                anExtractorToWork = featuresExtractor.getClass().newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            anExtractorToWork.setExecParams(execParams);
            featureExtractorToWorkWith.add(anExtractorToWork);

            if (anExtractorToWork instanceof SpectrumBased || anExtractorToWork instanceof EllisBassReas) {
                anExtractorToWork.initialize(audioReader);
            } else {
                anExtractorToWork.initialize(songFilePath);
            }
        }
    }


    public static int getChrSamplingPeriod(ExecParams execParams) {
        return (int) Math.floor(execParams.windowLength / execParams.samplingRate * ChordHTKParser.FEATURE_SAMPLE_RATE * (1 - execParams.overlapping) * execParams.pcpAveragingFactor);
    }


    /**
     * calculate index in the feature vector array that corresponds to the given time instant
     *
     * @param timeInstant timeInstant
     * @return index
     */
    public static int getIndexForTimeInstant(double timeInstant, ExecParams execParams) {
        return (int) Math.floor(timeInstant / (getChrSamplingPeriod(execParams) / ChordHTKParser.FEATURE_SAMPLE_RATE));
    }

    public static double getTimePeriodForFramesNumber(int frames, ExecParams execParams) {
        return frames * (getChrSamplingPeriod(execParams) / ChordHTKParser.FEATURE_SAMPLE_RATE);
    }







}
