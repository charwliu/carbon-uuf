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

package org.wso2.carbon.uuf.internal.exception;

import org.wso2.carbon.uuf.exception.UUFRuntimeException;

public class HttpErrorException extends UUFRuntimeException {

    private final int httpStatusCode;

    public HttpErrorException(int httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }

    public HttpErrorException(int httpStatusCode, String message) {
        super(message);
        this.httpStatusCode = httpStatusCode;
    }

    public HttpErrorException(int httpStatusCode, String message, Throwable cause) {
        super(message, cause);
        this.httpStatusCode = httpStatusCode;
    }

    public int getHttpStatusCode() {
        return this.httpStatusCode;
    }
}
