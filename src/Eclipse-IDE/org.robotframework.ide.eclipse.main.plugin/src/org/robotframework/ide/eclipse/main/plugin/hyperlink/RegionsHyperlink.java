/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.hyperlink;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.robotframework.ide.eclipse.main.plugin.RedImages;
import org.robotframework.ide.eclipse.main.plugin.model.RobotSuiteFile;

import com.google.common.annotations.VisibleForTesting;

/**
 * @author mmarzec
 */
public class RegionsHyperlink implements RedHyperlink {

    private final ITextViewer viewer;

    private final IRegion source;

    private final IRegion destination;

    private final RobotSuiteFile sourceAndDestinationFile;

    private final String additionalLabelDecoration;

    private final String elementName;

    public RegionsHyperlink(final ITextViewer viewer, final IRegion from, final IRegion to) {
        this(viewer, null, from, to, "", "");
    }

    public RegionsHyperlink(final ITextViewer viewer, final RobotSuiteFile fromAndToFile, final IRegion from,
            final IRegion to, final String additionalLabelDecoration, final String elementName) {
        this.viewer = viewer;
        this.sourceAndDestinationFile = fromAndToFile;
        this.source = from;
        this.destination = to;
        this.additionalLabelDecoration = additionalLabelDecoration;
        this.elementName = elementName;
    }

    @Override
    public IRegion getHyperlinkRegion() {
        return source;
    }

    @VisibleForTesting
    public IRegion getDestinationRegion() {
        return destination;
    }

    @Override
    public String getTypeLabel() {
        return null;
    }

    @Override
    public String getHyperlinkText() {
        return "Open Definition " + elementName();
    }

    @Override
    public String elementName() {
        return elementName;
    }

    @Override
    public String getLabelForCompoundHyperlinksDialog() {
        return sourceAndDestinationFile == null ? "[local definition in current file]"
                : sourceAndDestinationFile.getName();
    }

    @Override
    public String additionalLabelDecoration() {
        return additionalLabelDecoration;
    }

    @Override
    public ImageDescriptor getImage() {
        return RedImages.getImageForFileWithExtension(
                sourceAndDestinationFile == null ? "" : sourceAndDestinationFile.getFileExtension());
    }

    @Override
    public void open() {
        try {
            final int topIndexPosition = viewer.getDocument().getLineOfOffset(destination.getOffset());
            viewer.getTextWidget().setTopIndex(topIndexPosition);
            viewer.getTextWidget().setSelection(destination.getOffset(), destination.getOffset() + destination.getLength());

        } catch (final BadLocationException e) {
            throw new IllegalArgumentException("Cannot open hyperlink", e);
        }
    }
}
