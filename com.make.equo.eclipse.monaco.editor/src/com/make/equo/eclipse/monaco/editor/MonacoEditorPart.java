package com.make.equo.eclipse.monaco.editor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.handlers.IHandlerService;
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
		Shell shell= PlatformUI.getWorkbench().getModalDialogShellProvider().getShell();
		final IEditorInput input= getEditorInput();
		final IEditorInput newInput;
		
		SaveAsDialog dialog= new SaveAsDialog(shell);

		IFile original= (input instanceof IFileEditorInput) ? ((IFileEditorInput) input).getFile() : null;
		if (original != null)
			dialog.setOriginalFile(original);
		else
			dialog.setOriginalName(input.getName());

		dialog.create();
		
		if (dialog.open() == Window.CANCEL) {
			return;
		}
		
		IPath filePath= dialog.getResult();
		IWorkspace workspace= ResourcesPlugin.getWorkspace();
		IFile file= workspace.getRoot().getFile(filePath);
		newInput= new FileEditorInput(file);
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
					baos.write(singleByte);;
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
		editor.subscribeIsDirty(dirtyListener);
		
		editor.configSave((v) -> {
			Display.getDefault().asyncExec( () -> {
				IHandlerService handlerService = (IHandlerService) getSite().getService(IHandlerService.class);
				try {
				    handlerService.executeCommand("org.eclipse.ui.file.save", null);
				} catch (Exception ex) {}
			});
		});
			
		editor.configSelection((selection) -> {
			Display.getDefault().asyncExec( () -> {
				ISelection iSelection = (selection) ? new TextSelection(0, 1) : new TextSelection(0, -1);
				getSite().getSelectionProvider().setSelection(iSelection);
			});
		});
	}

	private void createActions() {
		IHandlerService handlerService = (IHandlerService) getSite().getService(IHandlerService.class);
		
		IHandler handler = new ActionHandler(selectionProvider, () -> editor.copy());
		handlerService.activateHandler(IWorkbenchCommandConstants.EDIT_COPY, handler);
		
		handler = new ActionHandler(selectionProvider, () -> editor.cut());
		handlerService.activateHandler(IWorkbenchCommandConstants.EDIT_CUT, handler);
		
		handler = new ActionHandler(selectionProvider, () -> editor.find(), true);
		handlerService.activateHandler(IWorkbenchCommandConstants.EDIT_FIND_AND_REPLACE, handler);
	}

	@Override
	public void setFocus() {
		
	}

}
