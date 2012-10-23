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

import org.mart.crs.config.Settings;
import org.mart.crs.management.label.chord.ChordStructure;
import org.mart.crs.utils.helper.HelperFile;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @version 1.0 4/18/12 9:55 PM
 * @author: Hut
 */

public class ChordEvaluatorTimeGridTest {

    @Test(groups = {"static"})
    public void testEvaluate(){
//        ChordEvaluator.PERFORM_REFINING_IN_CHORD_HYPOS = true;
        ChordEvaluatorTimeGrid chordEvaluatorTimeGrid = new ChordEvaluatorTimeGrid();
        chordEvaluatorTimeGrid.initializeDirectories("/home/hut/prg/PROJECTS/temp/checkConsistency/-", "/home/hut/mirdata/beatles/chordLab", "/home/hut/temp/garbage/TimeGrid.txt");
        chordEvaluatorTimeGrid.evaluate();
    }



    @Test(groups = {"static"})
    public void testEvaluateFromSeveralSources(){
        ChordEvaluatorTimeGridSeveralSources.majorVoting = true;

        List<String> additionalSources = new ArrayList<String>();
        additionalSources.add("/home/hut/prg/PROJECTS/temp/multiPenalty/-35");
//        additionalSources.add("/home/hut/prg/PROJECTS/temp/multiPenalty/-20");
        additionalSources.add("/home/hut/prg/PROJECTS/temp/multiPenalty/-50");
        additionalSources.add("/home/hut/prg/PROJECTS/temp/multiPenalty/-5");
//        additionalSources.add("/home/hut/prg/PROJECTS/temp/multiPenalty/5");


        ChordEvaluatorTimeGridSeveralSources chordEvaluatorTimeGrid = new ChordEvaluatorTimeGridSeveralSources();
        chordEvaluatorTimeGrid.initializeDirectories("/home/hut/prg/PROJECTS/temp/multiPenalty/-35", "/home/hut/mirdata/beatles/chordLab", "/home/hut/prg/PROJECTS/temp/multiPenalty/-35-5.txt", additionalSources);
        chordEvaluatorTimeGrid.evaluate();

    }

    @Test(groups = {"static"})
    public void testEvaluateHypos(){
        ChordEvaluatorTimeGridBeatSync chordEvaluatorTimeGrid = new ChordEvaluatorTimeGridBeatSync();
        chordEvaluatorTimeGrid.initializeDirectories("/home/hut/prg/PROJECTS/temp/-firstPass-", "/home/hut/mirdata/beatles/chordLab", "/home/hut/prg/PROJECTS/temp/-firstPass-.txt");
        chordEvaluatorTimeGrid.evaluate();

    }


    @Test(groups = {"static"})
    public void testEvaluateHyposRefine(){
        ChordEvaluator.PERFORM_REFINING_IN_CHORD_HYPOS = true;
        ChordEvaluatorTimeGrid chordEvaluatorTimeGrid = new ChordEvaluatorTimeGrid();
        chordEvaluatorTimeGrid.initializeDirectories("/home/hut/prg/PROJECTS/temp/-firstPass-", "/home/hut/mirdata/beatles/chordLab", "/home/hut/prg/PROJECTS/temp/RefineHypos.txt");
        chordEvaluatorTimeGrid.evaluate();

    }


    @Test(groups = {"static"})
    public void testEvaluateHyposRefineLeavingOrder(){
        ChordEvaluator.PERFORM_REFINING_IN_CHORD_HYPOS = true;
        ChordEvaluator.REFINING_IN_CHORD_HYPOS_ORDER = 1;
        ChordEvaluatorTimeGridBeatSync chordEvaluatorTimeGrid = new ChordEvaluatorTimeGridBeatSync();
        chordEvaluatorTimeGrid.initializeDirectories("/home/hut/prg/PROJECTS/temp/checkConsistency/-firstPass-", "/home/hut/mirdata/beatles/chordLab", "/home/hut/temp/garbage/RefineHypos_2.txt");
        chordEvaluatorTimeGrid.evaluate();
    }


    @Test(groups = {"static"})
    public void transformChordLabels(){
        File[] inFiles = HelperFile.getFile("/home/hut/prg/PROJECTS/temp/checkConsistency/-firstPass-").listFiles();
        String outFolder = "/home/hut/prg/PROJECTS/temp/checkConsistency/-firstPass-Transformed";
        HelperFile.createDir(outFolder);
        for(File file:inFiles){
            ChordStructure chordStructure = new ChordStructure(file.getPath());
            chordStructure.refineHypothesesLeavingOrder(1, Settings.beatLabelsGroundTruthDir);
            chordStructure.saveSegmentsInFile(outFolder);
        }
    }

    @Test(groups = {"static"})
    public void testEvaluate8088(){
        Settings.initialize();
        ChordEvaluatorTimeGrid chordEvaluatorTimeGrid = new ChordEvaluatorTimeGrid();
        chordEvaluatorTimeGrid.initializeDirectories("/home/hut/prg/PROJECTS/temp/8088/80", "/home/hut/mirdata/beatles/chordLab", "/home/hut/prg/PROJECTS/temp/8088/80_full.txt");
        chordEvaluatorTimeGrid.evaluate();
    }


    @Test(groups = {"static"})
    public void testEvaluateChordsGeneral(){
        Settings.initialize();
        ChordEvaluatorTimeGrid chordEvaluatorTimeGrid = new ChordEvaluatorTimeGrid();
        chordEvaluatorTimeGrid.initializeDirectories("/home/hut/temp/ANDCHORDS/REALTIME", "/home/hut/mirdata/beatles/chordLab", "/home/hut/temp/ANDCHORDS/REALTIMEResults.txt");
        chordEvaluatorTimeGrid.evaluate();
    }


    @Test(groups = {"static"})
    public void testEvaluateChordsCR(){
        Settings.initialize();
        ChordEvaluatorTimeGrid chordEvaluatorTimeGrid = new ChordEvaluatorTimeGrid();
        chordEvaluatorTimeGrid.initializeDirectories("/home/hut/work/TEST___/5_1024_025_nicenow", "/home/hut/mirdata/beatles/chordLab", "/home/hut/work/TEST___/Results/5_1024_025_nicenow.txt");
        chordEvaluatorTimeGrid.evaluate();
    }




}
