package com.equo.ws.api;

/**
 * Parser from ws event payload into an Equo Runnable.
 */
public interface IEquoRunnableParser<T> {
  /**
   * Parses the payload data.
   * @param  payload the payload data.
   * @return         the parsed data to an object of type T.
   */
  public T parsePayload(Object payload);

  /**
   * Gets the instance type of the parser from an equo runnable.
   * @return the instance IEquoRunnable.
   */
  public IEquoRunnable<T> getEquoRunnable();

}
