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

package org.mart.crs.model.htk.parser.chord;

import org.apache.log4j.Logger;
import org.mart.crs.config.Extensions;
import org.mart.crs.config.Settings;
import org.mart.crs.logging.CRSLogger;
import org.mart.crs.management.beat.BeatStructure;
import org.mart.crs.management.beat.segment.BeatSegment;
import org.mart.crs.management.features.FeaturesManager;
import org.mart.crs.management.label.LabelsSource;
import org.mart.crs.management.label.chord.ChordSegment;
import org.mart.crs.management.label.chord.ChordStructure;
import org.mart.crs.utils.helper.Helper;
import org.mart.crs.utils.helper.HelperFile;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import static org.mart.crs.utils.helper.HelperFile.getFile;

/**
 * @version 1.0 3/28/11 6:46 PM
 * @author: Hut
 */
public class ChordHTKParser {

    protected static Logger logger = CRSLogger.getLogger(ChordHTKParser.class);

    public static float FEATURE_SAMPLE_RATE = 10000000; //in units of 10ns
    public static final float PRECISION_COEFF_LATTICE = FEATURE_SAMPLE_RATE / 10000000;


    protected String htkOutFilePath;

    protected String parsedLabelsDir;

    protected ArrayList<ChordStructure> results;


    public ChordHTKParser(String htkOutFilePath, String parsedLabelsDir) {
        this.htkOutFilePath = htkOutFilePath;
        this.parsedLabelsDir = parsedLabelsDir;
        this.results = new ArrayList<ChordStructure>();
    }

    public void run() {
        parseResults();
        storeResults();
    }


    public void parseResults() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(htkOutFilePath));

            String line = reader.readLine();
            if (!line.equals("#!MLF!#")) {
                logger.error("File " + htkOutFilePath + " does not seem to be label file");
                throw new IOException();
            }
            while ((line = reader.readLine()) != null && line.length() > 0) {

                String[] tokensFilePath = line.split("/");
                String songFileName = tokensFilePath[tokensFilePath.length - 2];

                parseSongTranscription(reader, songFileName);
            }
            reader.close();
        } catch (Exception e) {
            logger.error("Unexpexted Error occured ");
            logger.error(Helper.getStackTrace(e));
        }

        Collections.sort(results);
        for (ChordStructure aResult : results) {
            Collections.sort(aResult.getChordSegments());
        }
    }

    protected void parseSongTranscription(BufferedReader reader, String songFileName) throws IOException {
        //Now read recognized chords
        String line;
        List<ChordSegment> chordSegments = new ArrayList<ChordSegment>();
        while ((line = reader.readLine()) != null && !line.equals(".")) {
            StringTokenizer tokenizer = new StringTokenizer(line);
            float startTime = Float.parseFloat(tokenizer.nextToken()) / ChordHTKParser.FEATURE_SAMPLE_RATE;
            float endTime = Float.parseFloat(tokenizer.nextToken()) / ChordHTKParser.FEATURE_SAMPLE_RATE;
            String chordName = tokenizer.nextToken().trim();
            float logliklihood = Float.parseFloat(tokenizer.nextToken().trim());

            ChordSegment chordSegment = new ChordSegment(startTime, endTime, chordName, logliklihood);
            chordSegments.add(chordSegment);
        }
        results.add(new ChordStructure(chordSegments, songFileName));
    }


    public void storeResults() {
        //First create outDir
        logger.info("Storing data into folder " + parsedLabelsDir);
        (getFile(parsedLabelsDir)).mkdirs();

        for (ChordStructure chordStructure : results) {
            chordStructure.saveSegmentsInFile(parsedLabelsDir);
        }
    }


    public ArrayList<ChordStructure> getResults() {
        return results;
    }


    protected void assignBeatTimings() {
        assignBeatTimings(results);
    }

    protected static void assignBeatTimings(ArrayList<ChordStructure> results) {
        Collections.sort(results);
        LabelsSource beatLabelsSource = new LabelsSource(Settings.beatLabelsGroundTruthDir, true, "beatGT", Extensions.BEAT_EXTENSIONS);

        for (ChordStructure cs : results) {
            BeatStructure beatStructure = BeatStructure.getBeatStructure(beatLabelsSource.getFilePathForSong(cs.getSongName()));

            List<BeatSegment> segments = beatStructure.getBeatSegments();
            segments.add(new BeatSegment(0, 0));
            beatStructure = new BeatStructure(segments, beatStructure.getSongName());

            double[] beats;
            if (FeaturesManager.downbeatGranulation) {
                beats = beatStructure.getDownBeats();
            } else{
                beats = beatStructure.getBeats();
            }

            for (int i = 0; i < cs.getChordSegments().size(); i++) {
                ChordSegment chordSegment = cs.getChordSegments().get(i);
                chordSegment.setOnset(beats[i]);
                if ( i + 1 < cs.getChordSegments().size()) {
                    chordSegment.setOffset(beats[i+1]);
                } else{
                    chordSegment.setOffset(beats[i] + 2 * (beats[i] - beats[i-1]));
                }
            }
        }
    }

//    protected void assignBeatTimings() {
//        Collections.sort(results);
//        LabelsSource beatLabelsSource = new LabelsSource(Settings.beatLabelsGroundTruthDir, true, "beatGT", Extensions.BEAT_EXTENSIONS);
//
//        for (ChordStructure cs : results) {
//            BeatStructure beatStructure = BeatStructure.getBeatStructure(beatLabelsSource.getFilePathForSong(cs.getSongName()));
//            double[] beats;
//            if (FeaturesManager.downbeatGranulation) {
//                beats = beatStructure.getDownBeats();
//            } else{
//                beats = beatStructure.getBeats();
//            }
//
//            for (ChordSegment chordSegment : cs.getChordSegments()) {
//                chordSegment.setOnset(beats[(int) chordSegment.getOnset()]);
//                chordSegment.setOffset(beats[(int) chordSegment.getOffset()]);
//            }
//        }
//    }



}
