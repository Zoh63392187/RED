/*
* Copyright 2016 Nokia Solutions and Networks
* Licensed under the Apache License, Version 2.0,
* see license.txt file for details.
*/
package org.robotframework.ide.eclipse.main.plugin.propertytester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.junit.jupiter.api.Test;
import org.rf.ide.core.testdata.model.ModelType;
import org.rf.ide.core.testdata.model.table.LocalSetting;
import org.rf.ide.core.testdata.text.read.recognizer.RobotToken;
import org.robotframework.ide.eclipse.main.plugin.model.RobotFileInternalElement;
import org.robotframework.ide.eclipse.main.plugin.model.RobotKeywordCall;
import org.robotframework.ide.eclipse.main.plugin.model.RobotSetting;
import org.robotframework.ide.eclipse.main.plugin.model.RobotSuiteStreamFile;

public class SelectionsPropertyTesterTest {

    private final SelectionsPropertyTester tester = new SelectionsPropertyTester();

    @Test
    public void exceptionIsThrown_whenReceiverIsNotStructuredSelection() {
        assertThatIllegalArgumentException().isThrownBy(() -> tester.test(new Object(), "property", null, true))
                .withMessage("Property tester is unable to test properties of java.lang.Object. It should be used with "
                        + ISelection.class.getName())
                .withNoCause();
    }

    @Test
    public void falseIsReturned_whenExpectedValueIsAString() {
        final boolean testResult = tester.test(StructuredSelection.EMPTY,
                SelectionsPropertyTester.ALL_ELEMENTS_HAVE_SAME_TYPE, null, "value");

        assertThat(testResult).isFalse();
    }

    @Test
    public void falseIsReturnedForUnknownProperty() {
        assertThat(tester.test(StructuredSelection.EMPTY, "unknown_property", null, true)).isFalse();
        assertThat(tester.test(StructuredSelection.EMPTY, "unknown_property", null, false)).isFalse();
    }

    @Test
    public void testAllElementsHaveSameTypeProperty() {
        final IStructuredSelection selectionWithSameTypeElements1 = new StructuredSelection(
                new Object[] { "abc", "def", "ghi" });
        final IStructuredSelection selectionWithSameTypeElements2 = new StructuredSelection(
                new Object[] { new B(), new A(), new B() });
        final IStructuredSelection selectionWithDifferentTypeElements1 = new StructuredSelection(
                new Object[] { "abc", 10, new Object() });
        final IStructuredSelection selectionWithDifferentTypeElements2 = new StructuredSelection(
                new Object[] { new B(), new A(), new Object() });

        assertThat(allElementsHaveSameType(StructuredSelection.EMPTY, true)).isTrue();
        assertThat(allElementsHaveSameType(StructuredSelection.EMPTY, false)).isFalse();

        assertThat(allElementsHaveSameType(selectionWithSameTypeElements1, true)).isTrue();
        assertThat(allElementsHaveSameType(selectionWithSameTypeElements1, false)).isFalse();

        assertThat(allElementsHaveSameType(selectionWithSameTypeElements2, true)).isTrue();
        assertThat(allElementsHaveSameType(selectionWithSameTypeElements2, false)).isFalse();

        assertThat(allElementsHaveSameType(selectionWithDifferentTypeElements1, true)).isFalse();
        assertThat(allElementsHaveSameType(selectionWithDifferentTypeElements1, false)).isTrue();

        assertThat(allElementsHaveSameType(selectionWithDifferentTypeElements2, true)).isFalse();
        assertThat(allElementsHaveSameType(selectionWithDifferentTypeElements2, false)).isTrue();
    }

    private boolean allElementsHaveSameType(final IStructuredSelection selection, final boolean expected) {
        return tester.test(selection, SelectionsPropertyTester.ALL_ELEMENTS_HAVE_SAME_TYPE, null, expected);
    }

    @Test
    public void testSelectedActualFileProperty() {
        final RobotFileInternalElement element = mock(RobotFileInternalElement.class);
        final RobotFileInternalElement elementFromHistory = mock(RobotFileInternalElement.class);
        final RobotSuiteStreamFile historyFile = mock(RobotSuiteStreamFile.class);
        when(elementFromHistory.getSuiteFile()).thenReturn(historyFile);
        final IStructuredSelection selectionWithoutRFIE = new StructuredSelection(
                new Object[] { "sth" });
        final IStructuredSelection selectionWithRFIE = new StructuredSelection(
                new Object[] { element });
        final IStructuredSelection selectionWithHistoryRFIE = new StructuredSelection(
                new Object[] { elementFromHistory });

        assertThat(selectedActualFile(StructuredSelection.EMPTY, true)).isFalse();
        assertThat(selectedActualFile(StructuredSelection.EMPTY, false)).isTrue();

        assertThat(selectedActualFile(selectionWithoutRFIE, true)).isFalse();
        assertThat(selectedActualFile(selectionWithoutRFIE, false)).isTrue();

        assertThat(selectedActualFile(selectionWithRFIE, true)).isTrue();
        assertThat(selectedActualFile(selectionWithRFIE, false)).isFalse();

        assertThat(selectedActualFile(selectionWithHistoryRFIE, true)).isFalse();
        assertThat(selectedActualFile(selectionWithHistoryRFIE, false)).isTrue();
    }

