package com.make.equo.builders.tests.util;

import org.osgi.service.component.annotations.Component;

import com.make.equo.application.api.IEquoApplication;
import com.make.equo.application.model.EquoApplicationBuilder;

@Component
public class EquoApp implements IEquoApplication{

	@Override
	public EquoApplicationBuilder buildApp(EquoApplicationBuilder appBuilder) {
		// TODO Auto-generated method stub
		return null;
	}

}
