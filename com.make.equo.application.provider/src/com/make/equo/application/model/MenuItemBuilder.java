package com.make.equo.application.model;

import org.eclipse.e4.ui.model.application.ui.menu.MHandledMenuItem;
import org.eclipse.e4.ui.model.application.ui.menu.MMenu;
import org.eclipse.e4.ui.model.application.ui.menu.impl.MenuFactoryImpl;

public class MenuItemBuilder {

	MHandledMenuItem menuItem;
	MenuBuilder menuBuilder;

	public MenuItemBuilder(MenuBuilder menuBuilder) {
		this.menuBuilder = menuBuilder;
	}
	
	public MenuItemBuilder(MenuItemBuilder menuItemBuilder) {
		this.menuItem = menuItemBuilder.menuItem;
		this.menuBuilder = menuItemBuilder.menuBuilder;
	}

	public MenuItemBuilder addMenuItem(String menuItemLabel) {
		menuItem = createMenuItem(menuItemLabel);
		return new MenuItemBuilder(this);
	}

	private MHandledMenuItem createMenuItem(String menuItemlabel) {
		MHandledMenuItem newMenuItem = MenuFactoryImpl.eINSTANCE.createHandledMenuItem();
		MMenu parentMenu = menuBuilder.menu;
		String menuItemId = parentMenu.getElementId() + "." + menuItemlabel.replaceAll("\\s+", "").toLowerCase();
		newMenuItem.setElementId(menuItemId);
		newMenuItem.setLabel(menuItemlabel);
		parentMenu.getChildren().add(newMenuItem);
		return newMenuItem;
	}
	
	public MenuItemBuilder onClick(Runnable runnable) {
		return new HandlerBuilder(this).onClick(runnable);
	}
	
	public MenuBuilder addMenu(String menuLabel) {
		return new MenuBuilder(this.menuBuilder).addMenu(menuLabel);
	}
}
