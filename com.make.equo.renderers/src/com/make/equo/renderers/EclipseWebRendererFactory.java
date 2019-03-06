package com.make.equo.renderers;

import org.eclipse.e4.ui.internal.workbench.swt.AbstractPartRenderer;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.model.application.ui.menu.MToolBar;
import org.eclipse.e4.ui.workbench.renderers.swt.WorkbenchRendererFactory;

import com.make.equo.ui.helper.provider.model.MWebDialog;

public class EclipseWebRendererFactory extends WorkbenchRendererFactory {

	private static final String EQUO_MAIN_TOOLBAR = "com.make.equo.main.toolbar";
	protected WebItemStackRenderer stackRenderer;
	protected String id;
	private ToolBarRenderer toolBarRenderer;
	private WebDialogRenderer webDialogRenderer;
	
	@Override
	public AbstractPartRenderer getRenderer(MUIElement uiElement, Object parent) {

		if (isEquoMainToolBar(uiElement)) {
			if (toolBarRenderer == null) {
				toolBarRenderer = new ToolBarRenderer();
				super.initRenderer(toolBarRenderer);
			}
			return toolBarRenderer;
		} else if (uiElement instanceof MWebDialog) {
		   if (webDialogRenderer == null) {
		      webDialogRenderer = new WebDialogRenderer();
		      super.initRenderer(webDialogRenderer);
		   }
		   return webDialogRenderer;
		}
		else if (uiElement instanceof MPartStack) {
			if (stackRenderer == null) {
				stackRenderer = new WebItemStackRenderer();
				super.initRenderer(stackRenderer);
			}
			return stackRenderer;
		}
		return super.getRenderer(uiElement, parent);
	}

	private boolean isEquoMainToolBar(MUIElement mUIToolbar) {
		return (mUIToolbar instanceof MToolBar) && EQUO_MAIN_TOOLBAR.equals(((MToolBar) mUIToolbar).getElementId());
	}
}