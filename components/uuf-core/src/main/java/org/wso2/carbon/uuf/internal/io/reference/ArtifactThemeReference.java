/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.uuf.internal.io.reference;

import org.wso2.carbon.uuf.api.reference.FileReference;
import org.wso2.carbon.uuf.api.reference.ThemeReference;
import org.wso2.carbon.uuf.internal.exception.FileOperationException;

import java.nio.file.Files;
import java.nio.file.Path;

public class ArtifactThemeReference implements ThemeReference {

    private final Path themeDirectory;
    private final ArtifactAppReference appReference;

    public ArtifactThemeReference(Path themeDirectory, ArtifactAppReference appReference) {
        this.themeDirectory = themeDirectory;
        this.appReference = appReference;
    }

    @Override
    public String getName() {
        Path fileName = themeDirectory.getFileName(); // Name of the theme is the name of the directory.
        return (fileName == null) ? "" : fileName.toString();
    }

    @Override
    public FileReference getConfiguration() {
        Path themeConfig = themeDirectory.resolve(FILE_NAME_CONFIGURATION);
        if (Files.exists(themeConfig)) {
            return new ArtifactFileReference(themeConfig, appReference);
        } else {
            throw new FileOperationException(
                    "Cannot find theme's configuration file '" + FILE_NAME_CONFIGURATION + "' of theme '" + getName() +
                            "' in app '" + appReference.getPath() + "'.");
        }
    }

    @Override
    public String getPath() {
        return themeDirectory.toString();
    }
}
