/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.testdata.model.mapping.hashComment.tableUserKeyword;

import java.util.List;

import org.rf.ide.core.testdata.model.RobotFile;
import org.rf.ide.core.testdata.model.mapping.IHashCommentMapper;
import org.rf.ide.core.testdata.model.table.userKeywords.KeywordTeardown;
import org.rf.ide.core.testdata.model.table.userKeywords.UserKeyword;
import org.rf.ide.core.testdata.text.read.ParsingState;
import org.rf.ide.core.testdata.text.read.recognizer.RobotToken;


public class UserKeywordSettingTeardownCommentMapper implements
        IHashCommentMapper {

    @Override
    public boolean isApplicable(ParsingState state) {
        return (state == ParsingState.KEYWORD_SETTING_TEARDOWN
                || state == ParsingState.KEYWORD_SETTING_TEARDOWN_KEYWORD || state == ParsingState.KEYWORD_SETTING_TEARDOWN_KEYWORD_ARGUMENT);
    }


    @Override
    public void map(RobotToken rt, ParsingState currentState,
            RobotFile fileModel) {
        List<UserKeyword> keywords = fileModel.getKeywordTable().getKeywords();
        UserKeyword keyword = keywords.get(keywords.size() - 1);

        List<KeywordTeardown> teardowns = keyword.getTeardowns();
        KeywordTeardown keywordTeardown = teardowns.get(teardowns.size() - 1);
        keywordTeardown.addCommentPart(rt);
    }
}
