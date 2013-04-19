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
import org.mart.crs.exec.operation.domain.ChordSmartLatticeDomain;
import org.mart.crs.management.audio.ReferenceFreqManager;
import org.mart.crs.management.beat.BeatStructure;
import org.mart.crs.management.beat.segment.BeatSegment;
import org.mart.crs.management.config.Configuration;
import org.mart.crs.management.features.FeatureVector;
import org.mart.crs.management.features.extractor.FeaturesExtractorHTK;
import org.mart.crs.management.label.LabelsSource;
import org.mart.crs.management.label.chord.ChordSegment;
import org.mart.crs.management.label.chord.ChordStructure;
import org.mart.crs.management.label.chord.ChordType;
import org.mart.crs.management.label.chord.Root;
import org.mart.crs.model.htk.parser.chord.ChordHTKParser;
import org.mart.crs.utils.filefilter.ExtensionFileFilter;
import org.mart.crs.utils.helper.Helper;
import org.mart.crs.utils.helper.HelperArrays;
import org.mart.crs.utils.helper.HelperFile;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mart.crs.management.label.chord.ChordType.isToUseChordWrappersToTrainChordChildren;
import static org.mart.crs.utils.helper.HelperData.*;
import static org.mart.crs.utils.helper.HelperFile.*;

/**
 * @version 1.0 Nov 10, 2009 11:26:44 AM
 * @author: Maksim Khadkevich
 */
public class FeaturesManagerChord extends FeaturesManager {


    public FeaturesManagerChord(String songFilePath, String outDirPath, boolean isForTraining, ExecParams execParams) {
        super(songFilePath, outDirPath, isForTraining, execParams);
    }


    public void extractFeaturesForSong(ReferenceFreqManager referenceFreqManager) {
        float refFrequency = referenceFreqManager.getRefFreqForSong(HelperFile.getShortFileName(songFilePath));

        logger.info(String.format("Processing file %s with reference freq %5.3f", HelperFile.getShortFileName(songFilePath), refFrequency));
        String dirName = outDirPath + File.separator + getNameWithoutExtension(songFilePath);

        if (isForTraining) {
            exctractForTraining(refFrequency, dirName);
        } else {
            extractForTest(refFrequency, dirName);
        }
    }


