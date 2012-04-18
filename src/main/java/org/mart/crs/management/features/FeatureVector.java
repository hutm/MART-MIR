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

package org.mart.crs.management.features;

import org.apache.log4j.Logger;
import org.mart.crs.logging.CRSLogger;
import org.mart.crs.utils.helper.Helper;
import org.mart.crs.utils.helper.HelperArrays;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static org.mart.crs.config.Extensions.CHROMA_EXT;
import static org.mart.crs.utils.helper.HelperData.*;

/**
 * This class represents feature vectors data structure
 */
public class FeatureVector implements Serializable {

    protected static Logger logger = CRSLogger.getLogger(FeatureVector.class);


    private List<float[][]> vectors;

    private float duration;

    private int samplePeriod;

    private String additionalInfo;

    public FeatureVector(float[][] vectors, int samplePeriod) {
        this.vectors = new ArrayList<float[][]>();
        this.vectors.add(vectors);
        this.samplePeriod = samplePeriod;
    }

    public FeatureVector(List<float[][]> vectors, int samplePeriod) {
        this.vectors = vectors;
        this.samplePeriod = samplePeriod;
    }


    public void storeDataInHTKFormat(String fileNameToStore) {
        storeDataInHTKFormatStatic(fileNameToStore, this);
    }

    /**
     * Stores Data in HTK originalFormat
     *
     * @param fileNameToStore filename
     * @param featureVector   FeatureVector data structure to store
     */
    public static void storeDataInHTKFormatStatic(String fileNameToStore, FeatureVector featureVector) {
        List<float[][]> vectors;

        vectors = featureVector.getVectors();

        if (vectors.size() == 0 || vectors.get(0).length == 0 || vectors.get(0)[0].length == 0) {
            return;
        }

        try {
            FileOutputStream outStream_ = new FileOutputStream(fileNameToStore);
            BufferedOutputStream outStream = new BufferedOutputStream(outStream_);

            int vectorSize = 0;
            for (float[][] vector : vectors) {
                vectorSize += vector[0].length;
            }

            //First write HTK Header
            //
            //int nSamples;
            //int sampPeriod;
            //short sampSize;
            //short parmKind;
            writeInt(vectors.get(0).length, outStream);
            writeInt(featureVector.getSamplePeriod(), outStream);
            writeShort((short) (vectorSize * Float.SIZE / 8), outStream);
            writeShort((short) 9, outStream);

            //Write Pcp data
            for (int i = 0; i < vectors.get(0).length; i++) {
                for (float[][] vector : vectors) {
                    for (int j = 0; j < vector[0].length; j++) {
                        writeFloat(vector[i][j], outStream);
                    }
                }
            }

            outStream.close();
        } catch (FileNotFoundException e) {
            logger.error("Cannot open stream to write data to file " + fileNameToStore);
            logger.error(Helper.getStackTrace(e));
        } catch (IOException e) {
            logger.error("Some strange error");
            logger.error(Helper.getStackTrace(e));
        }
    }


    /**
     * Reads chroma vectors from file
     *
     * @param fileName
     * @return
     */
    public static FeatureVector readFeatureVector(String fileName) {
        float[][] pcp;
        int nSamples, samplingPeriod;
        short sampSize, paramKind;
        try {
            FileInputStream inputStream = new FileInputStream(fileName);
            BufferedInputStream in = new BufferedInputStream(inputStream);


            nSamples = readInt(in);
            samplingPeriod = readInt(in);
            sampSize = readShort(in);
            paramKind = readShort(in);
            pcp = new float[nSamples][(sampSize * 8 / Float.SIZE)];
            for (int i = 0; i < nSamples; i++) {
                for (int j = 0; j < (sampSize * 8 / Float.SIZE); j++) {
                    pcp[i][j] = readFloat(in);
                }
            }

            FeatureVector outVector = new FeatureVector(pcp, samplingPeriod);
            return outVector;

        } catch (Exception e) {
            logger.error(Helper.getStackTrace(e));
            return null;
        }


    }



    public List<float[][]> getVectors() {
        return vectors;
    }

    public List<float[][]> getNormalizedVectors() {
        List<float[][]> outList = new ArrayList<float[][]>();
        float[][] out = new float[0][];
        for (float[][] aVector:vectors) {
            out = new float[aVector.length][];
            for (int i = 0; i < aVector.length; i++) {
                out[i] = HelperArrays.normalizeVector(aVector[i]);
            }
            outList.add(out);
        }
        return outList;
    }

    public void setVectors(List<float[][]> vectors) {
        this.vectors = vectors;
    }

    public int getSamplePeriod() {
        return samplePeriod;
    }

    public void setSamplePeriod(int samplePeriod) {
        this.samplePeriod = samplePeriod;
    }

    public float getDuration() {
        return duration;
    }

    public void setDuration(float duration) {
        this.duration = duration;
    }

    public String getFileNameToStoreTestVersion(){
        String filenameToSave = new StringBuilder().append("0.00_").append(getDuration()).append("_").append(CHROMA_EXT).toString();
        return filenameToSave;
    }


    public String getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(String additionalInfo) {
        this.additionalInfo = additionalInfo;
    }
}
