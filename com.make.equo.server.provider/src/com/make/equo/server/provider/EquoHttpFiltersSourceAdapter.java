package com.make.equo.server.provider;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.impl.ProxyUtils;
import org.osgi.framework.Bundle;

import com.make.equo.server.offline.api.IEquoOfflineServer;
import com.make.equo.server.offline.api.filters.OfflineRequestFiltersAdapter;
import com.make.equo.server.offline.api.resolvers.ILocalUrlResolver;
import com.make.equo.server.provider.resolvers.BundleUrlResolver;
import com.make.equo.server.provider.resolvers.EquoProxyServerUrlResolver;
import com.make.equo.server.provider.resolvers.EquoWebsocketsUrlResolver;
import com.make.equo.server.provider.resolvers.MainAppUrlResolver;
import com.make.equo.ws.api.IEquoWebSocketService;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.handler.codec.http.HttpRequest;

public class EquoHttpFiltersSourceAdapter extends HttpFiltersSourceAdapter {

	private static final String limitedConnectionGenericPageFilePath = EquoHttpProxyServer.EQUO_PROXY_SERVER_PATH
			+ "limitedConnectionPage.html";

	private IEquoWebSocketService equoWebsocketServer;
	private IEquoOfflineServer equoOfflineServer;

	private boolean connectionLimited = false;
	private boolean isOfflineCacheSupported;

	private String limitedConnectionAppBasedPagePath;
	private Bundle mainEquoAppBundle;
	private List<String> proxiedUrls;

	private String equoFrameworkJsApi;

	private String equoWebsocketsApi;

	private Map<String, String> urlsToScriptsAsStrings;

	public EquoHttpFiltersSourceAdapter(IEquoWebSocketService equoWebsocketServer, IEquoOfflineServer equoOfflineServer,
			boolean isOfflineCacheSupported, String limitedConnectionAppBasedPagePath, Bundle mainEquoAppBundle,
			List<String> proxiedUrls, String equoFrameworkJsApi, String equoWebsocketsApi,
			Map<String, String> urlsToScriptsAsStrings) {
		this.equoWebsocketServer = equoWebsocketServer;
		this.equoOfflineServer = equoOfflineServer;
		this.isOfflineCacheSupported = isOfflineCacheSupported;
		this.limitedConnectionAppBasedPagePath = limitedConnectionAppBasedPagePath;
		this.mainEquoAppBundle = mainEquoAppBundle;
		this.proxiedUrls = proxiedUrls;
		this.equoFrameworkJsApi = equoFrameworkJsApi;
		this.equoWebsocketsApi = equoWebsocketsApi;
		this.urlsToScriptsAsStrings = urlsToScriptsAsStrings;
	}