    public void exctractForTraining(float refFrequency, String dirName) {
        String lblFilePath = chordLabelSource.getFilePathForSong(songFilePath);

        if (lblFilePath == null) {
            logger.warn(String.format("Lablels for file %s were not found in directory %s", songFilePath, Settings.labelsGroundTruthDir));
            return;
        }

        List<ChordSegment> segments = (new ChordStructure(lblFilePath)).getChordSegments();

        for (int i = 0; i < segments.size(); i++) {
            ChordSegment curSegment = segments.get(i);
            if (!(curSegment.getChordType()).equals(ChordType.UNKNOWN_CHORD)) {

                //Skip unnecessary chord segments
                if (isToUseChordWrappersToTrainChordChildren) {
                    //In the current configuration all "wrapper"  chords are used to trained their reduced versions
                    if (!Arrays.asList(Configuration.chordDictionary).contains(curSegment.getChordType().getName())) {
                        continue;
                    }
                } else {
                    //In this case only the chords themselves are used to train models, without wrappers
                    if (!Arrays.asList(ChordType.chordDictionary).contains(curSegment.getChordType())) {
                        continue;
                    }
                }


                double startTime = segments.get(i).getOnset();
                double endTime = segments.get(i).getOffset();
                String filename = String.format("%5.3f_%5.3f_%s%s", startTime, endTime, curSegment.getChordType().getName(), Extensions.CHROMA_EXT);


                List<float[][]> features;
                String fileNameToStore;
                if (!(curSegment.getChordType() == ChordType.NOT_A_CHORD || isToSaveRotatedFeatures())) {
                    features = new ArrayList<float[][]>();
                    for (FeaturesExtractorHTK featuresExtractor : featureExtractorToWorkWith) {
                        float[][] feature = featuresExtractor.extractFeatures(startTime, endTime, refFrequency, curSegment.getRoot());
                        features.add(feature);
                    }

                    fileNameToStore = String.format("%s/%s", dirName, filename);
                    FeatureVector vector = new FeatureVector(features, chrSamplingPeriod);
                    vector.storeDataInHTKFormat(fileNameToStore);
                } else {
                    String chordTypeName = curSegment.getChordType().getName();
                    for (int rotation = 0; rotation < ChordSegment.SEMITONE_NUMBER; rotation++) {
                        features = new ArrayList<float[][]>();
                        int newRootIndex = 0;
                        if (curSegment.getChordType() == ChordType.NOT_A_CHORD) {
                            newRootIndex = rotation;
                        } else {
                            newRootIndex = HelperArrays.transformIntValueToBaseRange(curSegment.getRoot().ordinal() + rotation, Root.values().length);
                        }
                        int newRootLabelIndex = HelperArrays.transformIntValueToBaseRange(-1 * rotation, Root.values().length);
                        for (FeaturesExtractorHTK featuresExtractor : featureExtractorToWorkWith) {
                            float[][] feature = featuresExtractor.extractFeatures(startTime, endTime, refFrequency, Root.values()[newRootIndex]);
                            features.add(feature);
                        }

                        String filePath;
                        if (ChordOperationDomain.isSphinx) {
                            if (curSegment.getChordType() == ChordType.NOT_A_CHORD) {
                                filePath = filename.replaceAll(chordTypeName, String.format("%s%s", Root.values()[newRootLabelIndex].getName(), chordTypeName));
                            } else {
                                filePath = filename.replaceAll(chordTypeName, String.format("%s%s", Root.values()[newRootLabelIndex].getName(), chordTypeName));
                            }
                        } else {
                            if (curSegment.getChordType() == ChordType.NOT_A_CHORD) {
                                filePath = filename.replaceAll(chordTypeName, String.format("%s%s", Root.values()[newRootLabelIndex].getName(), chordTypeName));
                            } else {
                                filePath = filename.replaceAll(chordTypeName, String.format("%s%s", chordTypeName, Root.values()[newRootLabelIndex].getName()));
                            }
                        }

                        fileNameToStore = String.format("%s/%s", dirName, filePath);
                        FeatureVector vector = new FeatureVector(features, chrSamplingPeriod);
                        vector.storeDataInHTKFormat(fileNameToStore);
                    }
                }
            }
        }
    }

