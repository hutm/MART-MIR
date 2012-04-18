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

import org.mart.crs.config.Extensions;
import org.mart.crs.config.Settings;
import org.mart.crs.management.beat.BeatStructure;
import org.mart.crs.management.label.LabelsSource;
import org.mart.crs.management.label.chord.ChordSegment;
import org.mart.crs.management.label.chord.ChordStructure;
import org.mart.crs.utils.helper.HelperFile;
import sun.rmi.log.LogInputStream;
import sun.security.x509.Extension;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @version 1.0 4/17/12 10:14 AM
 * @author: Hut
 */
public class LogLikStatistics {

    protected List<Double> correctSegment;
    protected List<Double> oneBeatShift;
    protected List<Double> twoBeatsShift;

    protected List<BeatSyncChord> beatSyncChordList;
    protected List<BeatSyncChord> beatSyncChordListOneModel;
    protected List<BeatSyncChord> beatSyncChordListGT;

    protected LabelsSource beatLabelSource;
    protected LabelsSource chordGTLabelsource;

    protected LabelsSource chordhypoLabelsource;
    protected LabelsSource chordhypoLabelsourceOneModel;


    public LogLikStatistics() {
        this.beatLabelSource = new LabelsSource("/home/hut/prg/PROJECTS/mirdata/beatles/beatLab", true, "beatGT", Extensions.BEAT_EXTENSIONS);
        this.chordGTLabelsource = new LabelsSource("/home/hut/prg/PROJECTS/mirdata/beatles/chordLab", true, "chordGT", Extensions.LABEL_EXT);
        this.chordhypoLabelsource = new LabelsSource("/home/hut/prg/PROJECTS/temp/-firstPass-", false, "chordGT", Extensions.LABEL_EXT);
        this.chordhypoLabelsourceOneModel = new LabelsSource("/home/hut/prg/PROJECTS/temp/-firstPass-OneModel", false, "chordGT", Extensions.LABEL_EXT);

        correctSegment = new ArrayList<Double>();
        oneBeatShift = new ArrayList<Double>();
        twoBeatsShift = new ArrayList<Double>();
        beatSyncChordList = new ArrayList<BeatSyncChord>();
        beatSyncChordListOneModel = new ArrayList<BeatSyncChord>();
        beatSyncChordListGT = new ArrayList<BeatSyncChord>();

        init();
    }


    protected void init() {
        File[] files = HelperFile.getFile(chordhypoLabelsource.getPath()).listFiles();
        for (File file : files) {
            System.out.println(String.format("Processing file %s", file.getName()));
            String chordPath = file.getPath();
            ChordStructure chordStructure = new ChordStructure(chordPath);
            ChordStructure chordStructureOneModel = new ChordStructure(chordhypoLabelsourceOneModel.getFilePathForSong(file.getName()));
            ChordStructure chordStructureGT = new ChordStructure(chordGTLabelsource.getFilePathForSong(file.getName()));

            BeatStructure beatStructure = BeatStructure.getBeatStructure(beatLabelSource.getFilePathForSong(file.getName()));
            beatStructure.addTrailingBeats(chordStructure.getSongDuration());
            double[] timings = beatStructure.getBeats();

            beatSyncChordList = assignBeatTimings(chordStructure, timings);
            beatSyncChordListOneModel = assignBeatTimings(chordStructureOneModel, timings);
            beatSyncChordListGT = assignBeatTimings(chordStructureGT, timings);



            for (BeatSyncChord beatSyncChordGT : beatSyncChordListGT) {
                int startGT = beatSyncChordGT.getStartBeat();
                int endGT = beatSyncChordGT.getEndBeat();

                if(Math.abs(timings[startGT] - beatSyncChordGT.getChord().getOnset()) + Math.abs(timings[endGT] - beatSyncChordGT.getChord().getOffset()) > 0.25){
                    continue;
                }

                for (BeatSyncChord beatSyncChord : beatSyncChordList) {
                    int start = beatSyncChord.getStartBeat();
                    int end = beatSyncChord.getEndBeat();

                    //First find oneModel segment
                    BeatSyncChord beatSyncChordOneModelFound = null;
                    for(BeatSyncChord beatSyncChordOneModel: beatSyncChordListOneModel){
                        if(beatSyncChordOneModel.getStartBeat() == start && beatSyncChordOneModel.getEndBeat() == end){
                            beatSyncChordOneModelFound = beatSyncChordOneModel;
                            break;
                        }
                    }


                    double logLik =  beatSyncChord.getChord().getLogLikelihood() - beatSyncChordOneModelFound.getChord().getLogLikelihood();


                    if(startGT ==  start && endGT == end){
                        correctSegment.add(logLik);
                    }
                    if (endGT - startGT >= 2) {
                        if(startGT == start + 1 && (endGT == end || endGT == end + 1)){
                            oneBeatShift.add(logLik);
                        }
                        if(endGT == end - 1 && (startGT == start || startGT == start - 1)){
                            oneBeatShift.add(logLik);
                        }
                    }
                }
            }
        }

    }



