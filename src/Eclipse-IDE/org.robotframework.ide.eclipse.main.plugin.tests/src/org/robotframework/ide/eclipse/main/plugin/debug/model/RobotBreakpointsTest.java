/*
* Copyright 2017 Nokia Solutions and Networks
* Licensed under the Apache License, Version 2.0,
* see license.txt file for details.
*/
package org.robotframework.ide.eclipse.main.plugin.debug.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.robotframework.red.junit.jupiter.ProjectExtension.createFile;
import static org.robotframework.red.junit.jupiter.ProjectExtension.getFile;

import java.net.URI;
import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.model.IBreakpoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.rf.ide.core.execution.debug.RobotBreakpoint;
import org.robotframework.red.junit.jupiter.Project;
import org.robotframework.red.junit.jupiter.ProjectExtension;

@ExtendWith(ProjectExtension.class)
public class RobotBreakpointsTest {

    @Project
    static IProject project;

    private final IBreakpointManager breakpointManager = mock(IBreakpointManager.class);

    private final RobotBreakpoints breakpoints = new RobotBreakpoints(breakpointManager);

    @BeforeAll
    public static void beforeSuite() throws Exception {
        createFile(project, "suite.robot",
                "*** Test Cases ***",
                "case",
                "  Log  10");
    }

    @BeforeEach
    public void beforeTest() {
        reset(breakpointManager);
    }

    @AfterEach
    public void afterTest() throws CoreException {
        getFile(project, "suite.robot").deleteMarkers(RobotLineBreakpoint.MARKER_ID, true, 1);
    }

    @Test
    public void lineBreakpointForLineAndUriIsProperlyReturned() throws Exception {
        final IFile file = getFile(project, "suite.robot");
        final IMarker marker = createMarker(file, true);
        marker.setAttribute(IMarker.LINE_NUMBER, 42);

        final RobotLineBreakpoint bp = new RobotLineBreakpoint(marker);
        when(breakpointManager.isEnabled()).thenReturn(true);
        when(breakpointManager.getBreakpoints(RobotDebugElement.DEBUG_MODEL_ID)).thenReturn(new IBreakpoint[] { bp });

        final Optional<RobotBreakpoint> foundBreakpoint = breakpoints.lineBreakpointFor(file.getLocationURI(), 42);
        assertThat(foundBreakpoint).contains(bp);
    }

    @Test
    public void lineBreakpointIsNotFoundIfItIsDisabled() throws Exception {
        final IFile file = getFile(project, "suite.robot");
        final IMarker marker = createMarker(file, false);
        marker.setAttribute(IMarker.LINE_NUMBER, 42);

        final RobotLineBreakpoint bp = new RobotLineBreakpoint(marker);
        when(breakpointManager.isEnabled()).thenReturn(true);
        when(breakpointManager.getBreakpoints(RobotDebugElement.DEBUG_MODEL_ID)).thenReturn(new IBreakpoint[] { bp });

        final Optional<RobotBreakpoint> foundBreakpoint = breakpoints.lineBreakpointFor(file.getLocationURI(), 42);
        assertThat(foundBreakpoint).isEmpty();
    }

    @Test
    public void lineBreakpointIsNotFoundIfItHasOtherLine() throws Exception {
        final IFile file = getFile(project, "suite.robot");
        final IMarker marker = createMarker(file, true);
        marker.setAttribute(IMarker.LINE_NUMBER, 43);

        final RobotLineBreakpoint bp = new RobotLineBreakpoint(marker);
        when(breakpointManager.isEnabled()).thenReturn(true);
        when(breakpointManager.getBreakpoints(RobotDebugElement.DEBUG_MODEL_ID)).thenReturn(new IBreakpoint[] { bp });

        final Optional<RobotBreakpoint> foundBreakpoint = breakpoints.lineBreakpointFor(file.getLocationURI(), 42);
        assertThat(foundBreakpoint).isEmpty();
    }

    @Test
    public void lineBreakpointIsNotFoundIfItHasOtherSource() throws Exception {
        final IMarker marker = createMarker(getFile(project, "suite.robot"), true);
        marker.setAttribute(IMarker.LINE_NUMBER, 42);

        final RobotLineBreakpoint bp = new RobotLineBreakpoint(marker);
        when(breakpointManager.isEnabled()).thenReturn(true);
        when(breakpointManager.getBreakpoints(RobotDebugElement.DEBUG_MODEL_ID)).thenReturn(new IBreakpoint[] { bp });

        final Optional<RobotBreakpoint> foundBreakpoint = breakpoints
                .lineBreakpointFor(URI.create("file:///different.robot"), 42);
        assertThat(foundBreakpoint).isEmpty();
    }

    @Test
    public void lineBreakpointIsNotFoundIfBreakpointsAreGloballyDisabled() throws Exception {
        final IFile file = getFile(project, "suite.robot");
        final IMarker marker = createMarker(file, true);
        marker.setAttribute(IMarker.LINE_NUMBER, 42);

        final RobotLineBreakpoint bp = new RobotLineBreakpoint(marker);
        when(breakpointManager.isEnabled()).thenReturn(false);
        when(breakpointManager.getBreakpoints(RobotDebugElement.DEBUG_MODEL_ID)).thenReturn(new IBreakpoint[] { bp });

        final Optional<RobotBreakpoint> foundBreakpoint = breakpoints.lineBreakpointFor(file.getLocationURI(), 42);
        assertThat(foundBreakpoint).isEmpty();
    }

