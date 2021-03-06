/*
 * Copyright 2016 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.tableeditor.assist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.robotframework.red.junit.jupiter.ProjectExtension.createFile;
import static org.robotframework.red.junit.jupiter.ProjectExtension.getFile;

import java.util.AbstractMap.SimpleEntry;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IProject;
import org.eclipse.nebula.widgets.nattable.data.IRowDataProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.rf.ide.core.libraries.LibraryDescriptor;
import org.rf.ide.core.libraries.LibrarySpecification;
import org.robotframework.ide.eclipse.main.plugin.RedPlugin;
import org.robotframework.ide.eclipse.main.plugin.RedPreferences;
import org.robotframework.ide.eclipse.main.plugin.model.RobotKeywordCall;
import org.robotframework.ide.eclipse.main.plugin.model.RobotKeywordsSection;
import org.robotframework.ide.eclipse.main.plugin.model.RobotModel;
import org.robotframework.ide.eclipse.main.plugin.model.RobotProject;
import org.robotframework.ide.eclipse.main.plugin.model.RobotSettingsSection;
import org.robotframework.ide.eclipse.main.plugin.model.RobotSuiteFile;
import org.robotframework.ide.eclipse.main.plugin.project.editor.libraries.Libraries;
import org.robotframework.red.jface.assist.AssistantContext;
import org.robotframework.red.jface.assist.RedContentProposal;
import org.robotframework.red.jface.assist.RedTextContentAdapter.SubstituteTextModificationStrategy;
import org.robotframework.red.junit.jupiter.BooleanPreference;
import org.robotframework.red.junit.jupiter.FreshShell;
import org.robotframework.red.junit.jupiter.FreshShellExtension;
import org.robotframework.red.junit.jupiter.PreferencesExtension;
import org.robotframework.red.junit.jupiter.Project;
import org.robotframework.red.junit.jupiter.ProjectExtension;
import org.robotframework.red.nattable.edit.AssistanceSupport.NatTableAssistantContext;

@ExtendWith({ ProjectExtension.class, PreferencesExtension.class, FreshShellExtension.class })
public class KeywordProposalsInSettingsProviderTest {


    @Project
    static IProject project;

    @FreshShell
    Shell shell;

    private static RobotModel robotModel;

    @BeforeAll
    public static void beforeSuite() throws Exception {
        robotModel = RedPlugin.getModelManager().getModel();

        createFile(project, "kw_based_settings.robot",
                "*** Settings ***",
                "Suite Setup",
                "Suite Teardown",
                "Test Setup",
                "Test Teardown",
                "Test Template");
        createFile(project, "non_kw_based_settings.robot",
                "*** Settings ***",
                "Library",
                "Resource",
                "Variables",
                "Metadata",
                "Test Timeout",
                "Force Tags",
                "Default Tags");
        createFile(project, "kw_based_settings_with_lib_import.robot",
                "*** Settings ***",
                "Suite Setup",
                "Suite Teardown",
                "Test Setup",
                "Test Teardown",
                "Test Template",
                "Library  LibImported");
        createFile(project, "kw_based_settings_with_keywords_with_args.robot",
                "*** Settings ***",
                "Suite Setup",
                "Suite Teardown",
                "Test Setup",
                "Test Teardown",
                "Test Template",
                "*** Keywords ***",
                "kw_no_args",
                "kw_with_args",
                "  [Arguments]  ${arg1}  ${arg2}");
        createFile(project, "kw_based_settings_with_keywords_with_embedded_args.robot",
                "*** Settings ***",
                "Suite Setup",
                "Suite Teardown",
                "Test Setup",
                "Test Teardown",
                "Test Template",
                "*** Keywords ***",
                "kw_no_arg",
                "kw_with_${arg}");
    }

    @AfterAll
    public static void afterSuite() {
        RedPlugin.getModelManager().dispose();
    }

    @Test
    public void proposalsShouldNotBeShown_whenColumnIsDifferentThanSecond() {
        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(getFile(project, "kw_based_settings.robot"));
        final List<RobotKeywordCall> settings = suiteFile.findSection(RobotSettingsSection.class).get().getChildren();
        final RobotKeywordsSection kwSection = (RobotKeywordsSection) suiteFile
                .createRobotSection(RobotKeywordsSection.SECTION_NAME);
        kwSection.createChild(0, "keyword");

        final IRowDataProvider<Object> dataProvider = prepareSettingsProvider(settings);
        final KeywordProposalsInSettingsProvider provider = new KeywordProposalsInSettingsProvider(suiteFile,
                dataProvider);

        for (int column = 0; column < 10; column++) {
            for (int row = 0; row < settings.size(); row++) {
                final AssistantContext context = new NatTableAssistantContext(column, row);
                if (column == 1) {
                    assertThat(provider.shouldShowProposals(context)).isTrue();
                } else {
                    assertThat(provider.shouldShowProposals(context)).isFalse();
                }
            }
        }
    }

    @Test
    public void proposalsShouldNotBeShown_whenSettingIsNotKeywordBased() throws Exception {
        final RobotSuiteFile suiteFile = robotModel
                .createSuiteFile(getFile(project, "non_kw_based_settings.robot"));
        final List<RobotKeywordCall> settings = suiteFile.findSection(RobotSettingsSection.class).get().getChildren();
        final RobotKeywordsSection kwSection = (RobotKeywordsSection) suiteFile
                .createRobotSection(RobotKeywordsSection.SECTION_NAME);
        kwSection.createChild(0, "keyword");

        final IRowDataProvider<Object> dataProvider = prepareSettingsProvider(settings);
        final KeywordProposalsInSettingsProvider provider = new KeywordProposalsInSettingsProvider(suiteFile,
                dataProvider);

        for (int row = 0; row < settings.size(); row++) {
            final AssistantContext context = new NatTableAssistantContext(1, row);
            assertThat(provider.shouldShowProposals(context)).isFalse();
        }
    }

    @Test
    public void thereAreNoProposalsProvided_whenThereIsNoKeywordMatchingCurrentInput() throws Exception {
        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(getFile(project, "kw_based_settings.robot"));
        final List<RobotKeywordCall> settings = suiteFile.findSection(RobotSettingsSection.class).get().getChildren();
        final RobotKeywordsSection kwSection = (RobotKeywordsSection) suiteFile
                .createRobotSection(RobotKeywordsSection.SECTION_NAME);
        kwSection.createChild(0, "keyword");

        final IRowDataProvider<Object> dataProvider = prepareSettingsProvider(settings);
        final KeywordProposalsInSettingsProvider provider = new KeywordProposalsInSettingsProvider(suiteFile,
                dataProvider);

        for (int row = 0; row < settings.size(); row++) {
            final AssistantContext context = new NatTableAssistantContext(1, row);
            final RedContentProposal[] proposals = provider.computeProposals("foo", 1, context);
            assertThat(proposals).isEmpty();
        }
    }

    @Test
    public void thereAreProposalsProvided_whenInputIsMatchingAndProperContentIsInserted() throws Exception {
        final Text text = new Text(shell, SWT.SINGLE);
        text.setText("kw");

        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(getFile(project, "kw_based_settings.robot"));
        final RobotKeywordsSection kwSection = (RobotKeywordsSection) suiteFile
                .createRobotSection(RobotKeywordsSection.SECTION_NAME);
        kwSection.createChild(0, "keyword");

        final List<RobotKeywordCall> settings = suiteFile.findSection(RobotSettingsSection.class).get().getChildren();

        final IRowDataProvider<Object> dataProvider = prepareSettingsProvider(settings);
        final KeywordProposalsInSettingsProvider provider = new KeywordProposalsInSettingsProvider(suiteFile,
                dataProvider);

        for (int row = 0; row < settings.size(); row++) {
            final AssistantContext context = new NatTableAssistantContext(1, row);
            final RedContentProposal[] proposals = provider.computeProposals(text.getText(), 1, context);
            assertThat(proposals).hasSize(1);

            proposals[0].getModificationStrategy().insert(text, proposals[0]);
            assertThat(text.getText()).isEqualTo("keyword");
        }
    }

    @BooleanPreference(key = RedPreferences.ASSISTANT_KEYWORD_FROM_NOT_IMPORTED_LIBRARY_ENABLED, value = true)
    @Test
    public void thereAreOperationsToPerformAfterAccepting_onlyForNotAccessibleKeywordProposals() throws Exception {
        final Map<LibraryDescriptor, LibrarySpecification> refLibs = new LinkedHashMap<>();
        refLibs.putAll(Libraries.createRefLib("LibImported", "kw1", "kw2"));
        refLibs.putAll(Libraries.createRefLib("LibNotImported", "kw3", "kw4"));

        final RobotProject robotProject = robotModel.createRobotProject(project);
        robotProject.setReferencedLibraries(refLibs);

        final RobotSuiteFile suiteFile = robotModel
                .createSuiteFile(getFile(project, "kw_based_settings_with_lib_import.robot"));
        final List<RobotKeywordCall> settings = suiteFile.findSection(RobotSettingsSection.class).get().getChildren();

        final IRowDataProvider<Object> dataProvider = prepareSettingsProvider(settings);
        final KeywordProposalsInSettingsProvider provider = new KeywordProposalsInSettingsProvider(suiteFile,
                dataProvider);

        for (int row = 0; row < settings.size() - 1; row++) {
            final AssistantContext context = new NatTableAssistantContext(1, row);
            final RedContentProposal[] proposals = provider.computeProposals("kw", 2, context);
            assertThat(proposals).hasSize(4);

            assertThat(proposals[0].getLabel()).isEqualTo("kw1 - LibImported");
            assertThat(proposals[0].getOperationsToPerformAfterAccepting()).isEmpty();
            assertThat(proposals[1].getLabel()).isEqualTo("kw2 - LibImported");
            assertThat(proposals[1].getOperationsToPerformAfterAccepting()).isEmpty();
            assertThat(proposals[2].getLabel()).isEqualTo("kw3 - LibNotImported");
            assertThat(proposals[2].getOperationsToPerformAfterAccepting()).hasSize(1);
            assertThat(proposals[3].getLabel()).isEqualTo("kw4 - LibNotImported");
            assertThat(proposals[3].getOperationsToPerformAfterAccepting()).hasSize(1);
        }
    }

    @Test
    public void thereAreOperationsToPerformAfterAccepting_onlyForKeywordsWithArgumentsAndSettingIsNotTemplate()
            throws Exception {
        final RobotSuiteFile suiteFile = robotModel
                .createSuiteFile(getFile(project, "kw_based_settings_with_keywords_with_args.robot"));
        final List<RobotKeywordCall> settings = suiteFile.findSection(RobotSettingsSection.class).get().getChildren();

        final IRowDataProvider<Object> dataProvider = prepareSettingsProvider(settings, 5);
        final KeywordProposalsInSettingsProvider provider = new KeywordProposalsInSettingsProvider(suiteFile,
                dataProvider);

        for (int row = 0; row < settings.size(); row++) {
            final AssistantContext context = new NatTableAssistantContext(1, row);
            final RedContentProposal[] proposals = provider.computeProposals("kw", 2, context);
            assertThat(proposals).hasSize(2);

            assertThat(proposals[0].getLabel())
                    .isEqualTo("kw_no_args - kw_based_settings_with_keywords_with_args.robot");
            assertThat(proposals[0].getOperationsToPerformAfterAccepting()).isEmpty();
            assertThat(proposals[1].getLabel())
                    .isEqualTo("kw_with_args - kw_based_settings_with_keywords_with_args.robot");
            if (settings.get(row).getLinkedElement().getDeclaration().getText().equals("Test Template")) {
                assertThat(proposals[1].getOperationsToPerformAfterAccepting()).isEmpty();
            } else {
                assertThat(proposals[1].getOperationsToPerformAfterAccepting()).hasSize(1);
            }
        }
    }

    @Test
    public void thereAreNoOperationsToPerformAfterAccepting_whenColumnCountIsToSmall() throws Exception {
        final RobotSuiteFile suiteFile = robotModel
                .createSuiteFile(getFile(project, "kw_based_settings_with_keywords_with_args.robot"));
        final List<RobotKeywordCall> settings = suiteFile.findSection(RobotSettingsSection.class).get().getChildren();

        final IRowDataProvider<Object> dataProvider = prepareSettingsProvider(settings, 4);
        final KeywordProposalsInSettingsProvider provider = new KeywordProposalsInSettingsProvider(suiteFile,
                dataProvider);

        for (int row = 0; row < settings.size(); row++) {
            final AssistantContext context = new NatTableAssistantContext(1, row);
            final RedContentProposal[] proposals = provider.computeProposals("kw", 2, context);
            assertThat(proposals).hasSize(2);

            assertThat(proposals[0].getLabel())
                    .isEqualTo("kw_no_args - kw_based_settings_with_keywords_with_args.robot");
            assertThat(proposals[0].getOperationsToPerformAfterAccepting()).isEmpty();
            assertThat(proposals[1].getLabel())
                    .isEqualTo("kw_with_args - kw_based_settings_with_keywords_with_args.robot");
            assertThat(proposals[1].getOperationsToPerformAfterAccepting()).isEmpty();
        }
    }

    @Test
    public void keywordsWithEmbeddedArgumentsShouldBeInsertedWithoutCommitting() throws Exception {
        final RobotSuiteFile suiteFile = robotModel
                .createSuiteFile(getFile(project, "kw_based_settings_with_keywords_with_embedded_args.robot"));
        final List<RobotKeywordCall> settings = suiteFile.findSection(RobotSettingsSection.class).get().getChildren();

        final IRowDataProvider<Object> dataProvider = prepareSettingsProvider(settings, 5);
        final KeywordProposalsInSettingsProvider provider = new KeywordProposalsInSettingsProvider(suiteFile,
                dataProvider);

        for (int row = 0; row < settings.size(); row++) {
            final AssistantContext context = new NatTableAssistantContext(1, row);
            final RedContentProposal[] proposals = provider.computeProposals("kw", 2, context);
            assertThat(proposals).hasSize(2);

            assertThat(proposals[0].getLabel())
                    .isEqualTo("kw_no_arg - kw_based_settings_with_keywords_with_embedded_args.robot");
            assertThat(proposals[0].getModificationStrategy()).isInstanceOfSatisfying(
                    SubstituteTextModificationStrategy.class,
                    strategy -> assertThat(strategy.shouldCommitAfterInsert()).isTrue());
            assertThat(proposals[1].getLabel())
                    .isEqualTo("kw_with_${arg} - kw_based_settings_with_keywords_with_embedded_args.robot");
            assertThat(proposals[1].getModificationStrategy()).isInstanceOfSatisfying(
                    SubstituteTextModificationStrategy.class,
                    strategy -> assertThat(strategy.shouldCommitAfterInsert()).isFalse());
        }
    }

    private static IRowDataProvider<Object> prepareSettingsProvider(final List<RobotKeywordCall> settings) {
        @SuppressWarnings("unchecked")
        final IRowDataProvider<Object> dataProvider = mock(IRowDataProvider.class);
        for (int i = 0; i < settings.size(); i++) {
            final RobotKeywordCall call = settings.get(i);
            final Entry<String, Object> entry = new SimpleEntry<>(call.getName(), call);
            when(dataProvider.getRowObject(i)).thenReturn(entry);
        }
        return dataProvider;
    }

    private static IRowDataProvider<Object> prepareSettingsProvider(final List<RobotKeywordCall> settings,
            final int columnCount) {
        final IRowDataProvider<Object> dataProvider = prepareSettingsProvider(settings);
        when(dataProvider.getColumnCount()).thenReturn(columnCount);
        when(dataProvider.getDataValue(anyInt(), anyInt())).thenReturn("");
        return dataProvider;
    }
}
