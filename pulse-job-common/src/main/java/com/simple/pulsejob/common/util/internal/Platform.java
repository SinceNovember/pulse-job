/*
 * Copyright (c) 2015 The Jupiter Project
 *
 * Licensed under the Apache License, version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.simple.pulsejob.common.util.internal;

import java.util.Locale;
import com.simple.pulsejob.common.util.SystemPropertyUtil;

/**
 * jupiter
 * org.jupiter.common.util.internal
 *
 * @author jiachun.fjc
 */
public class Platform {


    private static final boolean IS_WINDOWS = isWindows0();

    /**
     * Return {@code true} if the JVM is running on Windows
     */
    public static boolean isWindows() {
        return IS_WINDOWS;
    }

    private static boolean isWindows0() {
        return SystemPropertyUtil.get("os.name", "").toLowerCase(Locale.US).contains("win");
    }
}
