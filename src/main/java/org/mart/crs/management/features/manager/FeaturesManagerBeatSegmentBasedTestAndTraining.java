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

import org.mart.crs.config.ExecParams;
import org.mart.crs.config.Extensions;
import org.mart.crs.management.beat.BeatStructure;
import org.mart.crs.management.beat.segment.BeatSegment;
import org.mart.crs.management.config.Configuration;
import org.mart.crs.management.features.FeatureVector;
import org.mart.crs.management.features.extractor.FeaturesExtractorHTK;
import org.mart.crs.management.label.chord.ChordSegment;
import org.mart.crs.management.label.chord.ChordStructure;
import org.mart.crs.management.label.chord.ChordType;
import org.mart.crs.management.label.chord.Root;
import org.mart.crs.utils.helper.HelperArrays;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

/**
 * @version 1.0 4/16/12 2:56 PM
 * @author: Hut
 */
public class FeaturesManagerBeatSegmentBasedTestAndTraining extends FeaturesManagerBeatSegmentBasedTest {


    public FeaturesManagerBeatSegmentBasedTestAndTraining(String songFilePath, String outDirPath, boolean isForTraining, ExecParams execParams) {
        super(songFilePath, outDirPath, isForTraining, execParams);
    }


    @Override
    public void exctractForTraining(float refFrequency, String dirName) {

        String chordFilePath = chordLabelSource.getFilePathForSong(songFilePath);
        String beatFilePath = beatLabelsSource.getFilePathForSong(songFilePath);

        ChordStructure chordStructure = new ChordStructure(chordFilePath);
        BeatStructure beatStructure = BeatStructure.getBeatStructure(beatFilePath);

        beatStructure.addTrailingBeats(songDuration);

        if (chordFilePath == null || beatFilePath == null) {
            logger.warn(String.format("Could not find labels for song %s", songFilePath));
            return;
        }

        for (BeatSegment beatSegment : beatStructure.getBeatSegments()) {
            if (beatSegment.getTimeInstant() == beatSegment.getNextBeatTimeInstant()) {
                continue;
            }
            ChordSegment tempSegmentWithBeatDuration = new ChordSegment(beatSegment.getTimeInstant(), beatSegment.getNextBeatTimeInstant(), ChordType.NOT_A_CHORD.getName());
            TreeMap<Float, ChordSegment> intersectionchords = new TreeMap<Float, ChordSegment>();
            for (ChordSegment chordSegment : chordStructure.getChordSegments()) {
                if (chordSegment.intersects(tempSegmentWithBeatDuration)) {
                    intersectionchords.put(chordSegment.getIntersection(tempSegmentWithBeatDuration), chordSegment);
                }
            }
            ChordSegment curSegment = null;
            try {
                curSegment = intersectionchords.lastEntry().getValue();
            } catch (Exception e) {
                e.printStackTrace();
            }


            // Now copypast from FeaturesManagerChord
            if (!(curSegment.getChordType()).equals(ChordType.UNKNOWN_CHORD)) {


                //Skip unnecessary chord segments
                if (ChordType.isToUseChordWrappersToTrainChordChildren) {
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


                double startTime = beatSegment.getTimeInstant();
                double endTime = beatSegment.getNextBeatTimeInstant();
                String filename = String.format("%5.3f_%5.3f_%s%s", startTime, endTime, curSegment.getChordType().getName(), Extensions.CHROMA_EXT);


                List<float[][]> features = new ArrayList<float[][]>();
                String fileNameToStore;
                if (!(curSegment.getChordType() == ChordType.NOT_A_CHORD || isToSaveRotatedFeatures())) {
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

                        if (curSegment.getChordType() == ChordType.NOT_A_CHORD) {
                            filePath = filename.replaceAll(chordTypeName, String.format("%s%s", Root.values()[newRootLabelIndex].getName(), chordTypeName));
                        } else {
                            filePath = filename.replaceAll(chordTypeName, String.format("%s%s", chordTypeName, Root.values()[newRootLabelIndex].getName()));
                        }

                        fileNameToStore = String.format("%s/%s", dirName, filePath);
                        FeatureVector vector = new FeatureVector(features, chrSamplingPeriod);
                        vector.storeDataInHTKFormat(fileNameToStore);
                    }
                }
            }
        }
    }
}
