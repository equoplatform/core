package com.equo.ws.api;

import java.util.Optional;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

/**
 * A helper class to obtain an EquoEventHandler instance.
 */
public class EquoEventHandlerProvider {

  private IEquoEventHandler equoEventHandler;

  /**
   * Default constructor.
   */
  public EquoEventHandlerProvider() {
    BundleContext ctx = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
    if (ctx != null) {
      @SuppressWarnings("unchecked")
      ServiceReference<IEquoEventHandler> serviceReference = (ServiceReference<
          IEquoEventHandler>) ctx.getServiceReference(IEquoEventHandler.class.getName());
      if (serviceReference != null) {
        equoEventHandler = ctx.getService(serviceReference);
      }
    }
  }

  public Optional<IEquoEventHandler> getEquoEventHandler() {
    return Optional.ofNullable(equoEventHandler);
  }

}