	@Override
	public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext clientCtx) {
		if (ProxyUtils.isCONNECT(originalRequest)) {
			return new HttpFiltersAdapter(originalRequest, clientCtx);
		}
		if (isEquoWebsocketJsApi(originalRequest)) {
			return new EquoWebsocketJsApiRequestFiltersAdapter(originalRequest,
					new EquoWebsocketsUrlResolver(EquoHttpProxyServer.EQUO_WEBSOCKETS_JS_PATH, equoWebsocketServer),
					equoWebsocketServer.getPort());
		}
		if (isLocalFileRequest(originalRequest)) {
			return new LocalFileRequestFiltersAdapter(originalRequest, getUrlResolver(originalRequest));
		}
		if (isConnectionLimited()) {
			if (isOfflineCacheSupported) {
				return equoOfflineServer.getOfflineHttpFiltersAdapter(originalRequest);
			} else if (limitedConnectionAppBasedPagePath != null) {
				return new OfflineRequestFiltersAdapter(originalRequest,
						getUrlResolver(limitedConnectionAppBasedPagePath), limitedConnectionAppBasedPagePath);
			} else {
				return new OfflineRequestFiltersAdapter(originalRequest,
						new EquoProxyServerUrlResolver(EquoHttpProxyServer.EQUO_PROXY_SERVER_PATH),
						limitedConnectionGenericPageFilePath);
			}
		} else {
			Optional<String> url = getRequestedUrl(originalRequest);
			if (url.isPresent()) {
				String appUrl = url.get();
				return new EquoHttpModifierFiltersAdapter(originalRequest, equoFrameworkJsApi, equoWebsocketsApi,
						getCustomScripts(appUrl), isOfflineCacheSupported, equoOfflineServer);
			} else {
				return new EquoHttpFiltersAdapter(originalRequest, equoOfflineServer, isOfflineCacheSupported);
			}
		}
	}

	private boolean isEquoWebsocketJsApi(HttpRequest originalRequest) {
		String uri = originalRequest.getUri();
		return uri.contains(EquoHttpProxyServer.EQUO_WEBSOCKETS_JS_PATH);
	}

	private ILocalUrlResolver getUrlResolver(HttpRequest originalRequest) {
		String uri = originalRequest.getUri();
		return getUrlResolver(uri);
	}

	private ILocalUrlResolver getUrlResolver(String uri) {
		if (uri.contains(EquoHttpProxyServer.LOCAL_SCRIPT_APP_PROTOCOL)) {
			return new MainAppUrlResolver(EquoHttpProxyServer.LOCAL_SCRIPT_APP_PROTOCOL, mainEquoAppBundle);
		}
		if (uri.contains(EquoHttpProxyServer.LOCAL_FILE_APP_PROTOCOL)) {
			return new MainAppUrlResolver(EquoHttpProxyServer.LOCAL_FILE_APP_PROTOCOL, mainEquoAppBundle);
		}
		if (uri.contains(EquoHttpProxyServer.BUNDLE_SCRIPT_APP_PROTOCOL)) {
			return new BundleUrlResolver(EquoHttpProxyServer.BUNDLE_SCRIPT_APP_PROTOCOL);
		}
		if (uri.contains(EquoHttpProxyServer.EQUO_PROXY_SERVER_PATH)) {
			return new EquoProxyServerUrlResolver(EquoHttpProxyServer.EQUO_PROXY_SERVER_PATH);
		}
		return null;
	}

	private boolean isLocalFileRequest(HttpRequest originalRequest) {
		String uri = originalRequest.getUri();
		return uri.contains(EquoHttpProxyServer.LOCAL_SCRIPT_APP_PROTOCOL)
				|| uri.contains(EquoHttpProxyServer.LOCAL_FILE_APP_PROTOCOL)
				|| uri.contains(EquoHttpProxyServer.BUNDLE_SCRIPT_APP_PROTOCOL)
				|| uri.contains(EquoHttpProxyServer.EQUO_PROXY_SERVER_PATH);
	}

	private Optional<String> getRequestedUrl(HttpRequest originalRequest) {
		return proxiedUrls.stream().filter(url -> containsHeader(url, originalRequest)).findFirst();
	}

	private boolean containsHeader(String url, HttpRequest originalRequest) {
		String host = originalRequest.headers().get(Names.HOST);
		if (host.indexOf(":") != -1) {
			return url.contains(host.substring(0, host.indexOf(":")));
		} else {
			return url.contains(host);
		}
	}

	private String getCustomScripts(String url) {
		if (!urlsToScriptsAsStrings.containsKey(url)) {
			return "";
		}
		return urlsToScriptsAsStrings.get(url);
	}

	@Override
	public int getMaximumResponseBufferSizeInBytes() {
		return 10 * 1024 * 1024;
	}

	@Override
	public int getMaximumRequestBufferSizeInBytes() {
		return 10 * 1024 * 1024;
	}

	private boolean isConnectionLimited() {
		return connectionLimited;
	}

	public void setConnectionLimited() {
		connectionLimited = true;
	}

	public void setConnectionUnlimited() {
		connectionLimited = false;
	}

}