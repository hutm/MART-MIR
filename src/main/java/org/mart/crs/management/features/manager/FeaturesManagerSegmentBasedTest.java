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
import org.mart.crs.exec.operation.domain.ChordOperationDomain;
import org.mart.crs.management.features.FeatureVector;
import org.mart.crs.management.features.extractor.FeaturesExtractorHTK;
import org.mart.crs.management.label.chord.ChordSegment;
import org.mart.crs.management.label.chord.ChordStructure;
import org.mart.crs.management.label.chord.ChordType;
import org.mart.crs.management.label.chord.Root;
import org.mart.crs.utils.helper.HelperArrays;

import java.util.ArrayList;
import java.util.List;

/**
 * @version 1.0 4/11/12 5:51 PM
 * @author: Hut
 */
public class FeaturesManagerSegmentBasedTest extends FeaturesManagerChord {


    public FeaturesManagerSegmentBasedTest(String songFilePath, String outDirPath, boolean isForTraining, ExecParams execParams) {
        super(songFilePath, outDirPath, isForTraining, execParams);
    }


    @Override
    public void extractForTest(float refFrequency, String dirName) {
        String lblFilePath = chordLabelSource.getFilePathForSong(songFilePath);

        if (lblFilePath == null) {
            logger.warn(String.format("Lablels for file %s were not found in directory %s", songFilePath, Settings.labelsGroundTruthDir));
            return;
        }

        List<ChordSegment> segments = (new ChordStructure(lblFilePath)).getChordSegments();

        for (int i = 0; i < segments.size(); i++) {
            ChordSegment curSegment = segments.get(i);

            double startTime = segments.get(i).getOnset();
            double endTime = segments.get(i).getOffset();
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
