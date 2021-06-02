/****************************************************************************
**
** Copyright (C) 2021 Equo
**
** This file is part of Equo Framework.
**
** Commercial License Usage
** Licensees holding valid commercial Equo licenses may use this file in
** accordance with the commercial license agreement provided with the
** Software or, alternatively, in accordance with the terms contained in
** a written agreement between you and Equo. For licensing terms
** and conditions see https://www.equoplatform.com/terms.
**
** GNU General Public License Usage
** Alternatively, this file may be used under the terms of the GNU
** General Public License version 3 as published by the Free Software
** Foundation. Please review the following
** information to ensure the GNU General Public License requirements will
** be met: https://www.gnu.org/licenses/gpl-3.0.html.
**
****************************************************************************/

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
