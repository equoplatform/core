package com.make.equo.node.packages.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.awaitility.Awaitility.await;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import org.eclipse.swt.SWT;
import org.eclipse.swt.chromium.Browser;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.make.equo.aer.api.IEquoLoggingService;
import com.make.equo.aer.client.api.ILoggingApi;
import com.make.equo.analytics.client.api.IAnalyticsApi;
import com.make.equo.analytics.internal.api.AnalyticsService;
import com.make.equo.application.api.IEquoApplication;
import com.make.equo.application.model.CustomDeserializer;
import com.make.equo.application.model.EquoMenu;
import com.make.equo.application.model.EquoMenuItem;
import com.make.equo.application.model.EquoMenuItemSeparator;
import com.make.equo.application.model.EquoMenuModel;
import com.make.equo.node.packages.tests.common.ChromiumSetup;
import com.make.equo.node.packages.tests.mocks.AnalyticsServiceMock;
import com.make.equo.node.packages.tests.mocks.LoggingServiceMock;
import com.make.equo.server.api.IEquoServer;
import com.make.equo.testing.common.util.EquoRule;
import com.make.equo.ws.api.IEquoEventHandler;
import com.make.equo.ws.api.IEquoRunnable;
import com.make.equo.ws.api.IEquoWebSocketService;
import com.make.equo.ws.api.JsonPayloadEquoRunnable;

public class PackagesIntegrationTest {

	@Inject
	protected IAnalyticsApi analyticsApi;

	@Inject
	protected ILoggingApi loggingApi;

	@Inject
	protected IEquoApplication equoApp;

	@Inject
	protected IEquoServer equoServer;

	@Inject
	protected AnalyticsService analyticsServiceMock;

	@Inject
	protected IEquoLoggingService loggingServiceMock;

	@Inject
	protected IEquoWebSocketService websocketService;

	@Inject
	protected IEquoEventHandler handler;

	protected Browser chromium;

	private Display display;
	private static Gson gson;

	@Rule
	public EquoRule rule = new EquoRule(this).runInNonUIThread();

	@BeforeClass
	public static void inicialize() {
		CustomDeserializer deserializer = new CustomDeserializer();
		deserializer.registerMenuType(EquoMenuItem.CLASSNAME, EquoMenuItem.class);
		deserializer.registerMenuType(EquoMenuItemSeparator.CLASSNAME, EquoMenuItemSeparator.class);

		gson = new GsonBuilder().registerTypeAdapter(EquoMenuModel.class, deserializer).create();

		new ChromiumSetup();
	}

	@Before
	public void before() {
		AtomicBoolean start = new AtomicBoolean(false);
		handler.on("_ready", (IEquoRunnable<Void>) runnable -> {
			start.set(true);
		});

		display = rule.getDisplay();
		display.syncExec(() -> {
			Shell shell = rule.createShell();
			chromium = new Browser(shell, SWT.NONE);
			GridData data = new GridData();
			data.minimumWidth = 1;
			data.minimumHeight = 1;
			data.horizontalAlignment = SWT.FILL;
			data.verticalAlignment = SWT.FILL;
			data.grabExcessHorizontalSpace = true;
			data.grabExcessVerticalSpace = true;
			chromium.setLayoutData(data);
			chromium.setUrl("http://testbundles/" + String.format("?equowsport=%d", websocketService.getPort()));
			shell.open();
		});

		await().untilTrue(start);
	}

	@Test
	public void analyticsAreReceivedCorrectlyByTheService() {
		await().untilAsserted(() -> {
			assertThat(analyticsServiceMock).isInstanceOf(AnalyticsServiceMock.class).extracting("receivedMessages")
					.asInstanceOf(list(String.class)).contains("testEvent-{\"testKey\":\"testValue\"}");
		});
	}

