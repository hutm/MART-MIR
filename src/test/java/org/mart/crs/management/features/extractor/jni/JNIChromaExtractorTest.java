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

package org.mart.crs.management.features.extractor.jni;

import org.testng.annotations.Test;

/**
 * @version 1.0 7/29/12 7:24 PM
 * @author: Hut
 */
public class JNIChromaExtractorTest {

    /**
     * Needs compiled native library
     */
    @Test(enabled = false)
    public void testJNI() {
        JNIChromaExtractor extractor = new JNIChromaExtractor();
        System.out.println("Starting java jni ...");
        String filePath = this.getClass().getResource("/audio/1.mp3").getPath();
        float[] pcp = extractor.getPCP(filePath, 11025f, 4096, 0.50f);
        System.out.println(pcp[0]);
    }

    @Test(enabled = false)
    public void testGetFeaturesInMartFormat() {
        JNIChromaExtractor extractor = new JNIChromaExtractor();
        String filePath = this.getClass().getResource("/audio/1.mp3").getPath();
        float[][] pcp = extractor.getFeaturesInMartFormat(filePath, 11025f, 4096, 0.50f);
        System.out.println(pcp[0]);
    }



}
