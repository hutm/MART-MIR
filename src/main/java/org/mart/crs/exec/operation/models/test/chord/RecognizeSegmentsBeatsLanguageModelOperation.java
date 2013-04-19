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

package org.mart.crs.exec.operation.models.test.chord;

import org.mart.crs.config.ExecParams;
import org.mart.crs.config.Settings;
import org.mart.crs.exec.operation.OperationType;
import org.mart.crs.exec.operation.domain.ChordFullTrainingOperationDomain;
import org.mart.crs.exec.scenario.stage.StageParameters;
import org.mart.crs.management.config.Configuration;
import org.mart.crs.model.htk.parser.chord.ChordHTKParserFromLatticeBeatSynchronous;
import org.mart.crs.model.htk.parser.chord.ChordHTKParserFromLatticeChordSynchronous;
import org.mart.crs.utils.helper.Helper;

import static org.mart.crs.config.Settings.EXECUTABLE_EXTENSION;
import static org.mart.crs.exec.scenario.stage.StageParameters.hmmDefs;
import static org.mart.crs.exec.scenario.stage.StageParameters.macros;

/**
 * @version 1.0 4/13/12 3:29 PM
 * @author: Hut
 */
public class RecognizeSegmentsBeatsLanguageModelOperation extends RecognizeSegmentsChordsLanguageModelOperation {

    public RecognizeSegmentsBeatsLanguageModelOperation(StageParameters stageParameters, ExecParams execParams) {
        super(stageParameters, execParams);
    }


    protected void finalEvaluation(String outRescoredFile, String recognizedFolder){
        ChordHTKParserFromLatticeBeatSynchronous parserFinal = new ChordHTKParserFromLatticeBeatSynchronous(outRescoredFile, recognizedFolder);
        parserFinal.run();
    }


    @Override
    protected void hvite(String trainedModelsDir, String hmmFolder, float penalty, String decodedOutPath) {
        String command = String.format("HVite%s -o N -C %s -m ", EXECUTABLE_EXTENSION, configPath);
        for (int i = 0; i < Configuration.NUMBER_OF_SEMITONES_IN_OCTAVE; i++) {
            command = command + String.format(" -H %s/%s/%s ", trainedModelsDir, hmmFolder + "_" + gaussianNumber, hmmDefs + i);
        }
        command = command + String.format(" -H %s/%s/%s -T 1 -S %s -i %s -n 4 4 -w %s -p %5.2f %s %s",
                trainedModelsDir, hmmFolder + "_" + gaussianNumber, macros, featureFileListTest, decodedOutPath,
                netFilePath, penalty, dictFilePath, wordListTestPath);
        Helper.execCmd(command);
    }



}