	@Test
	public void loggingMessagesAreReceivedCorrectlyByTheService() {
		handler.send("_makeLogs");
		await().untilAsserted(() -> {
			assertThat(loggingServiceMock).isInstanceOf(LoggingServiceMock.class).extracting("receivedMessages")
					.asInstanceOf(list(String.class)).hasSize(3).contains("testInfo", "testWarn", "testError");
		});
	}

	private void testMenuTemplate(String userActionOn, String userActionSend, String json) {
		AtomicReference<Boolean> wasCorrectly = new AtomicReference<>(false);
		handler.on(userActionOn, (JsonPayloadEquoRunnable) payload -> {
			if (payload.get("code") != null) {
				wasCorrectly.set(payload.toString().contains(json));
			} else {
				wasCorrectly.set(gson.fromJson(payload, EquoMenuModel.class).serialize().toString().equals(json));
			}
		});
		handler.send(userActionSend);
		await().until(() -> wasCorrectly.get());
	}

	private EquoMenuModel createTestMenuModel() {
		EquoMenuModel equoMenuModel = new EquoMenuModel();
		EquoMenu menu1 = new EquoMenu("Menu1");

		EquoMenuItem subMenu11 = new EquoMenuItem("SubMenu11");
		subMenu11.setShortcut("M1+W");
		subMenu11.setAction("_test");

		menu1.addItem(subMenu11);
		equoMenuModel.addMenu(menu1);

		EquoMenu menu2 = new EquoMenu("Menu2");

		EquoMenuItem equoMenuItem2 = new EquoMenuItem("SubMenu21");
		equoMenuItem2.setAction("_test");

		menu2.addItem(equoMenuItem2);

		EquoMenuItemSeparator equoMenuItemSeparator = new EquoMenuItemSeparator();
		menu2.addItem(equoMenuItemSeparator);

		EquoMenu subMenu22 = new EquoMenu("SubMenu22");

		EquoMenuItem subMenu221 = new EquoMenuItem("SubMenu221");
		subMenu221.setShortcut("M1+G");
		subMenu221.setAction("_test");

		subMenu22.addItem(subMenu221);

		EquoMenu subMenu222 = new EquoMenu("SubMenu222");

		EquoMenuItem subMenu2221 = new EquoMenuItem("SubMenu2221");

		subMenu222.addItem(subMenu2221);

		subMenu22.addItem(subMenu222);
		menu2.addItem(subMenu22);

		equoMenuModel.addMenu(menu2);

		return equoMenuModel;
	}

	@Test
	public void createEquoMenu() {
		testMenuTemplate("_testSetMenu1", "_createMenu",
				"{\"menus\":[{\"type\":\"EquoMenu\",\"title\":\"Menu1\",\"children\":[{\"type\":\"EquoMenuItem\""
						+ ",\"title\":\"SubMenu11\",\"shortcut\":\"M1+W\",\"action\":\"_test\"},{\"type\":\"EquoMenu\",\"title\":\"SubMenu12\",\"children\":[{\""
						+ "type\":\"EquoMenuItem\",\"title\":\"SubMenu121\"}]}]},{\"type\":\"EquoMenu\",\"title\":\"Menu2\",\"children\":[{\"type\":\"EquoMenuItem\""
						+ ",\"title\":\"SubMenu21\",\"action\":\"_test\"},{\"type\":\"EquoMenuItemSeparator\"},{\"type\":\"EquoMenu\",\"title\":\"SubMenu22\",\""
						+ "children\":[{\"type\":\"EquoMenuItem\",\"title\":\"SubMenu221\",\"shortcut\":\"M1+G\",\"action\":\"_test\"},{\"type\":\"EquoMenu\",\""
						+ "title\":\"SubMenu222\",\"children\":[{\"type\":\"EquoMenuItem\",\"title\":\"SubMenu2221\"}]}]}]}]}");
	}

