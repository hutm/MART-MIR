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
import org.mart.crs.exec.operation.domain.ChordSmartLatticeDomain;
import org.mart.crs.management.beat.BeatStructure;
import org.mart.crs.management.beat.segment.BeatSegment;
import org.mart.crs.management.features.FeatureVector;
import org.mart.crs.management.features.extractor.FeaturesExtractorHTK;
import org.mart.crs.management.label.chord.ChordSegment;
import org.mart.crs.management.label.chord.ChordStructure;
import org.mart.crs.management.label.chord.Root;
import org.mart.crs.model.htk.parser.chord.ChordHTKParser;
import org.mart.crs.utils.filefilter.ExtensionFileFilter;
import org.mart.crs.utils.helper.HelperFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.mart.crs.utils.helper.HelperFile.getFile;
import static org.mart.crs.utils.helper.HelperFile.getPathForFileWithTheSameName;

/**
 * @version 1.0 4/11/12 4:52 PM
 * @author: Hut
 */
public class FeaturesManagerSmartLattice extends FeaturesManagerChord {

    public FeaturesManagerSmartLattice(String songFilePath, String outDirPath, boolean isForTraining, ExecParams execParams) {
        super(songFilePath, outDirPath, isForTraining, execParams);
    }

    @Override
    public void extractForTest(float refFrequency, String dirName) {
        String beatLabelFilePath;
        beatLabelFilePath = HelperFile.getPathForFileWithTheSameName(songFilePath, Settings.beatLabelsGroundTruthDir, Extensions.BEAT_EXT);

        if (beatLabelFilePath == null) {
            logger.warn(String.format("Lablels for file %s were not found in directory %s", songFilePath, Settings.labelsGroundTruthDir));
            return;
        }

        BeatStructure beatStructure = BeatStructure.getBeatStructure(beatLabelFilePath);
        List<BeatSegment> segments;
        segments = beatStructure.getBeatSegments();

        double startTime, endTime;
        int onsetBeatNumber, offsetBeatNumber;

        for (int i = 0; i < segments.size(); i++) {
            onsetBeatNumber = i;
            startTime = segments.get(i).getTimeInstant();
            for (int length = 1; length <= ChordSmartLatticeDomain.maxSegmentInBeats; length++) {

                offsetBeatNumber = onsetBeatNumber + length;
                if (offsetBeatNumber >= segments.size()) {
                    //Not possible to extract longer segments
                    continue;
                }

                endTime = segments.get(offsetBeatNumber).getTimeInstant();

                String filename = String.format("%d_%d_%s", onsetBeatNumber, offsetBeatNumber, Extensions.CHROMA_EXT);


                List<float[][]> features = new ArrayList<float[][]>();
                for (FeaturesExtractorHTK featuresExtractor : featureExtractorToWorkWith) {
                    float[][] feature = featuresExtractor.extractFeatures(startTime, endTime, refFrequency, Root.C);
                    features.add(feature);
                }

                String fileNameToStore = String.format("%s/%s", dirName, filename);
                FeatureVector vector = new FeatureVector(features, chrSamplingPeriod);
                vector.storeDataInHTKFormat(fileNameToStore);
            }
        }
    }


    /**
     * This functions splits the feature vector stream into parts that contain only one chord segment. This is done
     * after first-pass ViterbiPath decoding
     *
     * @param featureVectorFilePath File path to the feature vector data
     * @param chordSegments         parsed data about chord segments' boundaries
     * @param outFolderPath         folder to save splited segments
     * @return number of parts
     */
    public static int splitFeatureVectorsInSegments(String featureVectorFilePath, List<ChordSegment> chordSegments, String outFolderPath, String songName) {
        FeatureVector featureVector = FeatureVector.readFeatureVector(featureVectorFilePath);
        int samplingPeriod = featureVector.getSamplePeriod();
        float[][] pcp = featureVector.getVectors().get(0);

        File outFolder = getFile(outFolderPath);
        outFolder.mkdirs();

        //now segment and store
        String outFilePath;
        float[][] segmentData;

        int counter = 0; //segment counter
        for (ChordSegment chordSegment : chordSegments) {
            int startIndex = (int) Math.round(chordSegment.getOnset() / samplingPeriod * ChordHTKParser.FEATURE_SAMPLE_RATE);
            int endIndex = (int) Math.round(chordSegment.getOffset() / samplingPeriod * ChordHTKParser.FEATURE_SAMPLE_RATE);
            if (endIndex >= pcp.length) {
                endIndex = pcp.length - 1;
            }

            segmentData = new float[endIndex - startIndex][];
            for (int i = startIndex; i < endIndex; i++) {
                segmentData[i - startIndex] = pcp[i];
            }

            outFilePath = outFolder.getAbsolutePath() + File.separator + counter++ + songName + Extensions.CHROMA_SEC_PASS_EXT;
            FeatureVector vector = new FeatureVector(segmentData, samplingPeriod);
            vector.storeDataInHTKFormat(outFilePath);
        }

        return counter;
    }


    public static int splitFeatureVectorsInSegments(String featureVectorFilePath, List<ChordSegment> chordSegments, String outFolderPath) {
        return splitFeatureVectorsInSegments(featureVectorFilePath, chordSegments, outFolderPath, "");
    }


    /**
     * Splits all data in the given folder
     *
     * @param featuresFolderPath   featuresFolderPath
     * @param recognizedLabelsPath recognizedLabelsPath
     */
    public static void splitAllData(String featuresFolderPath, String recognizedLabelsPath) {
        File featuresFolderDir = getFile(featuresFolderPath);
        File featureVectorFile;
        List<ChordSegment> chordLabels;
        String outDir;
        for (File songNameDir : featuresFolderDir.listFiles()) {
            if (songNameDir.isDirectory()) {
                File[] chromaFiles = songNameDir.listFiles(new ExtensionFileFilter(new String[]{Extensions.CHROMA_EXT}, false));
                if (chromaFiles.length > 1) {
                    logger.error("More than 1 feature vector data in directory " + songNameDir.getPath());
                } else {
                    if (chromaFiles.length == 0) {
                        continue; //There is no chroma output
                    }
                    logger.info("Spliting feature vectors for song " + songNameDir.getName());
                    featureVectorFile = chromaFiles[0];
                    chordLabels = (new ChordStructure(getPathForFileWithTheSameName(songNameDir.getName() + Extensions.WAV_EXT, recognizedLabelsPath, Extensions.LABEL_EXT))).getChordSegments();
                    outDir = songNameDir + File.separator + "out";
                    splitFeatureVectorsInSegments(featureVectorFile.getPath(), chordLabels, outDir);
                }
            }
        }
    }


}