    //TODO
    public static void exctractForTraining(List<FeaturesExtractorHTK> featureExtractorToWorkWith, LabelsSource chordLabelSource, String songFilePath, int chrSamplingPeriod, boolean isToSaveRotatedFeatures, float refFrequency, String dirName) {
        String lblFilePath = chordLabelSource.getFilePathForSong(songFilePath);

        if (lblFilePath == null) {
            logger.warn(String.format("Lablels for file %s were not found in directory %s", songFilePath, Settings.labelsGroundTruthDir));
            return;
        }

        List<ChordSegment> segments = (new ChordStructure(lblFilePath)).getChordSegments();

        for (int i = 0; i < segments.size(); i++) {
            ChordSegment curSegment = segments.get(i);
            if (!(curSegment.getChordType()).equals(ChordType.UNKNOWN_CHORD)) {

                //Skip unnecessary chord segments
                if (isToUseChordWrappersToTrainChordChildren) {
                    //In the current configuration all "wrapper"  chords are used to trained their reduced versions
                    if (!Arrays.asList(Configuration.chordDictionary).contains(curSegment.getChordType().getName())) {
                        continue;
                    }
                } else {
                    //In this case only the chords themselves are used to train models, without wrappers
                    if (!Arrays.asList(ChordType.chordDictionary).contains(curSegment.getChordType())) {
                        continue;
                    }
                }


                double startTime = segments.get(i).getOnset();
                double endTime = segments.get(i).getOffset();
                String filename = String.format("%5.3f_%5.3f_%s%s", startTime, endTime, curSegment.getChordType().getName(), Extensions.CHROMA_EXT);


                List<float[][]> features;
                String fileNameToStore;
                if (!(curSegment.getChordType() == ChordType.NOT_A_CHORD || isToSaveRotatedFeatures)) {
                    features = new ArrayList<float[][]>();
                    for (FeaturesExtractorHTK featuresExtractor : featureExtractorToWorkWith) {
                        float[][] feature = featuresExtractor.extractFeatures(startTime, endTime, refFrequency, curSegment.getRoot());
                        features.add(feature);
                    }

                    fileNameToStore = String.format("%s/%s", dirName, filename);
                    FeatureVector vector = new FeatureVector(features, chrSamplingPeriod);
                    vector.storeDataInHTKFormat(fileNameToStore);
                } else {
                    String chordTypeName = curSegment.getChordType().getName();
                    for (int rotation = 0; rotation < ChordSegment.SEMITONE_NUMBER; rotation++) {
                        features = new ArrayList<float[][]>();
                        int newRootIndex = 0;
                        if (curSegment.getChordType() == ChordType.NOT_A_CHORD) {
                            newRootIndex = rotation;
                        } else {
                            newRootIndex = HelperArrays.transformIntValueToBaseRange(curSegment.getRoot().ordinal() + rotation, Root.values().length);
                        }
                        int newRootLabelIndex = HelperArrays.transformIntValueToBaseRange(-1 * rotation, Root.values().length);
                        for (FeaturesExtractorHTK featuresExtractor : featureExtractorToWorkWith) {
                            float[][] feature = featuresExtractor.extractFeatures(startTime, endTime, refFrequency, Root.values()[newRootIndex]);
                            features.add(feature);
                        }

                        String filePath;
                        if (ChordOperationDomain.isSphinx) {
                            if (curSegment.getChordType() == ChordType.NOT_A_CHORD) {
                                filePath = filename.replaceAll(chordTypeName, String.format("%s%s", Root.values()[newRootLabelIndex].getName(), chordTypeName));
                            } else {
                                filePath = filename.replaceAll(chordTypeName, String.format("%s%s", Root.values()[newRootLabelIndex].getName(), chordTypeName));
                            }
                        } else {
                            if (curSegment.getChordType() == ChordType.NOT_A_CHORD) {
                                filePath = filename.replaceAll(chordTypeName, String.format("%s%s", Root.values()[newRootLabelIndex].getName(), chordTypeName));
                            } else {
                                filePath = filename.replaceAll(chordTypeName, String.format("%s%s", chordTypeName, Root.values()[newRootLabelIndex].getName()));
                            }
                        }

                        fileNameToStore = String.format("%s/%s", dirName, filePath);
                        FeatureVector vector = new FeatureVector(features, chrSamplingPeriod);
                        vector.storeDataInHTKFormat(fileNameToStore);
                    }
                }
            }
        }
    }


    /**
     * If necessary, perform circular roration and save
     */
    protected boolean isToSaveRotatedFeatures() {
        return false;
    }


    public void extractForTest(float refFrequency, String dirName) {
        FeatureVector featureVector = extractFeatureVectorForTest(refFrequency);

        String filenameToSave = new StringBuilder().append(dirName).append(File.separator).append(featureVector.getFileNameToStoreTestVersion()).toString();
        featureVector.storeDataInHTKFormat(filenameToSave);
    }


    public FeatureVector extractFeatureVectorForTest(float refFrequency) {
        List<float[][]> features = new ArrayList<float[][]>();
        for (FeaturesExtractorHTK featuresExtractor : featureExtractorToWorkWith) {
            float[][] feature = featuresExtractor.extractFeatures(refFrequency, Root.C);
            features.add(feature);
        }

        FeatureVector outFeatureVector = new FeatureVector(features, chrSamplingPeriod);
        outFeatureVector.setDuration(this.songDuration);
        return outFeatureVector;
    }


    //TODO: move to tests
    /* public static void main(String[] args) {
            readFeatureVector("/home/hut/work/test_keys/_key3/fold0/2-trainFeat/1050233778/data/01_-_A_Hard_Day's_Night/1.900_148.250_maj.chr");
        }
    */
}

