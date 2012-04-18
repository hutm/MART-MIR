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
import org.mart.crs.config.Extensions;
import org.mart.crs.config.Settings;
import org.mart.crs.exec.scenario.stage.StageParameters;
import org.mart.crs.management.config.Configuration;
import org.mart.crs.management.label.chord.ChordStructure;
import org.mart.crs.management.label.chord.ChordType;
import org.mart.crs.management.label.chord.Root;
import org.mart.crs.management.label.lattice.Lattice;
import org.mart.crs.model.htk.parser.chord.ChordHTKParserFromLatticeChordSynchronous;
import org.mart.crs.model.htk.parser.chord.ChordHTKParserSegmentBasedHypotheses;
import org.mart.crs.utils.filefilter.ExtensionFileFilter;
import org.mart.crs.utils.helper.Helper;
import org.mart.crs.utils.helper.HelperFile;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.mart.crs.config.Settings.EXECUTABLE_EXTENSION;
import static org.mart.crs.config.Settings.FIELD_SEPARATOR;
import static org.mart.crs.exec.scenario.stage.StageParameters.*;
import static org.mart.crs.exec.scenario.stage.StageParameters.hmmFolder;
import static org.mart.crs.management.label.chord.ChordType.NOT_A_CHORD;
import static org.mart.crs.management.label.chord.ChordType.chordDictionary;
import static org.mart.crs.utils.helper.HelperFile.createFileList;
import static org.mart.crs.utils.helper.HelperFile.getFile;

/**
 * @version 1.0 4/12/12 3:57 PM
 * @author: Hut
 */
public class RecognizeSegmentsChordsLanguageModelOperation extends RecognizeSegmentsOperation {

    public RecognizeSegmentsChordsLanguageModelOperation(StageParameters stageParameters, ExecParams execParams) {
        super(stageParameters, execParams);
    }


    @Override
    public void operate() {
        String command;

        String outFeaturesFolder = String.format("%s/%s", tempDirPath, "-firstPass-");

        //If time-consuming step of acoustic decoding has been accomplished, skip it
        if (! HelperFile.getFile(outFeaturesFolder).exists()) {
            hvite(trainedModelsDir, hmmFolder, penalty, decodedOutPath);
            ChordHTKParserSegmentBasedHypotheses parser = new ChordHTKParserSegmentBasedHypotheses(decodedOutPath, outFeaturesFolder);
            parser.run();
        }

        // results without language modeling
        String recognizedFolder = resultsDir + File.separator + "-";
        HelperFile.copyDirectory(outFeaturesFolder, recognizedFolder);

        //Now parse hypotheses from first pass processing
        Lattice aLattice;
        File[] firstPassFeatureFiles = HelperFile.getFile(outFeaturesFolder).listFiles(new ExtensionFileFilter(Extensions.LABEL_EXT));
        for (File song : firstPassFeatureFiles) {
            ChordStructure chordStructure = new ChordStructure(song.getPath());
            aLattice = new Lattice(chordStructure);
            aLattice.storeInFile(String.format("%s/%s%s", extractedFeaturesDir, chordStructure.getSongName(), Extensions.LATTICE_SEC_PASS_EXT));
        }


        //And now perform final lattice rescoring
        //Creating list of lattices
        String fileList = String.format("%s/%s", tempDirPath, "latticeList2ndPass.txt");
        createFileList(extractedFeaturesDir, fileList, new ExtensionFileFilter(new String[]{Extensions.LATTICE_SEC_PASS_EXT}), false);
        List<String> latticeFilePathList = HelperFile.readTokensFromTextFile(fileList, 1);

        //Assign Factored Language Model Weights
        logger.info("Assign Language Model Weights for generated lattices");
        String outLattice;
        File oldLattice, newLattice;
        for (String inLattice : latticeFilePathList) {
            outLattice = inLattice + "_";
            logger.debug("processing lattice " + inLattice);
            command = "lattice-tool" + EXECUTABLE_EXTENSION + " -debug 0 -htk-logbase 2.71828 -read-htk -write-htk -no-nulls -no-htk-nulls -in-lattice " + inLattice + " -out-lattice " + outLattice + " -lm " + lmDir + File.separator + LMModelStandardVersion + " -order " + execParams.latticeRescoringOrder;

            Helper.execCmd(command);

            oldLattice = HelperFile.getFile(inLattice);
            newLattice = HelperFile.getFile(outLattice);
            oldLattice.delete();
            newLattice.renameTo(oldLattice);
        }


        //Rescoring lattices, applying different lm, ac weights and wip
        logger.info("Rescoring lattices...");
        float lmWeight;
        float acWeight;
        float wip;


        for (int i = 0; i < execParams._lmWeights.length; i++) {
            for (int j = 0; j < execParams._acWeights.length; j++) {
                for (int k = 0; k < execParams._wips.length; k++) {
                    lmWeight = execParams._lmWeights[i];
                    acWeight = execParams._acWeights[j];
                    wip = execParams._wips[k];

                    logger.info("LM " + lmWeight + "; AC " + acWeight + "WIP " + wip);
                    logger.info("--------------------------------------------------");

                    //TODO refactor
                    String resultsDirName = String.format("lmWeight_%3.2f%sacWeight_%3.2f%swip_%3.2f", lmWeight, FIELD_SEPARATOR, acWeight, FIELD_SEPARATOR, wip);
                    recognizedFolder = resultsDir + File.separator + resultsDirName;
//                    LabelsManager.recognizedFolder_compare = recognizedFolder; //This is done in order to estimate the advantages of LM
                    String outRescoredFile = tempDirPath + File.separator + "out_" + resultsDirName;

                    try {
                        BufferedWriter writer = new BufferedWriter(new FileWriter(outRescoredFile));
                        command = "lattice-tool" + EXECUTABLE_EXTENSION + " -debug 0 -htk-logbase 2.71828 -in-lattice-list " + fileList + " -read-htk -htk-lmscale " + lmWeight + " -htk-wdpenalty " + wip + " -htk-acscale " + acWeight + " -nbest-viterbi -viterbi-decode -output-ctm";
                        Helper.execCmd(command, writer, false);
                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    //Parsing and evaluating results
                    logger.info("Parsing results and evaluating");
                     finalEvaluation(outRescoredFile, recognizedFolder);

                }
            }
        }

    }


    @Override
    protected void createGrammarFile(boolean isOnlyOneChord) {
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

    @Override
    protected void hvite(String trainedModelsDir, String hmmFolder, float penalty, String decodedOutPath) {
        String command = String.format("HVite%s -o N -C %s ", EXECUTABLE_EXTENSION, configPath);
        for (int i = 0; i < Configuration.NUMBER_OF_SEMITONES_IN_OCTAVE; i++) {
            command = command + String.format(" -H %s/%s/%s ", trainedModelsDir, hmmFolder + "_" + gaussianNumber, hmmDefs + i);
        }
        command = command + String.format(" -H %s/%s/%s -T 1 -S %s -i %s -n 4 4 -w %s -p %5.2f %s %s",
                trainedModelsDir, hmmFolder + "_" + gaussianNumber, macros, featureFileListTest, decodedOutPath,
                netFilePath, penalty, dictFilePath, wordListTestPath);
        Helper.execCmd(command);
    }

    protected void finalEvaluation(String outRescoredFile, String recognizedFolder){
        ChordHTKParserFromLatticeChordSynchronous parserFinal = new ChordHTKParserFromLatticeChordSynchronous(outRescoredFile, recognizedFolder);
        parserFinal.run();
    }

}