    private boolean selectedActualFile(final IStructuredSelection selection, final boolean expected) {
        return tester.test(selection, SelectionsPropertyTester.SELECTED_ACTUAL_FILE, null, expected);
    }

    @Test
    public void testIsMetadataSelectedProperty() {
        final RobotSetting nonMetaSetting = mock(RobotSetting.class);
        final RobotSetting metadataSetting = mock(RobotSetting.class);
        when(nonMetaSetting.getGroup()).thenReturn(RobotSetting.SettingsGroup.NO_GROUP);
        when(metadataSetting.getGroup()).thenReturn(RobotSetting.SettingsGroup.METADATA);
        final IStructuredSelection selectionWithNonSetting = new StructuredSelection(
                new Object[] { new Object() });
        final IStructuredSelection selectionWithNonMetaSetting = new StructuredSelection(
                new Object[] { nonMetaSetting });
        final IStructuredSelection selectionWithMetadata = new StructuredSelection(
                new Object[] { metadataSetting });

        assertThat(isMetadataSelected(StructuredSelection.EMPTY, true)).isFalse();
        assertThat(isMetadataSelected(StructuredSelection.EMPTY, false)).isTrue();

        assertThat(isMetadataSelected(selectionWithNonSetting, true)).isFalse();
        assertThat(isMetadataSelected(selectionWithNonSetting, false)).isTrue();

        assertThat(isMetadataSelected(selectionWithNonMetaSetting, true)).isFalse();
        assertThat(isMetadataSelected(selectionWithNonMetaSetting, false)).isTrue();

        assertThat(isMetadataSelected(selectionWithMetadata, true)).isTrue();
        assertThat(isMetadataSelected(selectionWithMetadata, false)).isFalse();
    }

    private boolean isMetadataSelected(final IStructuredSelection selection, final boolean expected) {
        return tester.test(selection, SelectionsPropertyTester.METADATA_SELECTED, null, expected);
    }

    @Test
    public void testIsKeywordCallButNotDocumentationProperty() {
        final RobotKeywordCall arguments = new RobotKeywordCall(null,
                new LocalSetting<>(ModelType.USER_KEYWORD_ARGUMENTS, RobotToken.create("")));
        final RobotKeywordCall testDocumentation = new RobotKeywordCall(null,
                new LocalSetting<>(ModelType.TEST_CASE_DOCUMENTATION, RobotToken.create("")));
        final RobotKeywordCall keywordDocumentation = new RobotKeywordCall(null,
                new LocalSetting<>(ModelType.USER_KEYWORD_DOCUMENTATION, RobotToken.create("")));
        final IStructuredSelection selectionWithCall = new StructuredSelection(new Object[] { arguments });
        final IStructuredSelection selectionWithTestDoc = new StructuredSelection(
                new Object[] { testDocumentation });
        final IStructuredSelection selectionWithKeywordDoc = new StructuredSelection(
                new Object[] { keywordDocumentation });

        assertThat(isKeywordCallButNotDocumentation(StructuredSelection.EMPTY, true)).isFalse();
        assertThat(isKeywordCallButNotDocumentation(StructuredSelection.EMPTY, false)).isTrue();

        assertThat(isKeywordCallButNotDocumentation(selectionWithCall, true)).isFalse();
        assertThat(isKeywordCallButNotDocumentation(selectionWithCall, false)).isTrue();

        assertThat(isKeywordCallButNotDocumentation(selectionWithTestDoc, true)).isFalse();
        assertThat(isKeywordCallButNotDocumentation(selectionWithTestDoc, false)).isTrue();

        assertThat(isKeywordCallButNotDocumentation(selectionWithKeywordDoc, true)).isFalse();
        assertThat(isKeywordCallButNotDocumentation(selectionWithKeywordDoc, false)).isTrue();
    }

    private boolean isKeywordCallButNotDocumentation(final IStructuredSelection selection, final boolean expected) {
        return tester.test(selection, SelectionsPropertyTester.METADATA_SELECTED, null, expected);
    }

    private static class A {
    }

    private static class B extends A {
    }
}
