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
import org.mart.crs.config.Extensions;
import org.mart.crs.config.Settings;
import org.mart.crs.exec.operation.domain.AbstractCRSOperation;
import org.mart.crs.exec.operation.domain.ChordFullTrainingOperationDomain;
import org.mart.crs.exec.operation.OperationType;
import org.mart.crs.exec.scenario.stage.StageParameters;
import org.mart.crs.exec.scenario.stage.TestFeaturesStage;
import org.mart.crs.exec.scenario.stage.TestRecognizeStage;
import org.mart.crs.exec.scenario.stage.TrainModelsStage;
import org.mart.crs.management.config.Configuration;
import org.mart.crs.management.label.chord.ChordStructure;
import org.mart.crs.management.label.chord.ChordType;
import org.mart.crs.management.label.chord.Root;
import org.mart.crs.model.htk.HTKResultsParser;
import org.mart.crs.model.htk.parser.chord.ChordHTKParser;
import org.mart.crs.model.htk.parser.chord.ChordHTKParserFromLattice;
import org.mart.crs.model.htk.parser.chord.ChordHTKParserFromLatticeBeatSynchronous;
import org.mart.crs.model.htk.parser.chord.ChordHTKParserPerBeat;
import org.mart.crs.utils.filefilter.ExtensionFileFilter;
import org.mart.crs.utils.helper.Helper;
import org.mart.crs.utils.helper.HelperFile;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mart.crs.config.Settings.EXECUTABLE_EXTENSION;
import static org.mart.crs.exec.scenario.stage.StageParameters.*;
import static org.mart.crs.management.label.chord.ChordType.NOT_A_CHORD;
import static org.mart.crs.management.label.chord.ChordType.chordDictionary;
import static org.mart.crs.utils.helper.HelperFile.createFileList;
import static org.mart.crs.utils.helper.HelperFile.getFile;
/**
 * @version 1.0 11-Jun-2010 16:49:09
 * @author: Hut
 */
public class RecognizeOperation extends AbstractCRSOperation {

    protected String resultsDir;
    protected String trainedModelsDir;
    protected String extractedFeaturesDir;
    protected String lmDir;
    protected String netHViteFilePath;

    protected String gramFilePath;
    protected String decodedOutPath;
    protected String featureFileListTest;
    protected String netFilePath;


    protected int gaussianNumber;
    protected float penalty;
    protected int interationInHEREST;

    protected boolean isToOutputLattices;

    public RecognizeOperation(StageParameters stageParameters, ExecParams execParams) {
        super(stageParameters, execParams);
    }


    @Override
    public void initialize() {
        super.initialize();

        TestRecognizeStage testRecognizeStage = (TestRecognizeStage) stageParameters.getStage(TestRecognizeStage.class);
        TrainModelsStage trainModelsStage = (TrainModelsStage) stageParameters.getStage(TrainModelsStage.class);
        TestFeaturesStage testFeaturesStage = ((TestFeaturesStage) stageParameters.getStage(TestFeaturesStage.class));

        this.resultsDir = testRecognizeStage.getResultsDirPath();
        this.trainedModelsDir = trainModelsStage.getHMMsDirPath();
        this.lmDir = trainModelsStage.getLanguageModelsDirPath();

        String featuresDirLocalCopy = String.format("%s/%s", this.tempDirPath, FEATURES_DIR_NAME);
        HelperFile.copyDirectory(testFeaturesStage.getFeaturesDirPath(), featuresDirLocalCopy);
        this.extractedFeaturesDir = featuresDirLocalCopy;

        netHViteFilePath = trainModelsStage.getNetHViteFilePath();

        this.gaussianNumber = execParams.gaussianNumber;
        this.penalty = execParams.penalty;
        this.interationInHEREST = execParams.interationInHEREST;


        gramFilePath = String.format("%s/%s", tempDirPath, GRAM_FILE);
        decodedOutPath = String.format("%s/%s", tempDirPath, DECODED_OUT);
        featureFileListTest = String.format("%s/%s", tempDirPath, FEATURE_FILELIST_TEST);
        netFilePath = String.format("%s/%s", tempDirPath, NET_FILE);

        isToOutputLattices = false;
        createFileList(extractedFeaturesDir, featureFileListTest, new ExtensionFileFilter(Extensions.CHROMA_EXT), true);
        operationDomain.createDictionaryFile();
        createGrammarFile();
        hParse();
    }


