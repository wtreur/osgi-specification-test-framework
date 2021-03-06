/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package net.luminis.osgitest.testhelper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to provide OSGi-specification information for multiple OSGi-versions to the JUnit test methods
 *
 * If this annotation is combined with a separate OSGiSpec directly annotated on a method (instead of embedded in this OSGiVersionSpecs annoation)
 * the separate OSGiSpec annotation will be ignored and this list is used.
 *
 * @see OSGiSpec
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OSGiVersionSpecs {
    /**
     * A list of OSGiSpec annotations.
     *
     * @return
     */
    OSGiSpec[] value();

}
