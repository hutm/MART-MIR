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

package org.mart.crs.management.features.extractor.chroma;

import org.mart.crs.config.Extensions;
import org.mart.crs.config.Settings;
import org.mart.crs.core.AudioReader;
import org.mart.crs.core.pcp.PCP;
import org.mart.crs.management.config.Configuration;
import org.mart.crs.management.features.extractor.FeaturesExtractorHTK;
import org.mart.crs.management.features.manager.FeaturesManagerChord;
import org.mart.crs.management.label.LabelsSource;
import org.mart.crs.management.label.chord.ChordSegment;
import org.mart.crs.management.label.chord.ChordStructure;
import org.mart.crs.management.label.chord.ChordType;
import org.mart.crs.management.label.chord.Root;

import java.util.Arrays;
import java.util.Random;

/**
 * PCP spectrum based feature
 *
 * @version 1.0 Mar 3, 2010 2:05:45 PM
 * @author: Maksim Khadkevich
 */
public abstract class SpectrumBased extends FeaturesExtractorHTK {

    protected static float std = 0.4f;
    protected static float mean0 = 0.2f;
    protected static float mean1 = 0.8f;
    protected static float mean2 = 2.8f;
    protected static float mean3 = 1.8f;



    public void extractGlobalFeatures(double refFrequency) {
        float[] samples = audioReader.getSamples();
        globalVectors.add(producePCP(samples, refFrequency).getPCP());

//        int length = FeaturesManagerChord.getIndexForTimeInstant(audioReader.getDuration(), execParams);
//        globalVectors.add(new float[length][12]);
//
//        //Now replace chroma with artificial data
//        LabelsSource chordLabelSource = new LabelsSource(Settings.chordLabelsGroundTruthDir, true, "chordGT", Extensions.LABEL_EXT);
//        ChordStructure chordStructure = new ChordStructure(chordLabelSource.getFilePathForSong(audioReader.getFilePath()));
//        for(ChordSegment cs:chordStructure.getChordSegments()){
//            int startIndex = FeaturesManagerChord.getIndexForTimeInstant(cs.getOnset(), execParams);
//            int endIndex = FeaturesManagerChord.getIndexForTimeInstant(cs.getOffset(), execParams);
//            float[][] segmentData = new float[endIndex - startIndex][12];
//            for(int i = 0; i < endIndex - startIndex; i++){
//                segmentData[i] = getMonteCarloPCP(cs.getChordType());
//            }
//            Root rootTo = cs.getRoot() == null ? Root.C : cs.getRoot();
//            segmentData = PCP.rotatePCP(segmentData, 1, Root.C, rootTo);
//
//            float[][] globalVector = globalVectors.get(0);
//            for(int i = startIndex; i < endIndex; i++){
//                if (i < globalVector.length) {
//                    globalVector[i] = segmentData[i - startIndex];
//                }
//            }
//        }

    }


//    protected float[] getMonteCarloPCP(ChordType chordType){
//        int[] noteStrings = chordType.getNotes();
//        Arrays.sort(noteStrings);
//        float[] out = new float[12];
//        Random random = new Random();
//        int intrandom = random.nextInt(3);
//        float mean = intrandom == 0 ? mean1 : intrandom == 1 ? mean2 : mean3;
//        for(int i = 0; i < 12; i++){
//            if(Arrays.binarySearch(noteStrings, i) >= 0){
//                out[i] = (float)(mean + std * random.nextGaussian());
//            } else{
//                out[i] = Math.max(0, (float)(mean0 + std * random.nextGaussian()));
//            }
//        }
//        return out;
//    }





    @Override
    public int getVectorSize() {
        return Configuration.NUMBER_OF_SEMITONES_IN_OCTAVE;
    }


    @Override
    public void initialize(String songFilePath) {
        super.initialize(songFilePath);
        this.audioReader = new AudioReader(songFilePath, execParams.samplingRate);
    }


    public void initialize(AudioReader audioReader) {
        super.initialize(audioReader);
    }


    protected abstract PCP producePCP(float[] samples, double refFrequency);


    @Override
    public float getDuration() {
        return this.audioReader.getDuration();
    }
}
