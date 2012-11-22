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
import org.mart.crs.model.htk.parser.chord.ChordHTKParser;
import org.mart.crs.utils.helper.HelperArrays;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import static org.mart.crs.management.label.chord.ChordType.isToUseChordWrappersToTrainChordChildren;
import static org.mart.crs.model.htk.parser.chord.ChordHTKParser.FEATURE_SAMPLE_RATE;
import static org.mart.crs.model.htk.parser.chord.ChordHTKParser.FEATURE_SAMPLE_RATE_BEAT_SYNCHRONOUS_COEFF;

/**
 * @version 1.0 4/4/12 12:33 AM
 * @author: Hut
 */
public class FeaturesManagerChordPerBeat extends FeaturesManagerChord {


    public FeaturesManagerChordPerBeat(String songFilePath, String outDirPath, boolean isForTraining, ExecParams execParams) {
        super(songFilePath, outDirPath, isForTraining, execParams);
    }


    @Override
    public FeatureVector extractFeatureVectorForTest(float refFrequency) {

        String beatFilePath = beatLabelsSource.getFilePathForSong(songFilePath);
        BeatStructure beatStructure = BeatStructure.getBeatStructure(beatFilePath);
        beatStructure.fixBeatStructure(songDuration);

        List<float[][]> features = new ArrayList<float[][]>();
        for (FeaturesExtractorHTK featuresExtractor : featureExtractorToWorkWith) {
            List<float[]> outFeatures = new ArrayList<float[]>();
            for (int i = 0; i < beatStructure.getBeats().length; i++) {
                BeatSegment beatSegment = beatStructure.getBeatSegments().get(i);
                if(beatSegment.getTimeInstant() == beatSegment.getNextBeatTimeInstant()){
                    continue;
                }

                float[][] feature = featuresExtractor.extractFeatures(beatSegment.getTimeInstant(), beatSegment.getNextBeatTimeInstant(), refFrequency, Root.C);
                float[] averageValue = HelperArrays.average(feature, 0, feature.length);
                averageValue = HelperArrays.normalizeVector(averageValue);
                if(averageValue == null || averageValue.length == 0){  //If beat segment is too short returned vector is of zero size
                    averageValue = new float[featuresExtractor.getVectorSize()];
                }
                outFeatures.add(averageValue);
            }
            float[][] out = new float[outFeatures.size()][];
            for (int i = 0; i < out.length; i++) {
                out[i] = outFeatures.get(i);
            }
            features.add(out);
        }

        FeatureVector outFeatureVector = new FeatureVector(features,  (int) (FEATURE_SAMPLE_RATE / FEATURE_SAMPLE_RATE_BEAT_SYNCHRONOUS_COEFF));
        outFeatureVector.setDuration(this.songDuration);
        return outFeatureVector;
    }


