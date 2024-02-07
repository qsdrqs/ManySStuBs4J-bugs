/**
 * Copyright 2014 NAVER Corp.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.navercorp.pinpoint.plugin.json.lib;

import static com.navercorp.pinpoint.common.trace.HistogramSchema.*;
import static com.navercorp.pinpoint.common.trace.ServiceTypeProperty.*;
import com.navercorp.pinpoint.common.trace.AnnotationKey;

import com.navercorp.pinpoint.common.trace.ServiceType;

/**
 * @author Sangyoon Lee
 *
 */
public interface JsonLibConstants {
    public static final ServiceType SERVICE_TYPE = ServiceType.of(5012, "JSON-LIB", NORMAL_SCHEMA);
    public static final AnnotationKey JSON_LIB_ANNOTATION_KEY_JSON_LENGTH = new AnnotationKey(9002, "JSON_LENGTH");
}
