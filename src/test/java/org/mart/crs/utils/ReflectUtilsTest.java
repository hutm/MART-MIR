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

package org.mart.crs.utils;

import org.mart.crs.config.ConfigSettings;
import org.mart.crs.config.ExecParams;
import org.mart.crs.exec.scenario.BatchParameter;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/**
 * ReflectUtils Tester.
 *
 * @author Hut
 * @version 1.0
 * @since <pre>02/15/2011</pre>
 */
public class ReflectUtilsTest {



    /**
     * Method: setVariableValue(Class classOrObject, String fieldName, String unparsedValue)
     */
    @Test
    public void testSetVariableValue() throws Exception {
        ConfigSettings.CONFIG_FILE_PATH = this.getClass().getResource("/cfg/configBeatsProto.cfg").getPath();
        ExecParams execParams = new ExecParams();
        List<BatchParameter> fields = ReflectUtils.getSettingsVariables(execParams, "_TRAIN_FEATURES_");
        String fieldName = fields.get(0).getField().getName();
        execParams.samplingRate = 11026;
        Assert.assertEquals(ReflectUtils.getVariableValue(execParams, "samplingRate"), 11026.0f);
    }


}
