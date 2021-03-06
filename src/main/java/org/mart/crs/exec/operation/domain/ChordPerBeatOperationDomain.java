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

package org.mart.crs.exec.operation.domain;

import org.mart.crs.config.ExecParams;
import org.mart.crs.exec.operation.models.test.chord.RecognizeConventionalVersionWithLMOperation;
import org.mart.crs.exec.operation.models.training.chord.TrainingLanguageModelsPerBeatOperation;
import org.mart.crs.exec.scenario.stage.StageParameters;
import org.mart.crs.management.features.manager.FeaturesManagerChord;
import org.mart.crs.management.features.manager.FeaturesManagerChordPerBeat;

/**
 * @version 1.0 3/16/12 7:50 PM
 * @author: Hut
 */
public class ChordPerBeatOperationDomain extends ChordOperationDomain {

    @Override
    public AbstractCRSOperation getRecognizeLanguageModelOperation(StageParameters stageParameters, ExecParams execParams) {
        return new RecognizeConventionalVersionWithLMOperation(stageParameters, execParams);
    }

    @Override
    public AbstractCRSOperation getTrainLanguageModelsOperation(StageParameters stageParameters, ExecParams execParams) {
        return new TrainingLanguageModelsPerBeatOperation(stageParameters, execParams);
    }

    @Override
    public FeaturesManagerChord getFeaturesManager(String songFilePath, String outDirPath, boolean isForTraining, ExecParams execParams) {
        return new FeaturesManagerChordPerBeat(songFilePath, outDirPath, isForTraining, execParams);
    }


}
