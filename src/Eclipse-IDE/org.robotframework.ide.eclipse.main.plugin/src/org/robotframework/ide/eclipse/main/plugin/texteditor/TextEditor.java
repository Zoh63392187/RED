package org.robotframework.ide.eclipse.main.plugin.texteditor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.e4.core.commands.ECommandService;
import org.eclipse.e4.core.commands.EHandlerService;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.tools.services.IDirtyProviderService;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.Persist;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.AbstractReusableInformationControlCreator;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.TextViewerUndoManager;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.source.AnnotationRulerColumn;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.LineNumberRulerColumn;
import org.eclipse.jface.text.source.OverviewRuler;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISources;
import org.eclipse.ui.internal.texteditor.AnnotationType;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.eclipse.ui.texteditor.AnnotationPreference;
import org.eclipse.ui.texteditor.DefaultMarkerAnnotationAccess;
import org.eclipse.ui.texteditor.ResourceMarkerAnnotationModel;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;
import org.eclipse.wb.swt.SWTResourceManager;
import org.robotframework.ide.eclipse.main.plugin.debug.model.RobotLineBreakpoint;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.RobotFormEditor;
import org.robotframework.ide.eclipse.main.plugin.texteditor.handlers.SaveAsHandler;
import org.robotframework.ide.eclipse.main.plugin.texteditor.utils.SharedTextColors;
import org.robotframework.ide.eclipse.main.plugin.texteditor.utils.TextEditorContentAssistProcessor;
import org.robotframework.ide.eclipse.main.plugin.texteditor.utils.TextEditorSourceViewerConfiguration;
import org.robotframework.ide.eclipse.main.plugin.texteditor.utils.TxtScanner;

/**
 * @author mmarzec
 *
 */
@SuppressWarnings("restriction")
public class TextEditor {

	@Inject
	ECommandService commandService;
	
	@Inject
	EHandlerService handlerService;
	
	@Inject
	IDirtyProviderService dirtyProviderService;
	
	private IFile editedFile;
	private IEditorInput input;
	private SourceViewer viewer;
	
	private int breakpointLine = 0; 
	
	private CompositeRuler compositeRuler;
	