    public void operate() {
        String outFilePath = decodedOutPath + String.format("%d_%2.1f", gaussianNumber, penalty);
        if (execParams.recognizeAtEachIteration) {
            hvite(trainedModelsDir, hmmFolder + "_" + gaussianNumber + "_" + interationInHEREST, penalty, outFilePath);
        } else {
            hvite(trainedModelsDir, hmmFolder + "_" + gaussianNumber, penalty, outFilePath);
        }
        String recognizedFolder = resultsDir + File.separator + "-";

//        ChordHTKParser parser = new ChordHTKParser(outFilePath, recognizedFolder);
//        parser.run();

        if(Settings.operationType.equals(OperationType.CHORD_OPERATION_PER_BEAT)){
            ChordHTKParserPerBeat parser = new ChordHTKParserPerBeat(outFilePath, recognizedFolder);
            parser.run();
        } else{
            ChordHTKParser parser = new ChordHTKParser(outFilePath, recognizedFolder);
            parser.run();
        }

//        HTKResultsParser.parse(outFilePath, recognizedFolder);
//        EvaluatorOld evaluator = new EvaluatorOld();
//        evaluator.evaluate(recognizedFolder, Settings.labelsGroundTruthDir, recognizedFolder + ".txt");
    }

    protected void createGrammarFile(boolean isOnlyOneChord) {
        if(Settings.useFrameLevelTranscript){
            createGrammarFileForFrameLevelTranscript(isOnlyOneChord);
            return;
        }
        try {
            StringBuffer buffer = new StringBuffer();
            FileWriter writer = new FileWriter(getFile(gramFilePath));
            buffer.append("$chords = ");
            for (ChordType modality : chordDictionary) {
                if (!modality.equals(ChordType.NOT_A_CHORD)) {
                    for (Root root : Root.values()) {
                        buffer.append(String.format("%s%s | ", root, modality));
                    }
                }
            }
            if (Arrays.asList(chordDictionary).contains(NOT_A_CHORD)) {
                buffer.append(ChordType.NOT_A_CHORD.getOriginalName());
            } else {
                buffer.deleteCharAt(buffer.length() - 2);
            }
            buffer.append(";\n");
            writer.write(buffer.toString());
            if (!isOnlyOneChord) {
                writer.write(defineRecognitionOutputRule());
            } else {
                writer.write("($chords)");
            }
            writer.close();
        } catch (IOException e) {
            logger.error(Helper.getStackTrace(e));
        }
    }


    protected void createGrammarFileForFrameLevelTranscript(boolean isOnlyOneChord) {
        try {
            StringBuffer buffer = new StringBuffer();
            FileWriter writer = new FileWriter(getFile(gramFilePath));
            buffer.append("(");
            for (ChordType modality : chordDictionary) {
                if (!modality.equals(ChordType.NOT_A_CHORD)) {
                    for (Root root : Root.values()) {
                        buffer.append(String.format("{%s%s} | ", root, modality));
                    }
                }
            }
            if (Arrays.asList(chordDictionary).contains(NOT_A_CHORD)) {
                buffer.append(String.format("{%s}", ChordType.NOT_A_CHORD.getOriginalName()));
            } else {
                buffer.deleteCharAt(buffer.length() - 2);
            }
            buffer.append(")\r\n");
            writer.write(buffer.toString());
            writer.close();
        } catch (IOException e) {
            logger.error(Helper.getStackTrace(e));
        }
    }

