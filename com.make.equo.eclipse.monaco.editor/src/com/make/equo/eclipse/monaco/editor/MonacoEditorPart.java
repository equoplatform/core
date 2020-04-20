package com.make.equo.eclipse.monaco.editor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.make.equo.monaco.EquoMonacoEditor;
import com.make.equo.monaco.EquoMonacoEditorBuilder;
import com.make.equo.ws.api.IEquoRunnable;

public class MonacoEditorPart extends EditorPart {

	private volatile boolean isDirty = false;

	private IEquoRunnable<Boolean> dirtyListener = (isDirty) -> {
		this.isDirty = isDirty;
		firePropertyChange(PROP_DIRTY);
	};

	private EquoMonacoEditor editor;

	private ISelectionProvider selectionProvider = new MonacoEditorSelectionProvider();

	private EditorAction undoAction;
	private EditorAction redoAction;
	private EditorAction selectAllAction;
	private EditorAction copyAction;
	private EditorAction cutAction;
	private EditorAction pasteAction;
	private EditorAction findAction;

	@Override
	public void doSave(IProgressMonitor monitor) {
		editor.getContentsSync((editorContents) -> {
			IEditorInput input = getEditorInput();
			if (input instanceof FileEditorInput) {
				Display.getDefault().asyncExec(() -> {
					try {
						((FileEditorInput) input).getFile().setContents(
								new ByteArrayInputStream(editorContents.getBytes(Charset.forName("UTF-8"))), true,
								false, monitor);
						editor.handleAfterSave();
					} catch (CoreException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				});
			}
		});

	}

	@Override
	public void doSaveAs() {
		Shell shell = PlatformUI.getWorkbench().getModalDialogShellProvider().getShell();
		final IEditorInput input = getEditorInput();
		final IEditorInput newInput;

		SaveAsDialog dialog = new SaveAsDialog(shell);

		IFile original = (input instanceof IFileEditorInput) ? ((IFileEditorInput) input).getFile() : null;
		if (original != null)
			dialog.setOriginalFile(original);
		else
			dialog.setOriginalName(input.getName());

		dialog.create();

		if (dialog.open() == Window.CANCEL) {
			return;
		}

		IPath filePath = dialog.getResult();
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IFile file = workspace.getRoot().getFile(filePath);
		newInput = new FileEditorInput(file);
		try {
			file.getLocation().toFile().createNewFile();
			file.getParent().refreshLocal(1, new NullProgressMonitor());
		} catch (IOException | CoreException e) {
			e.printStackTrace();
			return;
		}

		setInput(newInput);
		setPartName(file.getName());
		doSave(new NullProgressMonitor());
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setInput(input);
		setSite(site);

	}

	@Override
	public boolean isDirty() {
		return isDirty;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}

	@Override
	public void createPartControl(Composite parent) {
		IEditorInput input = getEditorInput();
		setPartName(input.getName());
		if (input instanceof FileEditorInput) {
			FileEditorInput fileInput = (FileEditorInput) input;
			setTitleToolTip(fileInput.getPath().toString());

			try (InputStream contents = fileInput.getFile().getContents()) {
				int singleByte;
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				while ((singleByte = contents.read()) != -1) {
					baos.write(singleByte);
					;
				}

				String textContent = new String(baos.toByteArray());

				try {
					BundleContext bndContext = FrameworkUtil.getBundle(EquoMonacoEditorBuilder.class)
							.getBundleContext();
					ServiceReference<EquoMonacoEditorBuilder> svcReference = bndContext
							.getServiceReference(EquoMonacoEditorBuilder.class);
					EquoMonacoEditorBuilder builder = bndContext.getService(svcReference);
					editor = builder.withParent(parent).withStyle(parent.getStyle()).withContents(textContent)
							.withFileName(fileInput.getURI().toString()).create();

					editorConfigs();

					getSite().setSelectionProvider(selectionProvider);

					createActions();
					activateActions();
				} catch (Exception e) {
					System.out.println("Couldn't retrieve Monaco Editor service");
				}
			} catch (CoreException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}

	private void editorConfigs() {
		IEquoRunnable<Boolean> redoListener = (canRedo) -> {
			redoAction.setEnabled(canRedo);
		};
		IEquoRunnable<Boolean> undoListener = (canUndo) -> {
			undoAction.setEnabled(canUndo);
		};
		editor.subscribeChanges(dirtyListener, undoListener, redoListener);

		editor.configSelection((selection) -> {
			Display.getDefault().asyncExec(() -> {
				ISelection iSelection = (selection) ? new TextSelection(0, 1) : new TextSelection(0, -1);
				selectionProvider.setSelection(iSelection);
			});
		});
	}

	private void createActions() {
		undoAction = new EditorAction(() -> editor.undo());
		redoAction = new EditorAction(() -> editor.redo());
		copyAction = new EditorAction(() -> editor.copy(), selectionProvider);
		cutAction = new EditorAction(() -> editor.cut(), selectionProvider);
		pasteAction = new EditorAction(() -> editor.paste(), null, () -> {
			Clipboard clipboard = new Clipboard(Display.getCurrent());
			TextTransfer textTransfer = TextTransfer.getInstance();
			String textData = (String) clipboard.getContents(textTransfer);
			clipboard.dispose();
			return (textData != null);
		});
		findAction = new EditorAction(() -> editor.find());
		findAction.setEnabled(true);
		selectAllAction = new EditorAction(() -> editor.selectAll());
		selectAllAction.setEnabled(true);
	}

	private void activateActions() {
		IActionBars actionBars = getEditorSite().getActionBars();
		actionBars.setGlobalActionHandler(ActionFactory.UNDO.getId(), undoAction);
		actionBars.setGlobalActionHandler(ActionFactory.REDO.getId(), redoAction);
		actionBars.setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(), selectAllAction);
		actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(), copyAction);
		actionBars.setGlobalActionHandler(ActionFactory.CUT.getId(), cutAction);
		actionBars.setGlobalActionHandler(ActionFactory.PASTE.getId(), pasteAction);
		actionBars.setGlobalActionHandler(ActionFactory.FIND.getId(), findAction);
		actionBars.updateActionBars();
	}

	@Override
	public void setFocus() {
		getSite().setSelectionProvider(selectionProvider);
		activateActions();
	}

	@Override
	public void dispose() {
		super.dispose();
		editor.dispose();
	}

}
