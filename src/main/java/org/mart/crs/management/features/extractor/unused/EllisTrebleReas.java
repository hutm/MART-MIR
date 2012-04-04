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

package org.mart.crs.management.features.extractor.unused;

/**
 * @version 1.0 20-Sep-2010 00:58:42
 * @author: Hut
 */
public class EllisTrebleReas extends EllisBassReas {


    @Override
    public void extractGlobalFeatures(double refFrequency) {
        float[] samples = audioReader.getSamples();
        globalVectors.add(getDataFromMatlab(samples, false));
    }

}
