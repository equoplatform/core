package com.make.equo.contribution.provider.filter;

import java.net.URI;
import java.net.URL;

import com.make.equo.contribution.api.resolvers.IEquoContributionUrlResolver;
import com.make.equo.server.offline.api.filters.IModifiableResponse;
import com.make.equo.server.offline.api.filters.OfflineRequestFiltersAdapter;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;

public class ContributionFileRequestFiltersAdapter extends OfflineRequestFiltersAdapter implements IModifiableResponse {

	private String contributionName;
	protected IEquoContributionUrlResolver urlResolver;

	public ContributionFileRequestFiltersAdapter(HttpRequest originalRequest, IEquoContributionUrlResolver urlResolver,
			String contributionName) {
		super(originalRequest);
		this.urlResolver = urlResolver;
		this.contributionName = contributionName;
	}

	@Override
	public HttpResponse clientToProxyRequest(HttpObject httpObject) {
		String requestUri = originalRequest.getUri();
		String fileName = requestUri.substring(requestUri.indexOf(contributionName) + contributionName.length(),
				requestUri.length());
		URL resolvedUrl = urlResolver.resolve(fileName);
		if (resolvedUrl == null) {
			URI requestUriAsUri = URI.create(requestUri);
			final String host = requestUriAsUri.getHost();
			if (host != null && host.contains(contributionName)) {
				fileName = fileName.substring(fileName.indexOf(contributionName) + contributionName.length(),
						fileName.length());
				resolvedUrl = urlResolver.resolve(fileName);
			}
		}
		return super.buildHttpResponse(resolvedUrl);
	}

	@Override
	public FullHttpResponse getOriginalFullHttpResponse() {
		return null;
	}

	@Override
	public boolean isModifiable() {
		return true;
	}

	@Override
	public String modifyOriginalResponse(String responseToTransform) {
		return null;
	}
}