	@Test
	public void appendMenuItem() {
		testMenuTemplate("_testSetMenu2", "_appendMenuItem",
				"{\"menus\":[{\"type\":\"EquoMenu\",\"title\":\"Menu1\",\"children\":[{\"type\":\"EquoMenuItem\""
						+ ",\"title\":\"SubMenu10\",\"shortcut\":\"M1+L\",\"action\":\"_test\"},{\"type\":\"EquoMenuItem\",\"title\":\"SubMenu11\",\"shortcut\":\""
						+ "M1+W\",\"action\":\"_test\"},{\"type\":\"EquoMenu\",\"title\":\"SubMenu12\",\"children\":[{\"type\":\"EquoMenuItem\",\"title\":\""
						+ "SubMenu121\"}]}]},{\"type\":\"EquoMenu\",\"title\":\"Menu2\",\"children\":[{\"type\":\"EquoMenuItem\",\"title\":\"SubMenu21\",\"action\""
						+ ":\"_test\"},{\"type\":\"EquoMenuItemSeparator\"},{\"type\":\"EquoMenu\",\"title\":\"SubMenu22\",\"children\":[{\"type\":\"EquoMenuItem\""
						+ ",\"title\":\"SubMenu221\",\"shortcut\":\"M1+G\",\"action\":\"_test\"},{\"type\":\"EquoMenu\",\"title\":\"SubMenu222\",\"children\":[{\""
						+ "type\":\"EquoMenuItem\",\"title\":\"SubMenu2221\"}]}]}]}]}");
	}

	@Test
	public void appendMenu() {
		testMenuTemplate("_testSetMenu3", "_appendMenu",
				"{\"menus\":[{\"type\":\"EquoMenu\",\"title\":\"Menu1\",\"children\":[{\"type\":\"EquoMenuItem\",\""
						+ "title\":\"SubMenu11\",\"shortcut\":\"M1+W\",\"action\":\"_test\"},{\"type\":\"EquoMenu\",\"title\":\"SubMenu12\",\"children\":[{\"type\":\""
						+ "EquoMenuItem\",\"title\":\"SubMenu121\"}]}]},{\"type\":\"EquoMenu\",\"title\":\"Menu2\",\"children\":[{\"type\":\"EquoMenuItem\",\"title\""
						+ ":\"SubMenu21\",\"action\":\"_test\"},{\"type\":\"EquoMenuItemSeparator\"},{\"type\":\"EquoMenu\",\"title\":\"SubMenu22\",\"children\":[{\""
						+ "type\":\"EquoMenuItem\",\"title\":\"SubMenu221\",\"shortcut\":\"M1+G\",\"action\":\"_test\"},{\"type\":\"EquoMenu\",\"title\":\"SubMenu223\""
						+ ",\"children\":[{\"type\":\"EquoMenuItem\",\"title\":\"SubMenu2231\",\"shortcut\":\"M1+K\",\"action\":\"_test\"}]},{\"type\":\"EquoMenu\",\""
						+ "title\":\"SubMenu222\",\"children\":[{\"type\":\"EquoMenuItem\",\"title\":\"SubMenu2221\"}]}]}]}]}");
	}

	@Test
	public void removeMenuElement() {
		testMenuTemplate("_testSetMenu4", "_removeMenuElement",
				"{\"menus\":[{\"type\":\"EquoMenu\",\"title\":\"Menu1\",\"children\":[{\"type\":\"EquoMenuItem\""
						+ ",\"title\":\"SubMenu11\",\"shortcut\":\"M1+W\",\"action\":\"_test\"},{\"type\":\"EquoMenu\",\"title\":\"SubMenu12\",\"children\":[{\"type\""
						+ ":\"EquoMenuItem\",\"title\":\"SubMenu121\"}]}]},{\"type\":\"EquoMenu\",\"title\":\"Menu2\",\"children\":[{\"type\":\"EquoMenuItem\",\"title\""
						+ ":\"SubMenu21\",\"action\":\"_test\"},{\"type\":\"EquoMenuItemSeparator\"},{\"type\":\"EquoMenu\",\"title\":\"SubMenu22\",\"children\":[{\"type\""
						+ ":\"EquoMenuItem\",\"title\":\"SubMenu221\",\"shortcut\":\"M1+G\",\"action\":\"_test\"}]}]}]}");
	}

