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

package org.mart.crs.management.features.manager;

import org.mart.crs.config.ExecParams;
import org.mart.crs.config.Extensions;
import org.mart.crs.config.Settings;
import org.mart.crs.management.beat.BeatStructure;
import org.mart.crs.management.features.FeatureVector;
import org.mart.crs.management.features.extractor.FeaturesExtractorHTK;
import org.mart.crs.management.label.chord.Root;

import java.util.ArrayList;
import java.util.List;

/**
 * @version 1.0 4/11/12 6:20 PM
 * @author: Hut
 */
public class FeaturesManagerBeatSegmentBasedTest extends FeaturesManagerSegmentBasedTest {

    protected static int maxDurationInBeats = 4;


    public FeaturesManagerBeatSegmentBasedTest(String songFilePath, String outDirPath, boolean isForTraining, ExecParams execParams) {
        super(songFilePath, outDirPath, isForTraining, execParams);
    }


    @Override
    public void extractForTest(float refFrequency, String dirName) {
        String lblFilePath = beatLabelsSource.getFilePathForSong(songFilePath);

        if (lblFilePath == null) {
            logger.warn(String.format("Lablels for file %s were not found in directory %s", songFilePath, Settings.beatLabelsGroundTruthDir));
            return;
        }

        BeatStructure beatStructure = BeatStructure.getBeatStructure(lblFilePath);
        beatStructure.fixBeatStructure(songDuration);
        double[] beats = beatStructure.getBeats();

        for (int i = 0; i < beats.length; i++) {
            double startTime = beats[i];
            for (int j = 1; j <= maxDurationInBeats; j++) {
                if(i+j > beats.length - 1){
                    continue;
                }
                double endTime = beats[i+j];

                if(startTime == endTime){
                    continue;
                }

                String filename = String.format("%5.3f_%5.3f_%s", startTime, endTime, Extensions.CHROMA_EXT);


                List<float[][]> features;
                String fileNameToStore;
                features = new ArrayList<float[][]>();
                for (FeaturesExtractorHTK featuresExtractor : featureExtractorToWorkWith) {
                    float[][] feature = featuresExtractor.extractFeatures(startTime, endTime, refFrequency, Root.C);
                    features.add(feature);
                }

                fileNameToStore = String.format("%s/%s", dirName, filename);
                FeatureVector vector = new FeatureVector(features, chrSamplingPeriod);
                vector.storeDataInHTKFormat(fileNameToStore);
            }
        }

    }









}
