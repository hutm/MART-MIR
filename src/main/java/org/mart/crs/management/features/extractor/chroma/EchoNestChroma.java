package org.mart.crs.management.features.extractor.chroma;

import org.mart.crs.core.pcp.PCP;
import org.mart.crs.management.features.extractor.FeaturesExtractorHTK;

/**
 * @version 1.0 11/20/12 6:15 PM
 * @author: Hut
 */
public class EchoNestChroma extends SpectrumBased {

    @Override
    public void extractGlobalFeatures(double refFrequency) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getVectorSize() {
        return 12;
    }

    @Override
    protected PCP producePCP(float[] samples, double refFrequency) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