	@Test
	public void appendMenuAtTheEnd() {
		testMenuTemplate("_testSetMenu5", "_appendMenuAtTheEnd",
				"{\"menus\":[{\"type\":\"EquoMenu\",\"title\":\"Menu1\",\"children\":[{\"type\":\"EquoMenuItem\""
						+ ",\"title\":\"SubMenu11\",\"shortcut\":\"M1+W\",\"action\":\"_test\"},{\"type\":\"EquoMenu\",\"title\":\"SubMenu12\",\"children\":[{\"type\":\""
						+ "EquoMenuItem\",\"title\":\"SubMenu121\"},{\"type\":\"EquoMenu\",\"title\":\"SubMenu122\",\"children\":[{\"type\":\"EquoMenuItem\",\"title\":\""
						+ "SubMenu1221\"}]}]}]},{\"type\":\"EquoMenu\",\"title\":\"Menu2\",\"children\":[{\"type\":\"EquoMenuItem\",\"title\":\"SubMenu21\",\"action\":\""
						+ "_test\"},{\"type\":\"EquoMenuItemSeparator\"},{\"type\":\"EquoMenu\",\"title\":\"SubMenu22\",\"children\":[{\"type\":\"EquoMenuItem\",\"title\""
						+ ":\"SubMenu221\",\"shortcut\":\"M1+G\",\"action\":\"_test\"},{\"type\":\"EquoMenu\",\"title\":\"SubMenu222\",\"children\":[{\"type\":\""
						+ "EquoMenuItem\",\"title\":\"SubMenu2221\"}]}]}]}]}");
	}

	@Test
	public void appendMenuItemAtTheEnd() {
		testMenuTemplate("_testSetMenu6", "_appendMenuItemAtTheEnd",
				"{\"menus\":[{\"type\":\"EquoMenu\",\"title\":\"Menu1\",\"children\":[{\"type\":\"EquoMenuI"
						+ "tem\",\"title\":\"SubMenu11\",\"shortcut\":\"M1+W\",\"action\":\"_test\"},{\"type\":\"EquoMenu\",\"title\":\"SubMenu12\",\"children\":[{\"typ"
						+ "e\":\"EquoMenuItem\",\"title\":\"SubMenu121\"},{\"type\":\"EquoMenuItem\",\"title\":\"SubMenu123\"}]}]},{\"type\":\"EquoMenu\",\"title\":\"Me"
						+ "nu2\",\"children\":[{\"type\":\"EquoMenuItem\",\"title\":\"SubMenu21\",\"action\":\"_test\"},{\"type\":\"EquoMenuItemSeparator\"},{\"type\":"
						+ "\"EquoMenu\",\"title\":\"SubMenu22\",\"children\":[{\"type\":\"EquoMenuItem\",\"title\":\"SubMenu221\",\"shortcut\":\"M1+G\",\"action\":\"_te"
						+ "st\"},{\"type\":\"EquoMenu\",\"title\":\"SubMenu222\",\"children\":[{\"type\":\"EquoMenuItem\",\"title\":\"SubMenu2221\"}]}]}]}]}");
	}

	@Test
	public void appendMenuRepeated() {
		testMenuTemplate("_testSetMenu7", "_appendMenuRepeated",
				"The menu SubMenu123 already exist in Menu1/SubMenu12");
	}

	@Test
	public void appendMenuItemRepeated() {
		testMenuTemplate("_testSetMenu8", "_appendMenuItemRepeated",
				"The menu SubMenu122 already exist in Menu1/SubMenu12");
	}

