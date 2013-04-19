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

package org.mart.crs.management.features.extractor.chroma;

import org.mart.crs.core.pcp.PCP;
import org.mart.crs.management.features.CoreElementsInitializer;
import org.mart.crs.management.features.extractor.jni.JNIChromaExtractor;

/**
 * @version 1.0 7/29/12 7:47 PM
 * @author: Hut
 */
public class TrebleJNI extends SpectrumBased{

    @Override
    protected PCP producePCP(float[] samples, double refFrequency) {
        JNIChromaExtractor extractor = new JNIChromaExtractor();
        float[][] pcpData = extractor.getFeaturesInMartFormatTreble(this.audioReader.getFilePath(), execParams.samplingRate, execParams.windowLength, execParams.overlapping);
        PCP pcp = CoreElementsInitializer.initializeBassPCPWithExecParamsData(PCP.BASIC_ALG, refFrequency, execParams);
        pcp.initMatrixData(pcpData, false, execParams.windowLength * execParams.overlapping);
        return pcp;
    }
}
