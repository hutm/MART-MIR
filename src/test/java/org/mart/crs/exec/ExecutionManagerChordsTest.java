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

package org.mart.crs.exec;

import junit.framework.Assert;
import org.mart.crs.utils.helper.Helper;
import org.mart.crs.utils.helper.HelperArrays;
import org.mart.crs.utils.helper.HelperFile;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import suite.MartSuiteTest;

import java.util.ArrayList;
import java.util.List;

/**
 * @version 1.0 10/19/12 1:07 PM
 * @author: Hut
 */
public class ExecutionManagerChordsTest {

    protected String cfgConfig;
    protected String workingDir;


    @BeforeMethod(groups = "integration")
    public void setUpChords() throws Exception {
        workingDir = String.format("%s/workChords", MartSuiteTest.getWorkingDirectoryFilePath());

        String audioFilePath = this.getClass().getResource("/audio/03_-_I'm_Only_Sleeping.wav").getPath();
        List<String> testFiles = new ArrayList<String>();
        testFiles.add(audioFilePath);
        String fileListPath = String.format("%s/listChords.txt", MartSuiteTest.getWorkingDirectoryFilePath());
        HelperFile.saveCollectionInFile(testFiles, fileListPath);

        String chordLabelsDirPath = this.getClass().getResource("/chordLabels").getPath();

        String cfgPrototype = this.getClass().getResource("/cfg/configChordsProto.cfg").getPath();
        cfgConfig = String.format("%s/configChords.cfg", MartSuiteTest.getWorkingDirectoryFilePath());
        List<String> configFileLines = HelperFile.readLinesFromTextFile(cfgPrototype);
        configFileLines.add(String.format("_workingDir=%s", workingDir));
        configFileLines.add(String.format("_waveFilesTrainFileList=%s", fileListPath));
        configFileLines.add(String.format("_waveFilesTestFileList=%s", fileListPath));
        configFileLines.add(String.format("chordLabelsGroundTruthDir=%s", chordLabelsDirPath));


        HelperFile.saveCollectionInFile(configFileLines, cfgConfig);
    }

    @Test(groups = {"integration"})
    public void testChordRecognition() {
        ExecutionManager.main(new String[]{"-c", cfgConfig});

        String resultsFilePath = String.format("%s/summaryChordEvaluatorNema.txt", workingDir);
        List<String> lines = HelperFile.readLinesFromTextFile(resultsFilePath);
        Assert.assertTrue(lines != null && lines.size() > 0);
        String lastLine = lines.get(lines.size() - 1);
        String score = lastLine.substring(lastLine.lastIndexOf("0."), lastLine.lastIndexOf("0.") + 4);
        float scoreFloat = Helper.parseFloat(score);
        Assert.assertTrue(scoreFloat >= 0.9);
    }






}
