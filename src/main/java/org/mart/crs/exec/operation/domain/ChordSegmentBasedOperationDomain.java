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
import org.mart.crs.exec.operation.models.test.chord.RecognizeSegmentsChordsLanguageModelOperation;
import org.mart.crs.exec.operation.models.test.chord.RecognizeSegmentsOperation;
import org.mart.crs.exec.scenario.stage.StageParameters;
import org.mart.crs.management.features.manager.FeaturesManagerSegmentBasedTest;
import org.mart.crs.management.features.manager.FeaturesManagerChord;
import org.mart.crs.management.label.chord.ChordType;
import org.mart.crs.utils.helper.Helper;
import org.mart.crs.utils.helper.HelperFile;

import java.io.FileWriter;
import java.io.IOException;

/**
 * @version 1.0 4/11/12 6:46 PM
 * @author: Hut
 */
public class ChordSegmentBasedOperationDomain extends ChordOperationDomain {


    @Override
    public AbstractCRSOperation getRecognizeOperation(StageParameters stageParameters, ExecParams execParams) {
        return new RecognizeSegmentsOperation(stageParameters, execParams);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public AbstractCRSOperation getRecognizeLanguageModelOperation(StageParameters stageParameters, ExecParams execParams) {
        return new RecognizeSegmentsChordsLanguageModelOperation(stageParameters, execParams);
    }


    @Override
    public FeaturesManagerChord getFeaturesManager(String songFilePath, String outDirPath, boolean isForTraining, ExecParams execParams) {
        return new FeaturesManagerSegmentBasedTest(songFilePath, outDirPath, isForTraining, execParams);    //To change body of overridden methods use File | Settings | File Templates.
    }







}