    protected List<BeatSyncChord> assignBeatTimings(ChordStructure chordStructure, double[] timings){
        List<BeatSyncChord> outList = new ArrayList<BeatSyncChord>();
        for (ChordSegment segment : chordStructure.getChordSegments()) {
            int startClosestIndex = -1;
            double startClosestDistance = Float.MAX_VALUE;
            int endClosestIndex = -1;
            double endClosestDistance = Float.MAX_VALUE;

            double startTime = segment.getOnset();
            double endTime = segment.getOffset();

            for (int i = 0; i < timings.length; i++) {
                if (Math.abs(startTime - timings[i]) < startClosestDistance) {
                    startClosestDistance = Math.abs(startTime - timings[i]);
                    startClosestIndex = i;
                }
                if (Math.abs(endTime - timings[i]) < endClosestDistance) {
                    endClosestDistance = Math.abs(endTime - timings[i]);
                    endClosestIndex = i;
                }
            }
            outList.add(new BeatSyncChord(startClosestIndex, endClosestIndex, segment));
        }
        return outList;
    }


    public List<Double> getCorrectSegment() {
        return correctSegment;
    }

    public List<Double> getOneBeatShift() {
        return oneBeatShift;
    }

    public List<Double> getTwoBeatsShift() {
        return twoBeatsShift;
    }

    public static void main(String[] args) {
        LogLikStatistics statistics = new LogLikStatistics();
        statistics.saveStatististicsInfile("outData/NotempFull_short");
    }


    public void saveStatististicsInfile(String fileName) {
        saveStringInAFile(getDoublesAsString(correctSegment), fileName + "_correct");
        saveStringInAFile(getDoublesAsString(oneBeatShift), fileName + "_one");
    }

    protected String getDoublesAsString(List<Double> doubles) {
        StringBuilder builder = new StringBuilder();
        for (Double numDouble : doubles) {
            builder.append(String.format("%5.2f ", numDouble));
        }
        return builder.toString();
    }

    protected void saveStringInAFile(String string, String filePath) {
        List<String> strings = new ArrayList<String>();
        strings.add(string);
        HelperFile.saveCollectionInFile(strings, filePath);
    }


}


class BeatSyncChord implements Comparable<BeatSyncChord> {

    protected int startBeat;
    protected int endBeat;
    protected ChordSegment chord;

    BeatSyncChord(int startBeat, int endBeat, ChordSegment chord) {
        this.startBeat = startBeat;
        this.endBeat = endBeat;
        this.chord = chord;
    }


    public int getStartBeat() {
        return startBeat;
    }

    public void setStartBeat(int startBeat) {
        this.startBeat = startBeat;
    }

    public int getEndBeat() {
        return endBeat;
    }

    public void setEndBeat(int endBeat) {
        this.endBeat = endBeat;
    }

    public ChordSegment getChord() {
        return chord;
    }

    public void setChord(ChordSegment chord) {
        this.chord = chord;
    }


    @Override
    public int compareTo(BeatSyncChord o) {
        return chord.compareTo(o.getChord());  //To change body of implemented methods use File | Settings | File Templates.
    }
}
