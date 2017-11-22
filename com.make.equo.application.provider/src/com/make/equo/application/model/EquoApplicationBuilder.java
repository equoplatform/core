package com.make.equo.application.model;

import java.util.List;

import org.eclipse.e4.ui.model.application.MAddon;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.MApplicationFactory;
import org.eclipse.e4.ui.model.application.commands.MBindingContext;
import org.eclipse.e4.ui.model.application.commands.MBindingTable;
import org.eclipse.e4.ui.model.application.commands.MCommandParameter;
import org.eclipse.e4.ui.model.application.commands.MCommandsFactory;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimmedWindow;
import org.eclipse.e4.ui.model.application.ui.menu.MMenu;
import org.eclipse.e4.ui.model.application.ui.menu.impl.MenuFactoryImpl;
import com.google.common.collect.Lists;
import com.make.equo.application.addon.EquoProxyServerAddon;
import com.make.equo.application.addon.EquoWebSocketServerAddon;
import com.make.equo.application.impl.HandlerBuilder;
import com.make.equo.application.util.IConstants;

public class EquoApplicationBuilder {

	private OptionalFieldBuilder optionalBuilder;
	private EquoApplication equoApplication;
	private final MApplication mApplication;
	private MTrimmedWindow mWindow;
	private UrlMandatoryBuilder urlMandatoryFieldBuilder;
	private String name;
	private MAddon proxyServerAddon;
	private MAddon webSocketServerAddon;

	EquoApplicationBuilder(EquoApplication equoApplication) {
		this.equoApplication = equoApplication;
		this.mApplication = equoApplication.getEquoApplicationModel().getMainApplication();
		this.urlMandatoryFieldBuilder = new UrlMandatoryBuilder(this);
		this.optionalBuilder = new OptionalFieldBuilder(this);
	}

	public UrlMandatoryBuilder name(String name) {
		this.name = name;
		String appId = IConstants.EQUO_APP_PREFIX + "." + name.trim().toLowerCase();
		this.mWindow = (MTrimmedWindow) getmApplication().getChildren().get(0);
		getmWindow().setLabel(name);
		MMenu mainMenu = MenuFactoryImpl.eINSTANCE.createMenu();
		mainMenu.setElementId(appId + "." + "mainmenu");
		getmWindow().setMainMenu(mainMenu);

		createDefaultBindingContexts();

		MBindingTable mainWindowBindingTable = MCommandsFactory.INSTANCE.createBindingTable();
		mainWindowBindingTable.setBindingContext(getmApplication().getBindingContexts().get(0));
		mainWindowBindingTable.setElementId(IConstants.DEFAULT_BINDING_TABLE);

		addAppLevelCommands(getmApplication());

		getmApplication().getBindingTables().add(mainWindowBindingTable);

		getmApplication().getAddons().add(createWebSocketServerAddon());
		getmApplication().getAddons().add(createProxyServerAddon());

		return this.getUrlMandatoryFieldBuilder();
	}

	private void addAppLevelCommands(MApplication mApplication) {
		createWebSocketTriggeredCommand(mApplication, IConstants.EQUO_WEBSOCKET_OPEN_BROWSER,
				IConstants.OPEN_BROWSER_COMMAND_CONTRIBUTION_URI);
		createOpenBrowserAsWindow(mApplication, IConstants.EQUO_OPEN_BROWSER_AS_WINDOW,
				IConstants.OPEN_BROWSER_AS_WINDOW_COMMAND_CONTRIBUTION_URI);
		createOpenBrowserAsSidePart(mApplication, IConstants.EQUO_OPEN_BROWSER_AS_SIDE_PART,
				IConstants.OPEN_BROWSER_AS_SIDE_PART_COMMAND_CONTRIBUTION_URI);
	}

	private void createOpenBrowserAsSidePart(MApplication mApplication, String commandId,
			String commandContributionUri) {
		HandlerBuilder handlerBuilder = new HandlerBuilder(mApplication, commandId, commandContributionUri) {
			@Override
			protected Runnable getRunnable() {
				return null;
			}

			@Override
			protected List<MCommandParameter> createCommandParameters() {
				MCommandParameter partNameCommandParameter = createCommandParameter(IConstants.EQUO_BROWSER_PART_NAME,
						"Part Name", true);
				MCommandParameter partPositionCommandParameter = createCommandParameter(
						IConstants.EQUO_BROWSER_PART_POSITION, "Part Position", true);
				return Lists.newArrayList(partNameCommandParameter, partPositionCommandParameter);
			}

		};
		handlerBuilder.createCommandAndHandler(commandId);
	}