	@Test
	public void createMenuWithRepeatedMenus() {
		testMenuTemplate("_testSetMenu9", "_createMenuWithRepeatedMenus",
				"{\"menus\":[{\"type\":\"EquoMenu\",\"title\":\"Menu1\",\"children\":[{\"type\":\""
						+ "EquoMenuItem\",\"title\":\"SubMenu11\",\"shortcut\":\"M1+W\",\"action\":\"_test\"},{\"type\":\"EquoMenu\",\"title\":\"SubMenu12\",\"child"
						+ "ren\":[{\"type\":\"EquoMenuItem\",\"title\":\"SubMenu121\"}]},{\"type\":\"EquoMenuItem\",\"title\":\"SubMenu14\",\"shortcut\":\"M1+W\",\""
						+ "action\":\"_test\"}]},{\"type\":\"EquoMenu\",\"title\":\"Menu2\",\"children\":[{\"type\":\"EquoMenuItem\",\"title\":\"SubMenu21\",\"actio"
						+ "n\":\"_test\"},{\"type\":\"EquoMenuItemSeparator\"},{\"type\":\"EquoMenu\",\"title\":\"SubMenu22\",\"children\":[{\"type\":\"EquoMenuItem"
						+ "\",\"title\":\"SubMenu221\",\"shortcut\":\"M1+G\",\"action\":\"_test\"},{\"type\":\"EquoMenu\",\"title\":\"SubMenu222\",\"children\":[{\""
						+ "type\":\"EquoMenuItem\",\"title\":\"SubMenu2221\"},{\"type\":\"EquoMenuItem\",\"title\":\"newMenu\"}]}]}]}]}");
	}

	@Test
	public void buildWithCurrentModel() {
		handler.on("_getMenu", (JsonPayloadEquoRunnable) payload -> {
			handler.send("_doGetMenu", createTestMenuModel().serialize());
		});

		testMenuTemplate("_testSetMenu10", "_buildWithCurrentModel",
				"{\"menus\":[{\"type\":\"EquoMenu\",\"title\":\"Menu1\",\"children\":[{\"type\""
						+ ":\"EquoMenuItem\",\"title\":\"SubMenu11\",\"shortcut\":\"M1+W\",\"action\":\"_test\"}]},{\"type\":\"EquoMenu\",\"title\":\"Menu2\""
						+ ",\"children\":[{\"type\":\"EquoMenuItem\",\"title\":\"SubMenu21\",\"action\":\"_test\"},{\"type\":\"EquoMenuItemSeparator\"},{\"ty"
						+ "pe\":\"EquoMenu\",\"title\":\"SubMenu22\",\"children\":[{\"type\":\"EquoMenuItem\",\"title\":\"SubMenu221\",\"shortcut\":\"M1+G\""
						+ ",\"action\":\"_test\"},{\"type\":\"EquoMenu\",\"title\":\"SubMenu222\",\"children\":[{\"type\":\"EquoMenuItem\",\"title\":\"SubMen"
						+ "u2221\"}]}]}]},{\"type\":\"EquoMenu\",\"title\":\"Menu3\",\"children\":[{\"type\":\"EquoMenuItem\",\"title\":\"subMenu31\",\"short"
						+ "cut\":\"M1+W\",\"action\":\"_test\"},{\"type\":\"EquoMenu\",\"title\":\"subMenu32\",\"children\":[{\"type\":\"EquoMenuItem\",\"tit"
						+ "le\":\"subMenu321\"}]}]}]}");
	}

	@Test
	public void buildWithCurrentModelWithRepeatedMenus() {
		handler.on("_getMenu", (JsonPayloadEquoRunnable) payload -> {
			handler.send("_doGetMenu", createTestMenuModel().serialize());
		});

		testMenuTemplate("_testSetMenu11", "_buildWithCurrentModelWithRepeatedMenus",
				"The menu SubMenu22 already exist in Menu2 and your type is EquoMenu");
	}
}
