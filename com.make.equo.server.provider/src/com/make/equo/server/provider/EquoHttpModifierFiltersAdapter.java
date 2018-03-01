package com.make.equo.server.provider;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.http.entity.ContentType;

import com.make.equo.server.offline.api.IEquoOfflineServer;
import com.make.equo.ws.api.IEquoWebSocketService;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;

public class EquoHttpModifierFiltersAdapter extends EquoHttpFiltersAdapter {

	private static final String URL_PATH = "urlPath";
	private static final String PATH_TO_STRING_REG = "PATHTOSTRING";
	private List<String> jsScripts;
	private static final String URL_SCRIPT_SENTENCE = "<script src=\"urlPath\"></script>";
	private static final String LOCAL_SCRIPT_SENTENCE = "<script src=\"PATHTOSTRING\"></script>";
	private String appUrl;
	private static final String equoFrameworkJsApi = "equoFramework.js";
	private IEquoWebSocketService equoWebsocketServer;

	public EquoHttpModifierFiltersAdapter(String appUrl, HttpRequest originalRequest, List<String> jsScripts,
			IEquoWebSocketService equoWebsocketServer, boolean isOfflineTraficSupported,
			IEquoOfflineServer equoOfflineServer) {
		super(originalRequest, equoOfflineServer, isOfflineTraficSupported);
		this.appUrl = appUrl;
		this.jsScripts = jsScripts;
		this.equoWebsocketServer = equoWebsocketServer;
	}

	@Override
	public HttpObject serverToProxyResponse(HttpObject httpObject) {
		if (httpObject instanceof FullHttpResponse
				&& ((FullHttpResponse) httpObject).getStatus().code() == HttpResponseStatus.OK.code()) {
			FullHttpResponse fullResponse = (FullHttpResponse) httpObject;
			String contentTypeHeader = fullResponse.headers().get("Content-Type");
			if (contentTypeHeader != null && contentTypeHeader.startsWith("text/")) {
				ContentType contentType = ContentType.parse(contentTypeHeader);
				Charset charset = contentType.getCharset();

				ByteBuf content = fullResponse.content();

				byte[] data = new byte[content.readableBytes()];
				content.readBytes(data);

				String responseToTransform = createStringFromData(data, charset);
				StringBuilder customResponseWithScripts = new StringBuilder(responseToTransform);
				customResponseWithScripts.append(getEquoWebsocketsApi());
				customResponseWithScripts.append(getEquoFrameworkJsApi());
				customResponseWithScripts.append(convertJsScriptsToString());

				byte[] bytes = createDataFromString(customResponseWithScripts.toString(), charset);
				ByteBuf transformedContent = Unpooled.buffer(bytes.length);
				transformedContent.writeBytes(bytes);

				DefaultFullHttpResponse transformedResponse = new DefaultFullHttpResponse(
						fullResponse.getProtocolVersion(), fullResponse.getStatus(), transformedContent);
				transformedResponse.headers().set(fullResponse.headers());
				HttpHeaders.setContentLength(transformedResponse, bytes.length);

				saveRequestResponseIfPossible(originalRequest, transformedResponse);

				return transformedResponse;
			}
		}
		return super.serverToProxyResponse(httpObject);
	}

	private String getEquoWebsocketsApi() {
		String equoWebsocketsJsResourceName = equoWebsocketServer.getEquoWebsocketsJsResourceName();
		return createLocalScriptSentence(EquoHttpProxyServer.EQUO_WEBSOCKETS_JS_PATH + equoWebsocketsJsResourceName);
	}

	private String getEquoFrameworkJsApi() {
		return createLocalScriptSentence(EquoHttpProxyServer.EQUO_FRAMEWORK_PATH + equoFrameworkJsApi);
	}

	private String convertJsScriptsToString() {
		if (jsScripts.isEmpty()) {
			return "";
		}
		String newLineSeparetedScripts = jsScripts.stream().map(string -> generateScriptSentence(string))
				.collect(Collectors.joining("\n"));
		return newLineSeparetedScripts;
	}

	private String generateScriptSentence(String scriptPath) {
		try {
			if (isLocalScript(scriptPath)) {
				return createLocalScriptSentence(scriptPath);
			} else {
				URL url = new URL(scriptPath);
				String scriptSentence = URL_SCRIPT_SENTENCE.replaceAll(URL_PATH, url.toString());
				return scriptSentence;
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return "";
	}

	private String createLocalScriptSentence(String scriptPath) {
		String scriptSentence = LOCAL_SCRIPT_SENTENCE.replaceAll(PATH_TO_STRING_REG, appUrl + scriptPath);
		return scriptSentence;
	}

	private boolean isLocalScript(String scriptPath) {
		String scriptPathLoweredCase = scriptPath.trim().toLowerCase();
		return scriptPathLoweredCase.startsWith(EquoHttpProxyServer.LOCAL_SCRIPT_PROTOCOL)
				|| scriptPathLoweredCase.startsWith(EquoHttpProxyServer.BUNDLE_SCRIPT_PROTOCOL);
	}

	private String createStringFromData(byte[] data, Charset charset) {
		return (charset == null) ? new String(data) : new String(data, charset);
	}

	private byte[] createDataFromString(String string, Charset charset) {
		return (charset == null) ? string.getBytes() : string.getBytes(charset);
	}

}