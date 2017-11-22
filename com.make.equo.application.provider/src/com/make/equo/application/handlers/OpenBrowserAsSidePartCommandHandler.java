package com.make.equo.application.handlers;

import java.util.Optional;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.basic.MBasicFactory;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartSashContainer;
import org.eclipse.e4.ui.model.application.ui.basic.MPartSashContainerElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimmedWindow;
import org.eclipse.e4.ui.model.application.ui.basic.MWindowElement;

import com.make.equo.application.parts.SinglePagePart;
import com.make.equo.application.util.IConstants;
import com.make.equo.application.util.IPositionConstants;

public class OpenBrowserAsSidePartCommandHandler {

	@Execute
	public void execute(@Named(IConstants.EQUO_OPEN_BROWSER_AS_SIDE_PART) String url,
			@Named(IConstants.EQUO_BROWSER_PART_NAME) String newPartName,
			@Named(IConstants.EQUO_BROWSER_PART_POSITION) String position, MTrimmedWindow mTrimmedWindow,
			MPart mainPart, UISynchronize sync) {
		sync.syncExec(() -> {
			String newPartStackId = IConstants.MAIN_PART_ID + "." + newPartName != null ? newPartName : "side.partstack." + position;
			mTrimmedWindow.getChildren().remove(mainPart);
			
			Optional<MUIElement> optionalSidePartStack = getSidePartStack(mainPart, newPartStackId);
			if (optionalSidePartStack.isPresent()) {
				MPartStack mPartStack = (MPartStack) optionalSidePartStack.get();
				if (!mPartStack.getChildren().isEmpty()) {
					MPart sideMPart = (MPart) mPartStack.getChildren().get(0);
					SinglePagePart singlePagePart = (SinglePagePart) sideMPart.getObject();
					singlePagePart.loadUrl(url);
				}
			} else {
				createNewSashContainer(url, newPartName, position, mTrimmedWindow, mainPart, newPartStackId);
			}
		});
	}

	private void createNewSashContainer(String url, String partName, String position, MTrimmedWindow mTrimmedWindow,
			MPart mainPart, String newPartStackId) {
		MPartSashContainer parentSashContainer = MBasicFactory.INSTANCE.createPartSashContainer();
		mTrimmedWindow.getChildren().add((MWindowElement) parentSashContainer);
		
		MPartStack newPartStack = MBasicFactory.INSTANCE.createPartStack();
		newPartStack.setElementId(newPartStackId);
		MPart newPart = MBasicFactory.INSTANCE.createPart();
		newPartStack.getChildren().add(newPart);
		// TODO generate a unique id...
		newPart.setElementId(newPartStackId + ".part");
		newPart.setContributionURI(IConstants.SINGLE_PART_CONTRIBUTION_URI);
		newPart.getProperties().put(IConstants.MAIN_URL_KEY, url);
		newPart.setCloseable(true);
		if (partName != null) {
			newPart.setLabel(partName);
		} else {
			newPartStack.getTags().add("NoTitle");
		}
		
		if (position != null) {
			switch (position) {
			case IPositionConstants.RIGHT:
				addChildrenToSashContainer(mainPart, newPartStack, parentSashContainer, true);
				break;
			case IPositionConstants.LEFT:
				addChildrenToSashContainer(newPartStack, mainPart, parentSashContainer, true);
				break;
			case IPositionConstants.BOTTOM:
				addChildrenToSashContainer(mainPart, newPartStack, parentSashContainer, false);
				break;
			case IPositionConstants.TOP:
				addChildrenToSashContainer(newPartStack, mainPart, parentSashContainer, false);
				parentSashContainer.setHorizontal(false);
				break;
			default:
				addChildrenToSashContainer(mainPart, newPartStack, parentSashContainer, false);
				break;
			}
		} else {
			addChildrenToSashContainer(mainPart, newPartStack, parentSashContainer, false);
		}

		parentSashContainer.setSelectedElement(newPartStack);
		parentSashContainer.setVisible(true);
		parentSashContainer.setToBeRendered(true);
	}

	private Optional<MUIElement> getSidePartStack(MPart mainPart, String newPartStackId) {
		MElementContainer<MUIElement> mainPartParent = mainPart.getParent();
		if (mainPartParent != null) {
			Optional<MUIElement> anySashElementChild = mainPartParent.getChildren().stream()
					.filter(sashContainerChild -> sashContainerChild.getElementId().equals(newPartStackId)).findAny();
			return anySashElementChild;
		}
		return Optional.empty();
	}

	private void addChildrenToSashContainer(MPartSashContainerElement firstElement,
			MPartSashContainerElement secondElement, MPartSashContainer partSashContainer, boolean horizontal) {
		partSashContainer.getChildren().add(firstElement);
		partSashContainer.getChildren().add(secondElement);
		partSashContainer.setHorizontal(horizontal);
	}
}
