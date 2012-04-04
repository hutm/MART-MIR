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

package org.mart.crs.config;

import org.apache.log4j.Logger;
import org.mart.crs.exec.operation.OperationType;
import org.mart.crs.logging.CRSLogger;
import org.mart.crs.management.config.Configuration;
import org.mart.crs.utils.ReflectUtils;
import org.mart.crs.utils.helper.HelperFile;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * User: Hut
 * Date: 11.05.2008
 * Class containing all constants
 */
public class Settings {

    protected static Logger logger = CRSLogger.getLogger(Settings.class);

    private static Properties properties;


    public static String scenario;
    public static int numberOfFolds;
    public static String operationDomain;
    public static OperationType operationType;
    public static int[] stagesToRun;
    public static int threadsNumberForFeatureExtraction;                  // number of simultaneous feature extraction threads
    public static int NumberOfParallelThreadsForConfigListenerService;    //For ConfigListener - number of parallel threads


    public static boolean isToDeleteTrainFeaturesAfterTraining;

    public static String[] chordDictionary;

    public static int numberOfTestMaterial;


    public static String labelsGroundTruthDir;
    public static String chordLabelsGroundTruthDir;
    public static String beatLabelsGroundTruthDir;
    public static String onsetLabelsGroundTruthDir;
    public static String keyLabelsGroundTruthDir;
    public static String tuningsGroudTruthFilePath;
    public static String chordRecognizedDirectory;     //Needed for key detection


    public static boolean isToUseLMs;
    public static boolean isToUseBigramDuringHVite;


    public static boolean isMIREX;
    public static boolean isQUAERO;


    //-----------------------initialization part---------------------------

    static {
        initialize();
    }

    private static boolean initialized;



    public static void initialize() {
        if(initialized){
            return;
        }
        String osName = System.getProperty("os.name");
        if (osName.contains("Windows")) {
            EXECUTABLE_EXTENSION = ".exe";
            DYNAMIC_LIBRARY_EXTENSION = ".dll";
            SCRIPT_EXTENSION = ".bat";
        } else if (osName.contains("Mac")) {
            EXECUTABLE_EXTENSION = "";
            DYNAMIC_LIBRARY_EXTENSION = ".so";
            SCRIPT_EXTENSION = ".sh";
        } else if (osName.contains("Linux")) {
            EXECUTABLE_EXTENSION = "";
            DYNAMIC_LIBRARY_EXTENSION = ".so";
            SCRIPT_EXTENSION = ".sh";
        }
        if (HelperFile.getFile(ConfigSettings.CONFIG_FILE_PATH).exists()) {
            readProperties(ConfigSettings.CONFIG_FILE_PATH);
        } else {
            throw new IllegalArgumentException(String.format("Configuration file '%s' is not found", ConfigSettings.CONFIG_FILE_PATH));
        }
        setOperationType(OperationType.fromString(Settings.operationDomain));
        operationType.getOperationDomain();
        initialized = true;
    }

    public static void readProperties(String fileName) {
        properties = new Properties();
        try {
            properties.load(new FileInputStream(fileName));
            ReflectUtils.fillInVariables(Settings.class, properties);
            Configuration.chordDictionary = Settings.chordDictionary;

            ExecParams._initialExecParameters = new ExecParams();
            ReflectUtils.fillInVariables(ExecParams._initialExecParameters, properties);
        } catch (IOException e) {
            logger.warn("Error while reading settings file: " + fileName, e);
        }
    }

    public static void readProperties(StringBuffer stringBuffer) {
        //At first read properties
        properties = new Properties();
        try {
            properties.load(new ByteArrayInputStream(stringBuffer.toString().getBytes("UTF-8")));
            ReflectUtils.fillInVariables(Settings.class, properties);
        } catch (IOException e) {
            logger.warn("Error while reading settings file from StringBuffer", e);
        }
    }


    public static void setOperationType(OperationType operationType_) {
        operationType = operationType_;
    }

    //------------------------------------------------------------------------------------

    public static String DYNAMIC_LIBRARY_EXTENSION;
    public static String EXECUTABLE_EXTENSION;
    public static String SCRIPT_EXTENSION;

    public static final String FIELD_SEPARATOR = "#";


    public static boolean isToUseRefFreq = true;
    public static boolean forceReExtractRefFreq = false;


}
