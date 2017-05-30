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

import org.apache.commons.io.FilenameUtils;
import org.wso2.carbon.uuf.api.reference.FileReference;
import org.wso2.carbon.uuf.internal.exception.FileOperationException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class ArtifactFileReference implements FileReference {

    private final Path path;
    private final ArtifactAppReference appReference;

    public ArtifactFileReference(Path path, ArtifactAppReference appReference) {
        this.path = path;
        this.appReference = appReference;
    }

    @Override
    public String getName() {
        Path fileName = path.getFileName();
        return (fileName == null) ? "" : fileName.toString();
    }

    @Override
    public String getExtension() {
        return FilenameUtils.getExtension(getName());
    }

    @Override
    public String getNameWithoutExtension() {
        return FilenameUtils.removeExtension(getName());
    }

    @Override
    public String getContent() {
        try {
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new FileOperationException("Cannot read content of file '" + path + "'.", e);
        }
    }

    @Override
    public String getRelativePath() {
        // Here 'appReference.getDirectory().getParent()' never null.
        return appReference.getDirectory().getParent().relativize(path).toString();
    }

    @Override
    public String getAbsolutePath() {
        try {
            return path.toRealPath().toString();
        } catch (IOException e) {
            return path.toAbsolutePath().toString();
        }
    }

    @Override
    public Optional<FileReference> getSibling(String name) {
        Path sibling = path.resolveSibling(name);
        return Files.exists(sibling) ? Optional.of(new ArtifactFileReference(sibling, appReference)) :
                Optional.<FileReference>empty();
    }
}
