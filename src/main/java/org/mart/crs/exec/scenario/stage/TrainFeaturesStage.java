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

package org.mart.crs.exec.scenario.stage;

import org.mart.crs.config.ExecParams;
import org.mart.crs.config.Settings;
import org.mart.crs.exec.operation.features.FeaturesExtractionOperation;
import org.mart.crs.utils.helper.HelperFile;

import java.io.File;

/**
 * @version 1.0 2/15/11 6:58 PM
 * @author: Hut
 */
public class TrainFeaturesStage extends AbstractStage implements ITrainTest {


    public TrainFeaturesStage(String parameterPrefix, AbstractStage childStage) {
        super(parameterPrefix, childStage);
        this.stageName = TRAIN_FEATURES_STAGE_NAME;
        this.stageNumber = TRAIN_FEATURES_STAGE_NUMBER;
    }

    @Override
    public void runStage(StageParameters stageParameters, ExecParams execParams) {
        scenario.addOperation(new FeaturesExtractionOperation(stageParameters, execParams));
    }

    @Override
    public boolean isDataOutputFound() {
        File featuresDir = HelperFile.getFile(getFeaturesDirPath());
        return featuresDir.exists() && featuresDir.listFiles().length > 0;
    }

    public String getFeaturesDirPath() {
        return getFilePathInDataDirectory("");
    }

    public String getFileListToProcess() {
        return execParams._waveFilesTrainFileList;
    }

    public boolean isFeaturesForTrain() {
        return true;
    }

    public String getRefFreqFilePath() {
        return ((TrainPreprocessingStage) stageParameters.getStage(TrainPreprocessingStage.class)).getRefFreqFilePath();
    }


}
