package com.make.equo.server.provider.resolvers;

import java.net.URL;
import java.util.Map;

import com.make.equo.contribution.api.IEquoContribution;
import com.make.equo.server.offline.api.resolvers.ILocalUrlResolver;

public class EquoContributionUrlResolver implements ILocalUrlResolver {

	private String equoContributionPaht;
	private Map<String, IEquoContribution> equoContributions;

	public EquoContributionUrlResolver(String equoContributionPaht, Map<String, IEquoContribution> equoContributions) {
		this.equoContributionPaht = equoContributionPaht;
		this.equoContributions = equoContributions;
	}

	@Override
	public String getProtocol() {
		return equoContributionPaht;
	}

	@Override
	public URL resolve(String contributionType) {
		System.out.println("Contribution type is " + contributionType);
		IEquoContribution equoContribution = equoContributions.get(contributionType);
		return equoContribution.getJavascriptAPIResource();
	}

}
