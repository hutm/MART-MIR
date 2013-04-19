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

package org.mart.crs.management.features.extractor.jni;

import org.mart.crs.management.features.FeatureVector;

/**
 * @version 1.0 7/29/12 6:06 PM
 * @author: Hut
 */
public class JNIChromaExtractor {

    static {
        System.loadLibrary("avutil");
        System.loadLibrary("avcodec");
        System.loadLibrary("avformat");
        System.out.println("loaded ffmmeg libs");
        System.loadLibrary("chordsChroma2");
    }


    protected native float[] getPCP(String filePath, float samplingRate, int windowSize, float overlapping);


    public float[][] getFeaturesInMartFormat(String filePath, float samplingRate, int windowSize, float overlapping){

        float[] data =  getPCP(filePath, samplingRate, windowSize, overlapping);
        int numberOfAdditionalFrames = Math.round(0.5f / overlapping);
        int dataSize =  data.length/24;
        float[][] out = new float[dataSize + numberOfAdditionalFrames][24];
        for (int counter = 0; counter < dataSize; counter++) {
            for(int i = 0; i < 12; i++){
                out[numberOfAdditionalFrames + counter][(i+9)%12] = data[counter * 24 + i];
            }
            for(int i = 12; i < 24; i++){
                out[numberOfAdditionalFrames + counter][12 + (i+9)%12] = data[counter * 24 + i];
            }
        }

        return out;
    }


    public float[][] getFeaturesInMartFormatTreble(String filePath, float samplingRate, int windowSize, float overlapping){
        float[] data =  getPCP(filePath, samplingRate, windowSize, overlapping);
        int numberOfAdditionalFrames = Math.round(0.5f / overlapping);
        int dataSize =  data.length/24;
        float[][] out = new float[dataSize + numberOfAdditionalFrames][12];
        for (int counter = 0; counter < dataSize; counter++) {
            for(int i = 12; i < 24; i++){
                out[numberOfAdditionalFrames + counter][(i+9)%12] = data[counter * 24 + i];
            }
        }

        return out;
    }

    public float[][] getFeaturesInMartFormatBass(String filePath, float samplingRate, int windowSize, float overlapping){

        float[] data =  getPCP(filePath, samplingRate, windowSize, overlapping);
        int numberOfAdditionalFrames = Math.round(0.5f / overlapping);
        int dataSize =  data.length/24;
        float[][] out = new float[dataSize + numberOfAdditionalFrames][12];
        for (int counter = 0; counter < dataSize; counter++) {
            for(int i = 0; i < 12; i++){
                out[numberOfAdditionalFrames + counter][(i+9)%12] = data[counter * 24 + i];
            }
        }

        return out;
    }


}