	@PostConstruct
	public void postConstruct(final Composite parent, final IEditorInput input, final IEditorPart editorPart) {
	    this.input = input;

		final FillLayout layout = new FillLayout();
		layout.marginHeight = 1;
        parent.setLayout(layout);
        
        final ParameterizedCommand saveAsCommand = this.createSaveAsCommand();
		
        FileEditorInput fileEditorInput = null;
        if(input instanceof FileEditorInput) {
        	fileEditorInput = (FileEditorInput) input;
        } else {
            // FIXME : there are other inputs when editor is opened via history
            // for example!
            return;
        }
        
        editedFile = fileEditorInput.getFile();
        final String text = this.extractTextFromFile();

        this.deleteMarkersFromFile();
		//this.createMarkers();
        
		final AnnotationType errorAnnotationType = new AnnotationType("org.eclipse.ui.workbench.texteditor.error", null);
		final AnnotationType breakpointAnnotationType = new AnnotationType("org.eclipse.debug.core.breakpoint", null);
		
		final DefaultMarkerAnnotationAccess markerAnnotationAccess = new DefaultMarkerAnnotationAccess();
		final ResourceMarkerAnnotationModel markerAnnotationModel = new ResourceMarkerAnnotationModel(editedFile);
		
		compositeRuler = new CompositeRuler(1);
		compositeRuler.setModel(markerAnnotationModel);
		
		final OverviewRuler overviewRuler = new OverviewRuler(markerAnnotationAccess, 15, new SharedTextColors());
		overviewRuler.addAnnotationType(errorAnnotationType.getType());
		overviewRuler.addHeaderAnnotationType(errorAnnotationType.getType());
		overviewRuler.setModel(markerAnnotationModel);
		
		//TODO move SourceViewer initialization to other class
		viewer = new SourceViewer(parent, compositeRuler, overviewRuler, true, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		viewer.setEditable(true);
		viewer.getTextWidget().setFont(JFaceResources.getTextFont());
		
		final AnnotationRulerColumn annotationColumn = new AnnotationRulerColumn(15,markerAnnotationAccess);
		annotationColumn.addAnnotationType(errorAnnotationType.getType());
		annotationColumn.addAnnotationType(breakpointAnnotationType.getType());
		
		final LineNumberRulerColumn lineNumberColumn = new LineNumberRulerColumn();
		lineNumberColumn.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_BACKGROUND));
		lineNumberColumn.setForeground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_DARK_SHADOW));
		lineNumberColumn.setFont(JFaceResources.getTextFont());
		
		viewer.addVerticalRulerColumn(annotationColumn);
		viewer.addVerticalRulerColumn(lineNumberColumn);
		
		final SourceViewerDecorationSupport decorationSupport = new SourceViewerDecorationSupport(viewer, overviewRuler, markerAnnotationAccess, new SharedTextColors());
		final IPreferenceStore editorsPreferenceStore = new ScopedPreferenceStore(InstanceScope.INSTANCE, "org.eclipse.ui.editors");
		final AnnotationPreference errorAnnotationPreference = this.createAnnotationPreference(errorAnnotationType);
		decorationSupport.setAnnotationPreference(errorAnnotationPreference);
		decorationSupport.install(editorsPreferenceStore);
		
		final IDocument document = new Document();
		document.set( text );
		viewer.setDocument( document, markerAnnotationModel );
		
		final TextEditorSourceViewerConfiguration svc = new TextEditorSourceViewerConfiguration();
		viewer.configure(svc);
		
		final Menu contextMenu = this.createContextMenu(lineNumberColumn, saveAsCommand);

		final TextViewerUndoManager undoManager = new TextViewerUndoManager(3);
		viewer.setUndoManager(undoManager);
		undoManager.connect(viewer);
		
		final ContentAssistant contentAssistant = this.createContentAssistant();
		contentAssistant.install(viewer);
		
		final PresentationReconciler reconciler = this.createPresentationReconciler();
		reconciler.install(viewer);
		
		//TODO Add Listener classes
		viewer.getTextWidget().addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent e) {
				dirtyProviderService.setDirtyState(true);
			}
		});
		
		viewer.getTextWidget().addKeyListener(new KeyListener() {
			@Override
			public void keyReleased(final KeyEvent e) {
			}
			@Override
			public void keyPressed(final KeyEvent e) {
				if ((e.stateMask == SWT.CTRL) && (e.keyCode == 'z')) {
					if(undoManager.undoable()) {
						undoManager.undo();
					}
				}
				if ((e.stateMask == SWT.CTRL) && (e.keyCode == 'y')) {
					if(undoManager.redoable()) {
						undoManager.redo();
					}
				}
				if ((e.stateMask == SWT.CTRL) && (e.keyCode == 's') && editorPart.isDirty()) {
					save(null);
				}
				if ((e.stateMask == SWT.CTRL) && (e.keyCode == SWT.SPACE)) {
				    contentAssistant.showPossibleCompletions();
				}
				if((e.stateMask & SWT.CTRL) != 0 && (e.stateMask & SWT.SHIFT) != 0 && e.keyCode == 'v') {
					contentAssistant.showContextInformation();
				}
			}
		});
		
		viewer.getTextWidget().addMouseListener(new MouseListener() {
			@Override
			public void mouseUp(final MouseEvent e) {}
			@Override
			public void mouseDown(final MouseEvent e) {
				if(e.button == 3) {
					contextMenu.setVisible(true);
				}
			}
			@Override
			public void mouseDoubleClick(final MouseEvent e) {}
		});
		
        compositeRuler.getControl().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDoubleClick(final MouseEvent e) {
                try {
                    final int line = (e.y / viewer.getTextWidget().getLineHeight() + 1) + viewer.getTopIndex(); 
                    if (line > viewer.getTextWidget().getLineCount()) {
                        return;
                    }

                    final IBreakpointManager breakpointManager = DebugPlugin.getDefault().getBreakpointManager();
                    
                    for (final IBreakpoint point : breakpointManager.getBreakpoints()) {
                        if (point.getMarker().getResource().equals(editedFile)
                                && point.getMarker().getAttribute(IMarker.LINE_NUMBER, -1) == line) {
                            breakpointManager.removeBreakpoint(point, true);
                            return;
                        }
                    }
                    breakpointManager.addBreakpoint(new RobotLineBreakpoint(editedFile, line));
                } catch (final CoreException e1) {
                    e1.printStackTrace();
                }
            }
        });
	}
	
	@Persist
	public void save(final IProgressMonitor monitor) {
		
		dirtyProviderService.setDirtyState(false);
		
        try (InputStream is = new ByteArrayInputStream(viewer.getDocument().get().getBytes())) {
            editedFile.setContents(is, false, true, monitor);
        } catch (CoreException | IOException e) {
            e.printStackTrace();
        }
		
	}

	@Focus
	public void onFocus() {
		
	}
	
	@Inject
    @Optional
    private void highlightLineEvent(@UIEventTopic("TextEditor/HighlightLine") final org.osgi.service.event.Event event,
            @Named(ISources.ACTIVE_EDITOR_NAME) final RobotFormEditor editor) {
	    if(((String) event.getProperty("file")).equals(editedFile.getName())) {
            if (editor != null) {
                editor.activateSourcePage();
            }
	        final int line = Integer.parseInt((String) event.getProperty("line"));
	        if(line > 0) {
        	    viewer.getTextWidget().setLineBackground(breakpointLine, 1, SWTResourceManager.getColor(255, 255, 255));
        	    viewer.getTextWidget().setLineBackground(line-1, 1, SWTResourceManager.getColor(198, 219, 174));
        	    showHighlightedLine(line);
        	    compositeRuler.immediateUpdate();
        	    breakpointLine = line-1;
	        }
	    }
    }
	
	private void showHighlightedLine(int lineNumber) {
	    int visibleLinesNumber = viewer.getTextWidget().getClientArea().height / viewer.getTextWidget().getLineHeight();
	    if (visibleLinesNumber < 2) {
	        viewer.getTextWidget().setTopIndex(lineNumber-1);
	        return;
	    }
	    int linesToCenter = visibleLinesNumber / 2;
	    int topIndexPosiiton = lineNumber - linesToCenter;
	    if(topIndexPosiiton >= 0) {
	        viewer.getTextWidget().setTopIndex(topIndexPosiiton);
	    } else {
	        viewer.getTextWidget().setTopIndex(lineNumber-1);
	    }
	}

    @Inject
    @Optional
    private void clearHighlightedLineEvent(@UIEventTopic("TextEditor/ClearHighlightedLine") final String file,
            @Named(ISources.ACTIVE_EDITOR_NAME) final RobotFormEditor editor) {
        if ("".equals(file) || editedFile.getName().equals(file)) {
            editor.activateSourcePage();
	        viewer.getTextWidget().setLineBackground(breakpointLine, 1, SWTResourceManager.getColor(255, 255, 255));
	        breakpointLine = 0;
	    }
    }
	
	private ParameterizedCommand createSaveAsCommand() {
		ParameterizedCommand cmd = null;
		final Command saveAsCommand = commandService.getCommand("org.eclipse.ui.file.saveAs");
		if (saveAsCommand.isDefined()) {
			handlerService.activateHandler("org.eclipse.ui.file.saveAs",new SaveAsHandler());
			cmd = commandService.createCommand("org.eclipse.ui.file.saveAs", null);
			handlerService.canExecute(cmd);
		}
		return cmd;
	}
	
	private String extractTextFromFile() {
		Scanner scanner = null;
        final StringBuilder text = new StringBuilder("");
        try(InputStream is = editedFile.getContents()) {
        	 scanner = new Scanner(is).useDelimiter("\\n");
    		while(scanner.hasNext()) {
    			text.append(scanner.next() + "\n");
    		}
		} catch (CoreException | IOException e) {
			e.printStackTrace();
		}
		scanner.close();
		
		return text.toString();
	}
	
	private void deleteMarkersFromFile() {
		try {
			final IMarker[] markers = editedFile.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ONE);
			for (int i = 0; i < markers.length; i++) {
				markers[i].delete();
			}
		} catch (final CoreException e) {
			e.printStackTrace();
		}
	}
	
	private AnnotationPreference createAnnotationPreference(final AnnotationType type) {
		final AnnotationPreference annotationPreference = new AnnotationPreference();
		annotationPreference.setAnnotationType(type.getType());
		annotationPreference.setTextStylePreferenceKey("errorTextStyle");
		annotationPreference.setTextStyleValue("UNDERLINE");
		annotationPreference.setOverviewRulerPreferenceKey("errorIndicationInOverviewRuler");
		annotationPreference.setOverviewRulerPreferenceValue(true);
		annotationPreference.setTextPreferenceKey("errorIndication");
		annotationPreference.setTextPreferenceValue(true);
		annotationPreference.setColorPreferenceKey("errorIndicationColor");
		annotationPreference.setColorPreferenceValue(new RGB(255, 0, 0));
		annotationPreference.setPresentationLayer(6);
		return annotationPreference;
	}
	
	private Menu createContextMenu(final LineNumberRulerColumn lineNumberColumn, final ParameterizedCommand saveAsCommand) {
		final Menu menu = new Menu(viewer.getControl());
		final MenuItem saveAsItem = new MenuItem(menu, SWT.PUSH);
		saveAsItem.setText("Save As");
		saveAsItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				handlerService.executeHandler(saveAsCommand); 
			}
		});
		
		final MenuItem breakpointItem = new MenuItem(menu, SWT.PUSH);
		breakpointItem.setText("Add breakpoint");
		breakpointItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                try {
                    final int line = viewer.getTextWidget().getLineAtOffset(viewer.getTextWidget().getSelection().x);
                    final RobotLineBreakpoint lineBreakpoint = new RobotLineBreakpoint((IResource) input.getAdapter(IResource.class), line+1);
                    DebugPlugin.getDefault().getBreakpointManager().addBreakpoint(lineBreakpoint);
                } catch (final CoreException e1) {
                    e1.printStackTrace();
                }
            }
        });
		
		new MenuItem(menu, SWT.SEPARATOR);
		
		final MenuItem toggleLineNumberColumnItem = new MenuItem(menu, SWT.CHECK);
		toggleLineNumberColumnItem.setSelection(true);
		toggleLineNumberColumnItem.setText("Show Line Numbers");
        toggleLineNumberColumnItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				if(toggleLineNumberColumnItem.getSelection()) {
					viewer.addVerticalRulerColumn(lineNumberColumn);
				} else {
					viewer.removeVerticalRulerColumn(lineNumberColumn);
				}
			}
		});
		return menu;
	}
	
	private ContentAssistant createContentAssistant() {
		final ContentAssistant contentAssistant = new ContentAssistant();
		contentAssistant.enableAutoInsert(true);
		contentAssistant.enablePrefixCompletion(true);
		contentAssistant.enableAutoActivation(true);
		contentAssistant.setShowEmptyList(true);
		contentAssistant.setContentAssistProcessor(new TextEditorContentAssistProcessor(), IDocument.DEFAULT_CONTENT_TYPE);
		contentAssistant.setEmptyMessage("No proposals");
		contentAssistant.setInformationControlCreator(new AbstractReusableInformationControlCreator() {
            @Override
            protected IInformationControl doCreateInformationControl(final Shell parent) {
                return new DefaultInformationControl(parent, true);
            }
        });
		return contentAssistant;
	}
	
	private PresentationReconciler createPresentationReconciler() {
		final PresentationReconciler reconciler = new PresentationReconciler();
		final TxtScanner txtScanner = new TxtScanner();
		final DefaultDamagerRepairer dr = new DefaultDamagerRepairer(txtScanner);
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);
		return reconciler;
	}
}