    protected void createGrammarFile() {
        createGrammarFile(false);
//        createGrammarFileFromGroundTruthForCoverID(); //TODO remove hardcoded String
    }


    protected void createGrammarFileFromGroundTruthForCoverID(){
        try {
            StringBuffer buffer = new StringBuffer();
            List<String> sentences = new ArrayList<String>();
            FileWriter writer = new FileWriter(getFile(gramFilePath));

            String gtLabelsDir = Settings.chordLabelsGroundTruthDir; //TODO remove hardcoded String
            //Read gtchord sequences from gt label files
            File[] gtFiles = HelperFile.getFile(gtLabelsDir).listFiles();
            for(File labelFile:gtFiles){
                ChordStructure chordStructure = new ChordStructure(labelFile.getPath());
                String fileNameVar = HelperFile.getNameWithoutExtension(labelFile.getPath()).replace("_Render", "");
                
                for(int i = 0; i < Configuration.NUMBER_OF_SEMITONES_IN_OCTAVE; i++){
                    String sentenceVar = String.format("$%s%d", fileNameVar, i);
                    sentences.add(sentenceVar);
                    buffer.append(sentenceVar).append(" = ");
                    ChordStructure transposedStructure = chordStructure.transpose(i);
                    String chordSequence = transposedStructure.getChordSequenceWithoutTimings();
                    buffer.append(chordSequence).append(";\r\n");
                }
            }



            buffer.append("$out = ");
            for (ChordType modality : chordDictionary) {
                    for (String sentence:sentences) {
                        buffer.append(String.format("%s | ", sentence));
                    }
            }
            buffer.deleteCharAt(buffer.length() - 2);
            buffer.append(";\r\n");
            writer.write(buffer.toString());
            writer.write("($out)");

            writer.close();
        } catch (IOException e) {
            logger.error("Problems: ");
            logger.error(Helper.getStackTrace(e));
        }
    }


    protected String defineRecognitionOutputRule() {
        return "({$chords})";
    }


    protected void hParse() {
        String command = String.format("HParse%s %s %s", EXECUTABLE_EXTENSION, gramFilePath, netFilePath);
        Helper.execCmd(command);
    }

    protected void hvite(String trainedModelsDir, String hmmFolder, float penalty, String decodedOutPath) {
        String command = String.format("HVite%s -o N -t 250.0 -C %s ",  EXECUTABLE_EXTENSION, configPath);

        //In case of full train, there are no rotated hmm def files
        int numberOfHmms;
        if (Settings.operationType.equals(OperationType.CHORD_OPERATION_FULL_TRAIN)) {
            numberOfHmms = ChordFullTrainingOperationDomain.getNumberOfTrainedHmms();
//            command = command + String.format(" -H %s/%s/%s ", trainedModelsDir, hmmFolder, hmmDefs);
        } else {
            numberOfHmms = Configuration.NUMBER_OF_SEMITONES_IN_OCTAVE;
        }
        for (int i = 0; i < numberOfHmms; i++) {
            command = command + String.format(" -H %s/%s/%s ", trainedModelsDir, hmmFolder, hmmDefs + i);
        }
        command = command + String.format(" -H %s/%s/%s -T 1 -S %s -i %s ",  trainedModelsDir, hmmFolder, macros, featureFileListTest, decodedOutPath);

        if(Settings.useFrameLevelTranscript || isToOutputLattices){
            command = command + String.format(" -n %d %d ", execParams.NBestCalculationLatticeOrder, execParams.NBestCalculationLatticeOrder);
        }
        if (isToOutputLattices) {
            command = command + String.format("-z lattice -q Atvaldmn ");
        }
        command = command + String.format(" -w %s -p %5.2f %s %s", netFilePath, penalty, dictFilePath, wordListTestPath);

        Helper.execCmd(command);
    }


}
