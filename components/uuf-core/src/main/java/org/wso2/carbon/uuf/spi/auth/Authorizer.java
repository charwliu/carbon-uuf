/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.uuf.spi.auth;

import org.wso2.carbon.uuf.api.auth.Permission;
import org.wso2.carbon.uuf.api.auth.User;
import org.wso2.carbon.uuf.api.exception.AuthorizationException;

/**
 * Evaluates permissions for users.
 * <p>
 * Please make note to specify the authorizer class name in the <tt>app.yaml</tt> configuration file under the
 * <tt>authorizer</tt> key in order for the implemented authorizer to be used in the app.
 * <p>
 * eg:
 * authorizer: "org.wso2.carbon.uuf.sample.featuresapp.bundle.api.auth.DemoAuthorizer"
 * <p>
 * The logic for persisting and retrieving permissions are expected to be implemented by the web developer. The
 * {@code hasPermission(User user, Permission permission)} method will be called internally by the UUF framework when
 * it is required to evaluate permissions.
 *
 * @since 1.0.0
 */
public interface Authorizer {

    /**
     * Checks whether the given user has permission to a resource.
     *
     * @param user       user to be checked
     * @param permission permission to be checked
     * @return {@code true} if the user has the permission, otherwise {@code false}
     * @throws AuthorizationException if an error occurs when checking permission
     */
    boolean hasPermission(User user, Permission permission) throws AuthorizationException;
}
