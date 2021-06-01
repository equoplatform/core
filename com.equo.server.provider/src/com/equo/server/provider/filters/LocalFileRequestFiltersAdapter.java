package com.equo.server.provider.filters;

import com.equo.server.offline.api.filters.OfflineRequestFiltersAdapter;
import com.equo.server.offline.api.resolvers.ILocalUrlResolver;

import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;

/**
 * Adapter to response a local file for the requests.
 */
public class LocalFileRequestFiltersAdapter extends OfflineRequestFiltersAdapter {

  public LocalFileRequestFiltersAdapter(HttpRequest originalRequest,
      ILocalUrlResolver urlResolver) {
    super(originalRequest, urlResolver);
  }

  @Override
  public HttpResponse clientToProxyRequest(HttpObject httpObject) {
    String uri = originalRequest.getUri();
    this.setLocalFilePathWithProtocol(uri);
    return super.clientToProxyRequest(httpObject);
  }

}
