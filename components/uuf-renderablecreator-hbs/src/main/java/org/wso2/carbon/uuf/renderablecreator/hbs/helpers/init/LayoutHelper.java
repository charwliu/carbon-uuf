/*
 *  Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.carbon.uuf.renderablecreator.hbs.helpers.init;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import org.wso2.carbon.uuf.renderablecreator.hbs.exception.HbsRenderingException;
import org.wso2.carbon.uuf.renderablecreator.hbs.internal.HbsPreprocessor;

import java.io.IOException;

public class LayoutHelper implements Helper<String> {

    public static final String HELPER_NAME = "layout";

    @Override
    public CharSequence apply(String layoutName, Options options) throws IOException {
        if ((layoutName == null) || layoutName.isEmpty()) {
            throw new HbsRenderingException("Layout name cannot be null or empty.");
        }

        Object currentLayout = options.data(HbsPreprocessor.DATA_KEY_CURRENT_LAYOUT);
        if (currentLayout != null) {
            throw new HbsRenderingException("Cannot set layout '" + layoutName + "' to this page because layout '" +
                                                    currentLayout + "' is already set.");
        }
        options.data(HbsPreprocessor.DATA_KEY_CURRENT_LAYOUT, layoutName);
        return "";
    }
}