	private void createOpenBrowserAsWindow(MApplication mApplication, String commandId, String commandContributionUri) {
		HandlerBuilder handlerBuilder = new HandlerBuilder(mApplication, commandId, commandContributionUri) {
			@Override
			protected Runnable getRunnable() {
				return null;
			}

			@Override
			protected List<MCommandParameter> createCommandParameters() {
				MCommandParameter windowNameCommandParameter = createCommandParameter(
						IConstants.EQUO_BROWSER_WINDOW_NAME, "Browser Window Name", true);
				return Lists.newArrayList(windowNameCommandParameter);
			}
		};
		handlerBuilder.createCommandAndHandler(commandId);
	}

	private void createWebSocketTriggeredCommand(MApplication mApplication, String commandId,
			String commandContributionUri) {
		HandlerBuilder handlerBuilder = new HandlerBuilder(mApplication, commandId, commandContributionUri) {
			@Override
			protected Runnable getRunnable() {
				return null;
			}
		};
		handlerBuilder.createCommandAndHandler(commandId);
	}

	private MCommandParameter createCommandParameter(String id, String name, boolean isOptional) {
		MCommandParameter commandParameter = MCommandsFactory.INSTANCE.createCommandParameter();
		commandParameter.setElementId(id);
		commandParameter.setName(name);
		commandParameter.setOptional(isOptional);
		return commandParameter;
	}

	private MAddon createWebSocketServerAddon() {
		webSocketServerAddon = MApplicationFactory.INSTANCE.createAddon();
		webSocketServerAddon.setContributionURI((EquoWebSocketServerAddon.CONTRIBUTION_URI));
		webSocketServerAddon.setElementId(IConstants.EQUO_WEBSOCKET_SERVER_ADDON);
		return webSocketServerAddon;
	}

	private MAddon createProxyServerAddon() {
		proxyServerAddon = MApplicationFactory.INSTANCE.createAddon();
		proxyServerAddon.setContributionURI((EquoProxyServerAddon.CONTRIBUTION_URI));
		proxyServerAddon.setElementId(IConstants.EQUO_PROXY_SERVER_ADDON);
		return proxyServerAddon;
	}

	private void createDefaultBindingContexts() {
		MBindingContext windowAndDialogBindingContext = MCommandsFactory.INSTANCE.createBindingContext();
		windowAndDialogBindingContext.setElementId(IConstants.DIALOGS_AND_WINDOWS_BINDING_CONTEXT);
		windowAndDialogBindingContext.setName("Dialogs and Windows Binding Context");

		MBindingContext windowBindingContext = MCommandsFactory.INSTANCE.createBindingContext();
		windowBindingContext.setElementId(IConstants.WINDOWS_BINDING_CONTEXT);
		windowBindingContext.setName("Windows Binding Context");

		MBindingContext dialogBindingContext = MCommandsFactory.INSTANCE.createBindingContext();
		dialogBindingContext.setElementId(IConstants.DIALOGS_BINDING_CONTEXT);
		dialogBindingContext.setName("Dialogs Binding Context");

		// keep this order, the order in which they are added is important.
		getmApplication().getBindingContexts().add(windowAndDialogBindingContext);
		getmApplication().getBindingContexts().add(windowBindingContext);
		getmApplication().getBindingContexts().add(dialogBindingContext);
	}

	OptionalFieldBuilder getOptionalBuilder() {
		return optionalBuilder;
	}

	UrlMandatoryBuilder getUrlMandatoryFieldBuilder() {
		return urlMandatoryFieldBuilder;
	}

	MApplication getmApplication() {
		return mApplication;
	}

	EquoApplication getEquoApplication() {
		return equoApplication;
	}

	MTrimmedWindow getmWindow() {
		return mWindow;
	}

	String getName() {
		return name;
	}

	MAddon getEquoProxyServerAddon() {
		return proxyServerAddon;
	}

}