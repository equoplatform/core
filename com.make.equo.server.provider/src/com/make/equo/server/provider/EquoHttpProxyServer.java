package com.make.equo.server.provider;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.littleshoot.proxy.DefaultHostResolver;
import org.littleshoot.proxy.HostResolver;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.extras.SelfSignedMitmManager;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import org.littleshoot.proxy.impl.ProxyUtils;
import org.osgi.framework.Bundle;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

import com.make.equo.server.api.IEquoServer;
import com.make.equo.server.offline.api.IEquoOfflineServer;
import com.make.equo.server.offline.api.IHttpRequestFilter;
import com.make.equo.server.provider.resolvers.BundleUrlResolver;
import com.make.equo.server.provider.resolvers.EquoProxyServerUrlResolver;
import com.make.equo.server.provider.resolvers.EquoWebsocketsUrlResolver;
import com.make.equo.server.provider.resolvers.ILocalUrlResolver;
import com.make.equo.server.provider.resolvers.MainAppUrlResolver;
import com.make.equo.ws.api.IEquoWebSocketService;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.handler.codec.http.HttpRequest;

@Component
public class EquoHttpProxyServer implements IEquoServer {

	public static final String LOCAL_SCRIPT_APP_PROTOCOL = "main_app_equo_script/";
	public static final String BUNDLE_SCRIPT_APP_PROTOCOL = "external_bundle_equo_script/";
	public static final String LOCAL_FILE_APP_PROTOCOL = "equo/";
	
	public static final String EQUO_PROXY_SERVER_PATH = "equoFramework/";
	public static final String EQUO_WEBSOCKETS_JS_PATH = "equoWebsockets/";

	private static final String limitedConnectionGenericPageFilePath = EquoHttpProxyServer.EQUO_PROXY_SERVER_PATH + "limitedConnectionPage.html";

	private List<String> proxiedUrls = new ArrayList<>();
	private Map<String, List<String>> urlsToScripts = new HashMap<String, List<String>>();
	private boolean enableOfflineCache = false;
	private boolean connectionLimited = false;
	private String limitedConnectionAppBasedPagePath;
	
	private HttpProxyServer proxyServer;
	private Bundle mainEquoAppBundle;

	@Inject
	private IEquoWebSocketService equoWebsocketServer;

	//TODO check if it works when it's null. Add cardinality to this service in case it fails. Use @Reference.
	@Inject
	private IEquoOfflineServer equoOfflineServer;

	private HostResolver serverResolver = new DefaultHostResolver() {
		/** This proxy uses unresolved adresses while offline */
		@Override
		public InetSocketAddress resolve(String host, int port) throws UnknownHostException {
			if (isConnectionLimited()) {
				return new InetSocketAddress(host, port);
			}
			return super.resolve(host, port);
		}
	};
	private ScheduledExecutorService internetConnectionChecker;

	@Override
	public void startServer() {
		Runnable internetConnectionRunnable = new Runnable() {
			@Override
			public void run() {
				if (!isInternetReachable()) {
					setConnectionLimited();
				} else {
					setConnectionUnlimited();
				}
			}
		};
		internetConnectionChecker = Executors.newSingleThreadScheduledExecutor();
		internetConnectionChecker.scheduleAtFixedRate(internetConnectionRunnable, 0, 5, TimeUnit.SECONDS);

		proxyServer = DefaultHttpProxyServer
			.bootstrap()
			.withPort(9896)
			.withManInTheMiddle(new SelfSignedMitmManager())
			.withAllowRequestToOriginServer(true)
			.withTransparent(false)
			.withServerResolver(serverResolver)
			.withFiltersSource(new HttpFiltersSourceAdapter() {
					public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext clientCtx) {
						if (ProxyUtils.isCONNECT(originalRequest)) {
							return new HttpFiltersAdapter(originalRequest, clientCtx);
						}
						if (isEquoWebsocketJsApi(originalRequest)) {
							return new EquoWebsocketJsApiRequestFiltersAdapter(originalRequest,
									new EquoWebsocketsUrlResolver(EQUO_WEBSOCKETS_JS_PATH, equoWebsocketServer),
									equoWebsocketServer.getPort());
						}
						if (isLocalFileRequest(originalRequest)) {
							return new LocalFileRequestFiltersAdapter(originalRequest, getUrlResolver(originalRequest));
						}
						if (isConnectionLimited()) {
							if (isOfflineCacheSupported()) {
								return equoOfflineServer.getOfflineHttpFiltersAdapter(originalRequest);
							} else if (limitedConnectionAppBasedPagePath != null) {
								return new OfflineRequestFiltersAdapter(originalRequest, getUrlResolver(limitedConnectionAppBasedPagePath), limitedConnectionAppBasedPagePath);
							} else {
								return new OfflineRequestFiltersAdapter(originalRequest, new EquoProxyServerUrlResolver(EQUO_PROXY_SERVER_PATH), limitedConnectionGenericPageFilePath);
							}
						} else {
							Optional<String> url = getRequestedUrl(originalRequest);
							if (url.isPresent()) {
								String appUrl = url.get();
								return new EquoHttpModifierFiltersAdapter(appUrl, originalRequest,
										getCustomScripts(appUrl), equoWebsocketServer, isOfflineCacheSupported(),
										equoOfflineServer);
							} else {
								return new EquoHttpFiltersAdapter(originalRequest, equoOfflineServer,
										isOfflineCacheSupported());
							}
						}
					}

					private boolean isEquoWebsocketJsApi(HttpRequest originalRequest) {
						String uri = originalRequest.getUri();
						return uri.contains(EQUO_WEBSOCKETS_JS_PATH);
					}

					private ILocalUrlResolver getUrlResolver(HttpRequest originalRequest) {
						String uri = originalRequest.getUri();
						return getUrlResolver(uri);
					}

					private ILocalUrlResolver getUrlResolver(String uri) {
						if (uri.contains(LOCAL_SCRIPT_APP_PROTOCOL)) {
							return new MainAppUrlResolver(LOCAL_SCRIPT_APP_PROTOCOL, mainEquoAppBundle);
						}
						if (uri.contains(LOCAL_FILE_APP_PROTOCOL)) {
							return new MainAppUrlResolver(LOCAL_FILE_APP_PROTOCOL, mainEquoAppBundle);
						}
						if (uri.contains(BUNDLE_SCRIPT_APP_PROTOCOL)) {
							return new BundleUrlResolver(BUNDLE_SCRIPT_APP_PROTOCOL);
						}
						if (uri.contains(EQUO_PROXY_SERVER_PATH)) {
							return new EquoProxyServerUrlResolver(EQUO_PROXY_SERVER_PATH);
						}
						return null;
					}

					private boolean isLocalFileRequest(HttpRequest originalRequest) {
						String uri = originalRequest.getUri();
						return uri.contains(LOCAL_SCRIPT_APP_PROTOCOL) || uri.contains(LOCAL_FILE_APP_PROTOCOL)
								|| uri.contains(BUNDLE_SCRIPT_APP_PROTOCOL) || uri.contains(EQUO_PROXY_SERVER_PATH);
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

					@Override
					public int getMaximumResponseBufferSizeInBytes() {
						return 10 * 1024 * 1024;
					}

					@Override
					public int getMaximumRequestBufferSizeInBytes() {
						return 10 * 1024 * 1024;
					}
			}).start();
	}

