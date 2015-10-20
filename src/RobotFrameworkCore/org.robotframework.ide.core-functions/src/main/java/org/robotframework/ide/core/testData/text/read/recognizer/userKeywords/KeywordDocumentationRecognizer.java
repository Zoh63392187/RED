/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.core.testData.text.read.recognizer.userKeywords;

import org.robotframework.ide.core.testData.text.read.recognizer.AExecutableElementSettingsRecognizer;
import org.robotframework.ide.core.testData.text.read.recognizer.ATokenRecognizer;
import org.robotframework.ide.core.testData.text.read.recognizer.RobotTokenType;


public class KeywordDocumentationRecognizer extends
        AExecutableElementSettingsRecognizer {

    public KeywordDocumentationRecognizer() {
        super(RobotTokenType.KEYWORD_SETTING_DOCUMENTATION);
    }


    @Override
    public ATokenRecognizer newInstance() {
        return new KeywordDocumentationRecognizer();
    }
}
