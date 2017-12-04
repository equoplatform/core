package com.make.equo.application.impl;

import java.util.Collections;
import java.util.List;

import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.commands.MCommand;
import org.eclipse.e4.ui.model.application.commands.MCommandParameter;
import org.eclipse.e4.ui.model.application.commands.MCommandsFactory;
import org.eclipse.e4.ui.model.application.commands.MHandler;
import org.eclipse.e4.ui.model.application.commands.impl.CommandsFactoryImpl;

import com.make.equo.application.util.IConstants;

public abstract class HandlerBuilder implements MParameterBuilder {
	
	private String commandParameterId;
	private String contributionUri;
	private MApplication mApplication;

	protected HandlerBuilder(MApplication mApplication, String commandParameterId, String contributionUri) {
		this.mApplication = mApplication;
		this.commandParameterId = commandParameterId;
		this.contributionUri = contributionUri;
	}

	/**
	 * Allows subclasses to add multiple command parameters;
	 *  
	 * @param id
	 * @return
	 */
	protected List<MCommandParameter> createCommandParameters() {
		return Collections.emptyList();
	}
	
	protected MCommandParameter createCommandParameter(String id, String name, boolean isOptional) {
		MCommandParameter commandParameter = MCommandsFactory.INSTANCE.createCommandParameter();
		commandParameter.setElementId(id);
		commandParameter.setName(name);
		commandParameter.setOptional(isOptional);
		return commandParameter;
	}

	private MHandler createNewHandler(String id, String contributionUri) {
		MHandler newHandler = CommandsFactoryImpl.eINSTANCE.createHandler();
		newHandler.setElementId(id + IConstants.HANDLER_SUFFIX) ;
		newHandler.setContributionURI(contributionUri);
		return newHandler;
	}

	private MCommand createNewCommand(String id) {
		MCommand newCommand = CommandsFactoryImpl.eINSTANCE.createCommand();
		newCommand.setElementId(id + IConstants.COMMAND_SUFFIX) ;
		newCommand.setCommandName(id + IConstants.COMMAND_SUFFIX);
		return newCommand;
	}
	
	public MCommand createCommandAndHandler(String id) {
		MCommand newCommand = createNewCommand(id);
		MCommandParameter commandParameter = createCommandParameter(commandParameterId, "Command Parameter Name", false);
		newCommand.getParameters().add(commandParameter);
		newCommand.getParameters().addAll(createCommandParameters());
		
		MHandler newHandler = createNewHandler(id, contributionUri);
		newHandler.setCommand(newCommand);
		
		mApplication.getCommands().add(newCommand);
		mApplication.getHandlers().add(newHandler);
		
		mApplication.getTransientData().put(newCommand.getElementId(), getRunnable());
		
		return newCommand;
	}
	
	protected abstract Runnable getRunnable();
	
}