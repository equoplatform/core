package com.make.equo.server.provider;

import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;

public class EquoWebsocketJsApiRequestFiltersAdapter extends LocalFileRequestFiltersAdapter {

	private int portNumber;

	public EquoWebsocketJsApiRequestFiltersAdapter(HttpRequest originalRequest, ILocalUrlResolver urlResolver,
			int portNumber) {
		super(originalRequest, urlResolver);
		this.portNumber = portNumber;
	}

	@Override
	protected HttpResponse buildResponse(ByteBuf buffer, String contentType) {
		String contentFile = buffer.toString(Charset.defaultCharset());
		String response = String.format(contentFile, portNumber);
		byte[] bytes = response.getBytes();
		return super.buildResponse(Unpooled.wrappedBuffer(bytes), contentType);
	}

}