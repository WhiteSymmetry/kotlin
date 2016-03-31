/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tools.klint.checks;

import com.android.tools.klint.detector.api.Category;
import com.android.tools.klint.detector.api.Detector;
import com.android.tools.klint.detector.api.Implementation;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.Scope;
import com.android.tools.klint.detector.api.Severity;

import java.util.Collections;
import java.util.List;

import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.check.UastAndroidContext;
import org.jetbrains.uast.check.UastScanner;

/**
 * Looks for invocations of android.webkit.WebSettings.setJavaScriptEnabled.
 */
public class SetJavaScriptEnabledDetector extends Detector implements UastScanner {
    /** Invocations of setJavaScriptEnabled */
    public static final Issue ISSUE = Issue.create("SetJavaScriptEnabled", //$NON-NLS-1$
            "Using `setJavaScriptEnabled`",

            "Your code should not invoke `setJavaScriptEnabled` if you are not sure that " +
            "your app really requires JavaScript support.",

            Category.SECURITY,
            6,
            Severity.WARNING,
            new Implementation(
                    SetJavaScriptEnabledDetector.class,
                    Scope.SOURCE_FILE_SCOPE))
            .addMoreInfo(
            "http://developer.android.com/guide/practices/security.html"); //$NON-NLS-1$

    /** Constructs a new {@link SetJavaScriptEnabledDetector} check */
    public SetJavaScriptEnabledDetector() {
    }

    // ---- Implements UastScanner ----

    @Override
    public void visitCall(UastAndroidContext context, UCallExpression node) {
        if (node.getValueArgumentCount() != 1) {
            return;
        }

        Object value = node.getValueArguments().get(0).evaluate();
        if (value instanceof Boolean && (Boolean) value) {
            context.report(ISSUE, node, context.getLocation(node),
                           "Using `setJavaScriptEnabled` can introduce XSS vulnerabilities " +
                           "into you application, review carefully.");
        }
    }

    @Override
    public List<String> getApplicableFunctionNames() {
        return Collections.singletonList("setJavaScriptEnabled");
    }
}
