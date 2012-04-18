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

package org.mart.crs.exec.operation.models.test.chord;

import org.mart.crs.config.ExecParams;
import org.mart.crs.exec.scenario.stage.StageParameters;
import org.mart.crs.model.htk.parser.chord.ChordHTKParserSegmentBasedHypotheses;

import java.io.File;

import static org.mart.crs.exec.scenario.stage.StageParameters.hmmFolder;

/**
 * @version 1.0 4/11/12 6:54 PM
 * @author: Hut
 */
public class RecognizeSegmentsOperation extends RecognizeOperation {

    public RecognizeSegmentsOperation(StageParameters stageParameters, ExecParams execParams) {
        super(stageParameters, execParams);
    }

    protected void createGrammarFile() {
        createGrammarFile(true);
    }

    @Override
    protected String defineRecognitionOutputRule() {
        return "($chords)";
    }


    public void operate() {
        String outFilePath = decodedOutPath + String.format("%d_%2.1f", gaussianNumber, penalty);
        hvite(trainedModelsDir, hmmFolder + "_" + gaussianNumber, penalty, outFilePath);
        String recognizedFolder = resultsDir + File.separator + "-";

        ChordHTKParserSegmentBasedHypotheses parser = new ChordHTKParserSegmentBasedHypotheses(outFilePath, recognizedFolder);
        parser.run();
    }

}
