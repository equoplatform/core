package com.make.equo.renderers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.e4.core.commands.ECommandService;
import org.eclipse.e4.core.commands.EHandlerService;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.MUILabel;
import org.eclipse.e4.ui.model.application.ui.menu.MToolBar;
import org.eclipse.e4.ui.model.application.ui.menu.MToolBarElement;
import org.eclipse.e4.ui.workbench.renderers.swt.ToolBarManagerRenderer;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.make.equo.application.api.IEquoApplication;
import com.make.equo.server.api.IEquoServer;
import com.make.equo.ws.api.EquoEventHandler;
import com.make.swtcef.Chromium;

public class ToolBarRenderer extends ToolBarManagerRenderer implements IEquoRenderer {

	static final String EQUO_RENDERERS_NAME = "toolbar";
	static final String EQUO_RENDERERS_URL = EQUO_RENDERERS_URL_PREFIX + EQUO_RENDERERS_NAME
			+ EQUO_RENDERERS_URL_SUFFIX;

	private static final String UNKNOWN_EQUO_COMMAND = "unknownEquoCommand";

	@Inject
	private IEquoApplication equoApplication;

	@Inject
	private static EquoEventHandler equoEventHandler;

	@Inject
	private IEquoServer equoProxyServer;

	@Inject
	private MApplication mApplication;

	@Inject
	private UISynchronize sync;

	private MToolBar mToolBar;

	private String namespace;

	@Override
	public Object createWidget(MUIElement mUIToolbar, Object parent) {
		if (!(mUIToolbar instanceof MToolBar) || !(parent instanceof Composite)) {
			return null;
		}
		this.mToolBar = (MToolBar) mUIToolbar;
		this.namespace = "ToolBar" + Integer.toHexString(mUIToolbar.hashCode());

		Composite parentComp = (Composite) parent;

		Composite browserComposite = new Composite(parentComp, 1000);

		configureAndStartRenderProcess(browserComposite);

		return parentComp;
	}

	@Override
	public IEquoServer getEquoProxyServer() {
		return equoProxyServer;
	}

	@Override
	public String getNamespace() {
		return namespace;
	}

	@Override
	public EquoEventHandler getEquoEventHandler() {
		return equoEventHandler;
	}

	@Override
	public List<String> getJsFileNamesForRendering() {
		return Lists.newArrayList("renderers/toolBarRenderer.js");
	}

	@Override
	public List<String> getFrameworkContributionJSONFileNames() {
		return Lists.newArrayList(getModelContributionPath() + "IDEMainToolBarContribution.json");
	}

	@Override
	public Chromium createBrowserComponent(Composite toolBarParent) {
		Chromium.setCommandLine(new String[][] { new String[] { "proxy-server", "localhost:9896" },
				new String[] { "ignore-certificate-errors", null },
				new String[] { "allow-file-access-from-files", null }, new String[] { "disable-web-security", null },
				new String[] { "enable-widevine-cdm", null }, new String[] { "proxy-bypass-list", "127.0.0.1" } });

		GridLayoutFactory.fillDefaults().applyTo(toolBarParent);

		toolBarParent.getHorizontalBar().setVisible(false);
		toolBarParent.getVerticalBar().setVisible(false);
		Chromium browser = new Chromium(toolBarParent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).hint(2000, 24).applyTo(browser);

		return browser;
	}

	@Override
	public void onActionPerformedOnElement() {
		getEquoEventHandler().on(namespace + "_itemClicked", (JsonObject payload) -> {
			JsonElement commandAsJson = payload.get("commandId");
			String commandId = commandAsJson.getAsString();

			if (UNKNOWN_EQUO_COMMAND.equals(commandId)) {
				// TODO Do a real log of the toolBarElement id which does not have an associated
				// command
				String toolBarElementId = payload.get("toolBarElementId").getAsString();
				System.out.println("There is no command for the toolBar element " + toolBarElementId);
				return;
			}

			IEclipseContext eclipseContext = mApplication.getContext();
			ECommandService commandService = eclipseContext.get(ECommandService.class);
			Command command = commandService.getCommand(commandId);

			if (command != null) {
				sync.asyncExec(() -> {
					ParameterizedCommand parametrizedCommand = new ParameterizedCommand(command, null);
					EHandlerService handlerService = eclipseContext.get(EHandlerService.class);
					handlerService.executeHandler(parametrizedCommand);
				});
			}
		});
	}

	@Override
	public List<Map<String, String>> getEclipse4Model(String namespace) {
		List<Map<String, String>> e4Model = new ArrayList<Map<String, String>>();
		List<MToolBarElement> mToolBarElements = mToolBar.getChildren();

		List<MToolBarElement> toolBarElementsToRendered = mToolBarElements.stream().filter(toolItem -> {
			return toolItem.isToBeRendered() && toolItem.isVisible();
		}).collect(Collectors.toList());

		for (MToolBarElement mToolBarElement : toolBarElementsToRendered) {
			if (mToolBarElement instanceof MUILabel) {
				HashMap<String, String> elementModel = new HashMap<String, String>();
				String elementId = mToolBarElement.getElementId();
				elementModel.put("id", elementId);
				e4Model.add(elementModel);
			}
		}
		return e4Model;
	}

	@Override
	public IEquoApplication getEquoApplication() {
		return equoApplication;
	}

	@Override
	public String getModelContributionPath() {
		return "contributions/mainToolBar/";
	}

	@Override
	public String getEquoRendererURL() {
		return EQUO_RENDERERS_URL;
	}

}