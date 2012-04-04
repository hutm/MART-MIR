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

package org.mart.crs.management.features.extractor.chroma;

import org.mart.crs.core.AudioReader;
import org.mart.crs.core.pcp.PCP;
import org.mart.crs.management.config.Configuration;
import org.mart.crs.management.features.extractor.FeaturesExtractorHTK;

/**
 * PCP spectrum based feature
 *
 * @version 1.0 Mar 3, 2010 2:05:45 PM
 * @author: Maksim Khadkevich
 */
public abstract class SpectrumBased extends FeaturesExtractorHTK {


    public void extractGlobalFeatures(double refFrequency) {
        float[] samples = audioReader.getSamples();
        globalVectors.add(producePCP(samples, refFrequency).getPCP());
    }

    @Override
    public int getVectorSize() {
        return Configuration.NUMBER_OF_SEMITONES_IN_OCTAVE;
    }


    @Override
    public void initialize(String songFilePath) {
        super.initialize(songFilePath);
        this.audioReader = new AudioReader(songFilePath, execParams.samplingRate);
    }


    public void initialize(AudioReader audioReader) {
        super.initialize(audioReader);
    }


    protected abstract PCP producePCP(float[] samples, double refFrequency);


    @Override
    public float getDuration() {
        return this.audioReader.getDuration();
    }
}