	private void setConnectionLimited() {
		connectionLimited = true;
	}

	private void setConnectionUnlimited() {
		connectionLimited = false;
	}

	private boolean isConnectionLimited() {
		return connectionLimited;
	}

	private List<String> getCustomScripts(String url) {
		if (!urlsToScripts.containsKey(url)) {
			return Collections.emptyList();
		}
		return urlsToScripts.get(url);
	}

	@Deactivate
	public void stop() {
		System.out.println("Stopping proxy...");
		if (internetConnectionChecker != null) {
			internetConnectionChecker.shutdownNow();
		}
		if (proxyServer != null) {
			proxyServer.stop();
		}
	}

	@Override
	public void addCustomScript(String url, String scriptUrl) {
		if (!urlsToScripts.containsKey(url)) {
			urlsToScripts.put(url, new ArrayList<>());
		}
		urlsToScripts.get(url).add(scriptUrl);
	}

	@Override
	public void addUrl(String url) {
		proxiedUrls.add(url);
	}

	@Override
	public void setMainAppBundle(Bundle mainEquoAppBundle) {
		this.mainEquoAppBundle = mainEquoAppBundle;
	}

	@Override
	public String getLocalScriptProtocol() {
		return LOCAL_SCRIPT_APP_PROTOCOL;
	}

	@Override
	public String getBundleScriptProtocol() {
		return BUNDLE_SCRIPT_APP_PROTOCOL;
	}

	@Override
	public String getLocalFileProtocol() {
		return LOCAL_FILE_APP_PROTOCOL;
	}

	private boolean isOfflineCacheSupported() {
		return isOfflineServerSupported() && enableOfflineCache;
	}

	private boolean isOfflineServerSupported() {
		return equoOfflineServer != null;
	}

	@Override
	public void enableOfflineCache() {
		this.enableOfflineCache = true;
		if (isOfflineCacheSupported()) {
			equoOfflineServer.setProxiedUrls(proxiedUrls);
		}
	}

	@Override
	public void addOfflineSupportFilter(IHttpRequestFilter httpRequestFilter) {
		if (isOfflineCacheSupported()) {
			equoOfflineServer.addHttpRequestFilter(httpRequestFilter);
		}
	}

	private boolean isInternetReachable() {
		if (proxiedUrls.isEmpty()) {
			return false;
		}
		try (Socket socket = new Socket()) {
			String urlToProxy = proxiedUrls.get(0);
			URI uri = new URI(urlToProxy);
			String host = uri.getHost();
			socket.connect(new InetSocketAddress(host, 80));
			return true;
		} catch (IOException | URISyntaxException e) {
			return false; // Either timeout or unreachable or failed DNS lookup.
		}
	}

	@Override
	public void addLimitedConnectionPage(String limitedConnectionPagePath) {
		this.limitedConnectionAppBasedPagePath = limitedConnectionPagePath;
	}

}
