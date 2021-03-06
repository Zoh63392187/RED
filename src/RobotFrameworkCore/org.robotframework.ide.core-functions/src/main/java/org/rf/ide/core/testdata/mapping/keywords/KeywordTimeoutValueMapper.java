/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.testdata.mapping.keywords;

import java.util.List;
import java.util.Stack;

import org.rf.ide.core.testdata.mapping.table.IParsingMapper;
import org.rf.ide.core.testdata.mapping.table.ParsingStateHelper;
import org.rf.ide.core.testdata.model.FilePosition;
import org.rf.ide.core.testdata.model.RobotFileOutput;
import org.rf.ide.core.testdata.model.table.KeywordTable;
import org.rf.ide.core.testdata.model.table.LocalSetting;
import org.rf.ide.core.testdata.model.table.keywords.UserKeyword;
import org.rf.ide.core.testdata.text.read.ParsingState;
import org.rf.ide.core.testdata.text.read.RobotLine;
import org.rf.ide.core.testdata.text.read.recognizer.RobotToken;
import org.rf.ide.core.testdata.text.read.recognizer.RobotTokenType;

public class KeywordTimeoutValueMapper implements IParsingMapper {

    private final ParsingStateHelper utility = new ParsingStateHelper();

    @Override
    public boolean checkIfCanBeMapped(final RobotFileOutput robotFileOutput, final RobotLine currentLine,
            final RobotToken rt, final String text, final Stack<ParsingState> processingState) {

        if (utility.getCurrentState(processingState) == ParsingState.KEYWORD_SETTING_TIMEOUT) {
            final List<UserKeyword> keywords = robotFileOutput.getFileModel().getKeywordTable().getKeywords();
            final List<LocalSetting<UserKeyword>> timeouts = keywords.get(keywords.size() - 1).getTimeouts();
            return !hasTimeoutAlready(timeouts);
        }
        return false;
    }

    static boolean hasTimeoutAlready(final List<LocalSetting<UserKeyword>> timeouts) {
        return !timeouts.isEmpty()
                && timeouts.get(timeouts.size() - 1).getToken(RobotTokenType.TEST_CASE_SETTING_TIMEOUT_VALUE) != null;
    }

    @Override
    public RobotToken map(final RobotLine currentLine, final Stack<ParsingState> processingState,
            final RobotFileOutput robotFileOutput, final RobotToken rt, final FilePosition fp, final String text) {

        rt.setText(text);

        final KeywordTable keywordTable = robotFileOutput.getFileModel().getKeywordTable();
        final List<UserKeyword> keywords = keywordTable.getKeywords();
        final UserKeyword keyword = keywords.get(keywords.size() - 1);
        final List<LocalSetting<UserKeyword>> timeouts = keyword.getTimeouts();
        if (!timeouts.isEmpty()) {
            timeouts.get(timeouts.size() - 1).addToken(rt);
        }

        processingState.push(ParsingState.KEYWORD_SETTING_TIMEOUT_VALUE);
        return rt;
    }
}