    @Test
    public void kwFailBreakpointForGivenNameIsReturnedIfItMatchesPattern() throws Exception {
        final IMarker marker = createMarker(true);
        marker.setAttribute(RobotKeywordFailBreakpoint.KEYWORD_NAME_PATTERN_ATTRIBUTE, "Key?ord*");

        final RobotKeywordFailBreakpoint bp = new RobotKeywordFailBreakpoint(marker);
        when(breakpointManager.isEnabled()).thenReturn(true);
        when(breakpointManager.getBreakpoints(RobotDebugElement.DEBUG_MODEL_ID)).thenReturn(new IBreakpoint[] { bp });

        assertThat(breakpoints.keywordFailBreakpointFor("keyword")).contains(bp);
        assertThat(breakpoints.keywordFailBreakpointFor("keyword with longer name")).contains(bp);
        assertThat(breakpoints.keywordFailBreakpointFor("keyford")).contains(bp);
    }

    @Test
    public void kwFailBreakpointIsNotFoundIfItIsDisabled() throws Exception {
        final IMarker marker = createMarker(false);
        marker.setAttribute(RobotKeywordFailBreakpoint.KEYWORD_NAME_PATTERN_ATTRIBUTE, "Key?ord*");

        final RobotKeywordFailBreakpoint bp = new RobotKeywordFailBreakpoint(marker);
        when(breakpointManager.isEnabled()).thenReturn(true);
        when(breakpointManager.getBreakpoints(RobotDebugElement.DEBUG_MODEL_ID)).thenReturn(new IBreakpoint[] { bp });

        assertThat(breakpoints.keywordFailBreakpointFor("keyword")).isEmpty();
    }

    @Test
    public void kwFailBreakpointIsNotFoundIfItDoesNotMatchPattern() throws Exception {
        final IMarker marker = createMarker(true);
        marker.setAttribute(RobotKeywordFailBreakpoint.KEYWORD_NAME_PATTERN_ATTRIBUTE, "Key?ord*");

        final RobotKeywordFailBreakpoint bp = new RobotKeywordFailBreakpoint(marker);
        when(breakpointManager.isEnabled()).thenReturn(true);
        when(breakpointManager.getBreakpoints(RobotDebugElement.DEBUG_MODEL_ID)).thenReturn(new IBreakpoint[] { bp });

        assertThat(breakpoints.keywordFailBreakpointFor("keyyword")).isEmpty();
    }

    @Test
    public void kwFailBreakpointIsNotFoundIfBreakpointsAreGloballyDisabled() throws Exception {
        final IMarker marker = createMarker(true);
        marker.setAttribute(RobotKeywordFailBreakpoint.KEYWORD_NAME_PATTERN_ATTRIBUTE, "Key?ord*");

        final RobotKeywordFailBreakpoint bp = new RobotKeywordFailBreakpoint(marker);
        when(breakpointManager.isEnabled()).thenReturn(false);
        when(breakpointManager.getBreakpoints(RobotDebugElement.DEBUG_MODEL_ID)).thenReturn(new IBreakpoint[] { bp });

        assertThat(breakpoints.keywordFailBreakpointFor("keyword")).isEmpty();
    }

    @Test
    public void breakpointsAreEnabled_ifTheyWereDisabledDueToHitCountStop() throws Exception {
        final IBreakpoint bp1 = mock(IBreakpoint.class);
        final RobotLineBreakpoint bp2 = mock(RobotLineBreakpoint.class);
        final RobotLineBreakpoint bp3 = new RobotLineBreakpoint(createMarker(false));
        final RobotLineBreakpoint bp4 = new RobotLineBreakpoint(createMarker(false));
        doThrow(new CoreException(mock(IStatus.class))).when(bp2).enableIfDisabledByHitCounter();
        bp3.setDisabledDueToHitCounter(true);
        bp4.setDisabledDueToHitCounter(false);

        final RobotKeywordFailBreakpoint bp5 = new RobotKeywordFailBreakpoint(createMarker(false));
        final RobotKeywordFailBreakpoint bp6 = new RobotKeywordFailBreakpoint(createMarker(false));
        bp5.setDisabledDueToHitCounter(true);
        bp6.setDisabledDueToHitCounter(false);

        when(breakpointManager.getBreakpoints(RobotDebugElement.DEBUG_MODEL_ID))
                .thenReturn(new IBreakpoint[] { bp1, bp3, bp4, bp5, bp6 });

        breakpoints.enableBreakpointsDisabledByHitCounter();

        assertThat(bp3.isEnabled()).isTrue();
        assertThat(bp4.isEnabled()).isFalse();
        assertThat(bp5.isEnabled()).isTrue();
        assertThat(bp6.isEnabled()).isFalse();
    }

    private IMarker createMarker(final boolean enabled) {
        return createMarker(null, enabled);
    }

    private IMarker createMarker(final IResource resource, final boolean enabled) {
        final MockMarker marker = new MockMarker(resource);
        marker.setAttribute(IBreakpoint.ENABLED, enabled);
        return marker;
    }
}
