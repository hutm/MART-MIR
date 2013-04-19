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

import org.imirsel.nema.model.NemaChord;
import org.mart.crs.management.label.chord.ChordSegment;
import org.mart.crs.utils.helper.HelperArrays;

import java.util.ArrayList;
import java.util.List;

/**
 * @version 1.0 4/19/12 12:42 AM
 * @author: Hut
 */
public class ChordEvaluatorTimeGridBeatSync extends ChordEvaluatorTimeGrid {

    public static final int maxNumberOfHypos = 12;


    @Override
    protected ChordEvalResult compareLabels(List<ChordSegment> chordList, List<ChordSegment> chordListGT, String song) {

        int[][] gridGT = getGrid(chordListGT);
        List<int[][]> gridSys = getGrid(chordList, maxNumberOfHypos);

        int overlap_total = 0;
        int lnOverlap = gridGT.length;
        for (int i = 0; i < lnOverlap; i++) {
            int[] match = new int[maxNumberOfHypos];
            for(int j = 0; j < maxNumberOfHypos; j++){
                if (i < gridSys.get(j).length && gridSys.get(j)[i] != null) {
                    match[j+1] = calcOverlap(gridGT[i], gridSys.get(j)[i]);
                }
            }
            overlap_total += HelperArrays.findMax(match);
        }

        //set eval metrics on input obj for track
        double overlap_score = (double) overlap_total / gridGT.length;
        return new ChordEvalResult(song, 1, 1, 1, overlap_score, 1, 1);
    }



    protected List<int[][]> getGrid(List<ChordSegment> chordList, int maxNumberOfHypos){
        List<int[][]> out = new ArrayList<int[][]>();

        //Create grid for the ground-truth
        int length = (int) Math.ceil(chordList.get(chordList.size() - 1).getOffset() * GRID_RESOLUTION);
        if (length == 0) {
            throw new IllegalArgumentException("Length of GT is 0!");
        }
        for (int i = 0; i < maxNumberOfHypos; i++) {
            out.add(new int[length][]);
        }
        for (int i = 0; i < chordList.size(); i++) {
            NemaChord currentChord = chordList.get(i);
            int onset_index = (int) (currentChord.getOnset() * GRID_RESOLUTION);
            int offset_index = (int) (currentChord.getOffset() * GRID_RESOLUTION);
            for (int j = onset_index; j < offset_index; j++) {
                addNotes(out, j, currentChord.getNotes());
            }
        }
        return out;
    }


    protected void addNotes(List<int[][]>out, int j, int[] notes){
        int index = 0;
        while(out.get(index)[j] != null){
            index++;
        }
        out.get(index)[j] = notes;
    }

}
