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

package org.mart.crs.exec.operation.eval.chord;

import org.mart.crs.config.Extensions;
import org.mart.crs.exec.operation.eval.chord.confusion.ConfusionChordManager;
import org.mart.crs.management.label.LabelsParser;
import org.mart.crs.management.label.LabelsSource;
import org.mart.crs.management.label.chord.ChordSegment;
import org.mart.crs.management.label.chord.ChordStructure;
import org.mart.crs.management.label.chord.ChordType;
import org.mart.crs.utils.filefilter.ExtensionFileFilter;
import org.mart.crs.utils.helper.HelperArrays;
import org.mart.crs.utils.helper.HelperFile;
import sun.rmi.log.LogInputStream;

import java.io.File;
import java.util.*;

import static org.mart.crs.config.Extensions.LABEL_EXT;
import static org.mart.crs.utils.helper.HelperFile.getFile;
import static org.mart.crs.utils.helper.HelperFile.getPathForFileWithTheSameName;

/**
 * @version 1.0 4/18/12 10:17 PM
 * @author: Hut
 */
public class ChordEvaluatorTimeGridSeveralSources extends ChordEvaluatorTimeGrid{

    public static boolean majorVoting = false;

    protected List<LabelsSource> extraRecognizedLabelsSources;

    public void initializeDirectories(String recognizedDirPath, String groundTruthFolder, String outTxtFile, List<String> extraRecognizedDirPaths) {
        super.initializeDirectories(recognizedDirPath, groundTruthFolder, outTxtFile);
        this.extraRecognizedLabelsSources = new ArrayList<LabelsSource>();
        for(String extraDirPath:extraRecognizedDirPaths){
            extraRecognizedLabelsSources.add(new LabelsSource(extraDirPath, false, extraDirPath, Extensions.LABEL_EXT));
        }
    }

    @Override
    protected ChordEvalResult compareLabels(List<ChordSegment> chordList, List<ChordSegment> chordListGT, String song) {

        List<List<ChordSegment>>  additionalLabels = new ArrayList<List<ChordSegment>>();
        for(LabelsSource labelsSource:extraRecognizedLabelsSources){
            additionalLabels.add((new ChordStructure(labelsSource.getFilePathForSong(song))).getChordSegments());
        }


        int[][] gridGT = getGrid(chordListGT);
        int[][] gridSys = getGrid(chordList);

        List<int[][]> additionalGrids = new ArrayList<int[][]>();
        for(List<ChordSegment> chordSegments:additionalLabels){
            additionalGrids.add(getGrid(chordSegments));
        }


        int overlap_total = 0;
        int lnOverlap = Math.min(gridGT.length, gridSys.length);

        if (majorVoting) {
            for (int i = 0; i < lnOverlap; i++) {
                int[] notes = majorVoting(additionalGrids, i);
                int match = calcOverlap(gridGT[i], notes);
                overlap_total += match;
            }
        } else{
            for (int i = 0; i < lnOverlap; i++) {
                int[] match = new int[additionalGrids.size() + 1];
                match[0] = calcOverlap(gridGT[i], gridSys[i]);
                for(int j = 0; j < additionalGrids.size(); j++){
                    if (additionalGrids.get(j).length - 1 >= i) {
                        match[j+1] = calcOverlap(gridGT[i], additionalGrids.get(j)[i]);
                    }
                }
                overlap_total += HelperArrays.findMax(match);
            }
        }

        //set eval metrics on input obj for track
        double overlap_score = (double) overlap_total / gridGT.length;
        return new ChordEvalResult(song, 1, 1, 1, overlap_score, 1, 1);
    }




    public static int[] majorVoting(List<int[][]> grids, int frameIndex){
        Map<int[], Integer> hm = new LinkedHashMap<int[], Integer>();
        int maxCount = 0;
        int[] maxNotes = new int[]{};
        for (int i = 0; i < grids.size(); i++) {
            int[][]  item = grids.get(i);
            if(item.length - 1 < frameIndex){
                continue;
            }
            Integer count = hm.get(item[frameIndex]);
            int currentCount = count == null ? 1 : count + 1;
            if(currentCount > maxCount){
                maxCount = currentCount;
                maxNotes = item[frameIndex];
            }
            hm.put(item[frameIndex] , currentCount);
        }
        return maxNotes;
    }






}
