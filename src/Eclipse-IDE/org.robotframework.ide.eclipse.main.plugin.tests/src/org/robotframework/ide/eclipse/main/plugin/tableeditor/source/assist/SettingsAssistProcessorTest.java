/*
 * Copyright 2016 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.tableeditor.source.assist;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.robotframework.ide.eclipse.main.plugin.tableeditor.source.assist.Assistant.createAssistant;
import static org.robotframework.ide.eclipse.main.plugin.tableeditor.source.assist.Proposals.applyToDocument;
import static org.robotframework.ide.eclipse.main.plugin.tableeditor.source.assist.Proposals.proposalWithImage;
import static org.robotframework.red.junit.jupiter.ProjectExtension.createFile;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.robotframework.ide.eclipse.main.plugin.RedImages;
import org.robotframework.ide.eclipse.main.plugin.mockdocument.Document;
import org.robotframework.ide.eclipse.main.plugin.mockmodel.RobotSuiteFileCreator;
import org.robotframework.ide.eclipse.main.plugin.model.RobotModel;
import org.robotframework.ide.eclipse.main.plugin.model.RobotSuiteFile;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.source.SuiteSourcePartitionScanner;
import org.robotframework.red.graphics.ImagesManager;
import org.robotframework.red.junit.jupiter.Project;
import org.robotframework.red.junit.jupiter.ProjectExtension;

@ExtendWith(ProjectExtension.class)
public class SettingsAssistProcessorTest {

    @Project
    static IProject project;

    @Test
    public void settingsProcessorIsValidOnlyForVariablesSection() {
        final SettingsAssistProcessor processor = new SettingsAssistProcessor(createAssistant((RobotSuiteFile) null));
        assertThat(processor.getApplicableContentTypes()).containsOnly(SuiteSourcePartitionScanner.TEST_CASES_SECTION,
                SuiteSourcePartitionScanner.TASKS_SECTION, SuiteSourcePartitionScanner.KEYWORDS_SECTION);
    }

    @Test
    public void settingsProcessorProcessorHasTitleDefined() {
        final SettingsAssistProcessor processor = new SettingsAssistProcessor(createAssistant((RobotSuiteFile) null));
        assertThat(processor.getProposalsTitle()).isNotNull().isNotEmpty();
    }

    @Test
    public void noProposalsAreProvided_whenInVariablesSection() throws Exception {
        final int offset = 28;
        final List<String> lines = newArrayList("*** Variables ***", "${var}  123");

        final ITextViewer viewer = mock(ITextViewer.class);
        final IDocument document = spy(new Document(lines));
        when(viewer.getDocument()).thenReturn(document);
        when(document.getContentType(offset)).thenReturn(SuiteSourcePartitionScanner.VARIABLES_SECTION);

        final RobotSuiteFile model = new RobotSuiteFileCreator().appendLines(lines).build();

        final SettingsAssistProcessor processor = new SettingsAssistProcessor(createAssistant(model));
        final List<? extends ICompletionProposal> proposals = processor.computeProposals(viewer, offset);

        assertThat(proposals).isNull();
    }

    @Test
    public void noProposalsAreProvided_whenInTheFirstCell() throws Exception {
        final int offset = 25;
        final List<String> lines = newArrayList("*** Test Cases ***", "case", "  keyword  argument");

        final IFile file = createFile(project, "1.robot", lines.toArray(new String[0]));

        final ITextViewer viewer = mock(ITextViewer.class);
        final IDocument document = spy(new Document(lines));
        when(viewer.getDocument()).thenReturn(document);
        when(document.getContentType(offset)).thenReturn(SuiteSourcePartitionScanner.TEST_CASES_SECTION);

        final RobotSuiteFile model = new RobotModel().createSuiteFile(file);

        final SettingsAssistProcessor processor = new SettingsAssistProcessor(createAssistant(model));
        final List<? extends ICompletionProposal> proposals = processor.computeProposals(viewer, offset);

        assertThat(proposals).isNull();
    }

    @Test
    public void keywordSettingsProposalsAreProvided_whenInFirstCellOfKeywordSection() throws Exception {
        final int offset = 27;
        final List<String> lines = newArrayList("*** Keywords ***", "keyword", "  ");

        final IFile file = createFile(project, "1.robot", lines.toArray(new String[0]));

        final ITextViewer viewer = mock(ITextViewer.class);
        final IDocument document = spy(new Document(lines));
        when(viewer.getDocument()).thenReturn(document);
        when(document.getContentType(offset)).thenReturn(SuiteSourcePartitionScanner.KEYWORDS_SECTION);

        final RobotSuiteFile model = new RobotModel().createSuiteFile(file);

        final SettingsAssistProcessor processor = new SettingsAssistProcessor(createAssistant(model));
        final List<? extends ICompletionProposal> proposals = processor.computeProposals(viewer, offset);

        assertThat(proposals).hasSize(6).haveExactly(6,
                proposalWithImage(ImagesManager.getImage(RedImages.getRobotSettingImage())));

        assertThat(proposals).extracting(proposal -> applyToDocument(document, proposal)).containsOnly(
                new Document("*** Keywords ***", "keyword", "  [Arguments]  "),
                new Document("*** Keywords ***", "keyword", "  [Documentation]  "),
                new Document("*** Keywords ***", "keyword", "  [Return]  "),
                new Document("*** Keywords ***", "keyword", "  [Tags]  "),
                new Document("*** Keywords ***", "keyword", "  [Teardown]  "),
                new Document("*** Keywords ***", "keyword", "  [Timeout]  "));
    }

    @Test
    public void onlyKeywordSettingsProposalsMatchingInputAreProvided_whenInFirstCellOfSetting() throws Exception {
        final int offset = 29;
        final List<String> lines = newArrayList("*** Keywords ***", "keyword", "  [T");

        final IFile file = createFile(project, "1.robot", lines.toArray(new String[0]));

        final ITextViewer viewer = mock(ITextViewer.class);
        final IDocument document = spy(new Document(lines));
        when(viewer.getDocument()).thenReturn(document);
        when(document.getContentType(offset)).thenReturn(SuiteSourcePartitionScanner.KEYWORDS_SECTION);

        final RobotSuiteFile model = new RobotModel().createSuiteFile(file);

        final SettingsAssistProcessor processor = new SettingsAssistProcessor(createAssistant(model));
        final List<? extends ICompletionProposal> proposals = processor.computeProposals(viewer, offset);

        assertThat(proposals).hasSize(3).haveExactly(3,
                proposalWithImage(ImagesManager.getImage(RedImages.getRobotSettingImage())));

        assertThat(proposals).extracting(proposal -> applyToDocument(document, proposal)).containsOnly(
                new Document("*** Keywords ***", "keyword", "  [Tags]  "),
                new Document("*** Keywords ***", "keyword", "  [Teardown]  "),
                new Document("*** Keywords ***", "keyword", "  [Timeout]  "));
    }

    @Test
    public void testCaseSettingsProposalsAreProvided_whenInFirstCellOfTestCasesSection() throws Exception {
        final int offset = 26;
        final List<String> lines = newArrayList("*** Test Cases ***", "case", "  ");

        final IFile file = createFile(project, "1.robot", lines.toArray(new String[0]));

        final ITextViewer viewer = mock(ITextViewer.class);
        final IDocument document = spy(new Document(lines));
        when(viewer.getDocument()).thenReturn(document);
        when(document.getContentType(offset)).thenReturn(SuiteSourcePartitionScanner.TEST_CASES_SECTION);

        final RobotSuiteFile model = new RobotModel().createSuiteFile(file);

        final SettingsAssistProcessor processor = new SettingsAssistProcessor(createAssistant(model));
        final List<? extends ICompletionProposal> proposals = processor.computeProposals(viewer, offset);

        assertThat(proposals).hasSize(6).haveExactly(6,
                proposalWithImage(ImagesManager.getImage(RedImages.getRobotSettingImage())));

        assertThat(proposals).extracting(proposal -> applyToDocument(document, proposal)).containsOnly(
                new Document("*** Test Cases ***", "case", "  [Setup]  "),
                new Document("*** Test Cases ***", "case", "  [Documentation]  "),
                new Document("*** Test Cases ***", "case", "  [Template]  "),
                new Document("*** Test Cases ***", "case", "  [Tags]  "),
                new Document("*** Test Cases ***", "case", "  [Teardown]  "),
                new Document("*** Test Cases ***", "case", "  [Timeout]  "));
    }

    @Test
    public void onlyTestCaseSettingsProposalsMatchingInputAreProvided_whenInFirstCellOfSetting() throws Exception {
        final int offset = 29;
        final List<String> lines = newArrayList("*** Test Cases ***", "case", "  [Te");

        final IFile file = createFile(project, "1.robot", lines.toArray(new String[0]));

        final ITextViewer viewer = mock(ITextViewer.class);
        final IDocument document = spy(new Document(lines));
        when(viewer.getDocument()).thenReturn(document);
        when(document.getContentType(offset)).thenReturn(SuiteSourcePartitionScanner.TEST_CASES_SECTION);

        final RobotSuiteFile model = new RobotModel().createSuiteFile(file);

        final SettingsAssistProcessor processor = new SettingsAssistProcessor(createAssistant(model));
        final List<? extends ICompletionProposal> proposals = processor.computeProposals(viewer, offset);

        assertThat(proposals).hasSize(2).haveExactly(2,
                proposalWithImage(ImagesManager.getImage(RedImages.getRobotSettingImage())));

        assertThat(proposals).extracting(proposal -> applyToDocument(document, proposal)).containsOnly(
                new Document("*** Test Cases ***", "case", "  [Template]  "),
                new Document("*** Test Cases ***", "case", "  [Teardown]  "));
    }
}
