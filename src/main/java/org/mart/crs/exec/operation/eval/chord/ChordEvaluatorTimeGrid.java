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

package org.mart.crs.exec.operation.eval.chord;

import org.imirsel.nema.model.NemaChord;
import org.imirsel.nema.model.util.ChordConversionUtil;
import org.mart.crs.config.Settings;
import org.mart.crs.management.label.chord.ChordSegment;

import java.util.List;

/**
 * @version 1.0 4/18/12 6:57 PM
 * @author: Hut
 */
public class ChordEvaluatorTimeGrid extends ChordEvaluator {


    public static final int GRID_RESOLUTION = 1000; //The grid resolution.

    @Override
    protected ChordEvalResult compareLabels(List<ChordSegment> chordList, List<ChordSegment> chordListGT, String song) {

        int[][] gridGT = getGrid(chordListGT);
        int[][] gridSys = getGrid(chordList);

        int overlap_total = 0;
        int lnOverlap = Math.min(gridGT.length, gridSys.length);
        for (int i = 0; i < lnOverlap; i++) {
            int[] gtFrame = gridGT[i];
            int[] sysFrame = gridSys[i];
            overlap_total += calcOverlap(gtFrame, sysFrame);
        }

        //set eval metrics on input obj for track
        double overlap_score = (double) overlap_total / gridGT.length;
        return new ChordEvalResult(song, 1, 1, 1, overlap_score, 1, 1);
    }



    protected int[][] getGrid(List<ChordSegment> chordList){
        //Create grid for the ground-truth
        int lnGT = (int) Math.ceil(chordList.get(chordList.size() - 1).getOffset() * GRID_RESOLUTION);
        if (lnGT == 0) {
            throw new IllegalArgumentException("Length of GT is 0!");
        }
        int[][] gridGT = new int[lnGT][];
        for (int i = 0; i < chordList.size(); i++) {
            NemaChord currentChord = chordList.get(i);
            int onset_index = (int) (currentChord.getOnset() * GRID_RESOLUTION);
            int offset_index = (int) (currentChord.getOffset() * GRID_RESOLUTION);
            for (int j = onset_index; j < offset_index; j++) {
                gridGT[j] = currentChord.getNotes();
            }
        }
        return gridGT;
    }




    protected int calcOverlap(int[] gt, int[] sys) {


        if (gt == null || sys == null) {
            return 0;
        } else if (gt.length == 1 && sys.length == 1) {
            if (gt[0] == 24 && sys[0] == 24) {
                return 1;
            } else {
                return 0;
            }

        } else {
            int match_ctr = 0;
            for (int i = 0; i < sys.length; i++) {
                if (involves(sys[i], gt)) {
                    match_ctr++;
                }
            }
            int threshold = 3;
            String chord = ChordConversionUtil.getInstance().convertNoteNumbersToShorthand(gt);
            if (chord.contains("dim") || chord.contains("aug")) {
                threshold = 2;
            }
            if (match_ctr >= threshold) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    private boolean involves(int key, int[] set) {

        for (int i = 0; i < set.length; i++) {
            if (set[i] == key) {
                return true;
            }
        }
        return false;
    }



    public static void main(String args[]){
        Settings.initialize();
        ChordEvaluatorTimeGrid chordEvaluatorTimeGrid = new ChordEvaluatorTimeGrid();
        chordEvaluatorTimeGrid.initializeDirectories(args[0], args[1]);
        chordEvaluatorTimeGrid.evaluate();
    }


}