    @Override
    public void exctractForTraining(float refFrequency, String dirName) {

        String chordFilePath = chordLabelSource.getFilePathForSong(songFilePath);
        String beatFilePath = beatLabelsSource.getFilePathForSong(songFilePath);

        ChordStructure chordStructure = new ChordStructure(chordFilePath);
        BeatStructure beatStructure = BeatStructure.getBeatStructure(beatFilePath);
        beatStructure.fixBeatStructure(songDuration);

        if (chordFilePath == null || beatFilePath == null) {
            logger.warn(String.format("Could not find labels for song %s", songFilePath));
            return;
        }

        FeatureVector globalFeatureVector = extractFeatureVectorForTest(refFrequency);

        ChordSegment severalFramesSegment = null;

        int startBeatIndex = 0;
        int endbeatIndex = 0;

        for (int i = 0; i <beatStructure.getBeatSegments().size(); i++) {
            BeatSegment beatSegment = beatStructure.getBeatSegments().get(i);
            if (beatSegment.getTimeInstant() == beatSegment.getNextBeatTimeInstant()) {
                continue;
            }

            ChordSegment curSegment = getNextChordSegment(beatSegment, chordStructure);
            if(severalFramesSegment == null){
                severalFramesSegment = curSegment;
                startBeatIndex = i;
                endbeatIndex = i+1;
                continue;
            } else{
                if(curSegment.getChordName().equals(severalFramesSegment.getChordName())){
                    severalFramesSegment.setOffset(curSegment.getOffset());
                    endbeatIndex++;
                    continue;
                }
            }


            // Now copypast from FeaturesManagerChord
            if (!(severalFramesSegment.getChordType()).equals(ChordType.UNKNOWN_CHORD)) {

                //Skip unnecessary chord segments
                if (isToUseChordWrappersToTrainChordChildren) {
                    //In the current configuration all "wrapper"  chords are used to trained their reduced versions
                    if (!Arrays.asList(Configuration.chordDictionary).contains(severalFramesSegment.getChordType().getName())) {
                        continue;
                    }
                } else {
                    //In this case only the chords themselves are used to train models, without wrappers
                    if (!Arrays.asList(ChordType.chordDictionary).contains(severalFramesSegment.getChordType())) {
                        continue;
                    }
                }

                double startTime = severalFramesSegment.getOnset();
                double endTime = severalFramesSegment.getOffset();
                String filename = String.format("%5.3f_%5.3f_%s%s", startTime, endTime, severalFramesSegment.getChordType().getName(), Extensions.CHROMA_EXT);


                List<float[][]> features = globalFeatureVector.getVectors();
                String fileNameToStore;
                if (!(curSegment.getChordType() == ChordType.NOT_A_CHORD || isToSaveRotatedFeatures())) {

                    List<float[][]> segmentFeatures = new ArrayList<float[][]>();
                    for(float[][] feature:features){
                        float[][] segmentFloat = new float[endbeatIndex - startBeatIndex][];
                        System.arraycopy(feature, startBeatIndex, segmentFloat, 0, endbeatIndex - startBeatIndex);
                        segmentFloat = FeaturesExtractorHTK.rotateFeaturesStatic(segmentFloat,  severalFramesSegment.getRoot());
                        segmentFeatures.add(segmentFloat);
                    }

                    fileNameToStore = String.format("%s/%s", dirName, filename);
                    FeatureVector vector = new FeatureVector(segmentFeatures, (int) (FEATURE_SAMPLE_RATE / FEATURE_SAMPLE_RATE_BEAT_SYNCHRONOUS_COEFF));
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

                        List<float[][]> segmentFeatures = new ArrayList<float[][]>();
                        for(float[][] feature:features){
                            float[][] segmentFloat = new float[endbeatIndex - startBeatIndex][];
                            System.arraycopy(segmentFloat, 0, feature, startBeatIndex, endbeatIndex - startBeatIndex);
                            segmentFloat = FeaturesExtractorHTK.rotateFeaturesStatic(segmentFloat,  Root.values()[newRootIndex]);
                            segmentFeatures.add(segmentFloat);
                        }

                        String filePath;

                        if (curSegment.getChordType() == ChordType.NOT_A_CHORD) {
                            filePath = filename.replaceAll(chordTypeName, String.format("%s%s", Root.values()[newRootLabelIndex].getName(), chordTypeName));
                        } else {
                            filePath = filename.replaceAll(chordTypeName, String.format("%s%s", chordTypeName, Root.values()[newRootLabelIndex].getName()));
                        }

                        fileNameToStore = String.format("%s/%s", dirName, filePath);
                        FeatureVector vector = new FeatureVector(segmentFeatures, (int) (FEATURE_SAMPLE_RATE / FEATURE_SAMPLE_RATE_BEAT_SYNCHRONOUS_COEFF));
                        vector.storeDataInHTKFormat(fileNameToStore);
                    }
                }
            }

            severalFramesSegment = curSegment;
            startBeatIndex = endbeatIndex;
            endbeatIndex++;
        }





    }

    protected ChordSegment getNextChordSegment(BeatSegment beatSegment, ChordStructure chordStructure){

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
            return tempSegmentWithBeatDuration;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ChordSegment(beatSegment.getTimeInstant(), beatSegment.getNextBeatTimeInstant(), curSegment.getChordName());
    }

}
