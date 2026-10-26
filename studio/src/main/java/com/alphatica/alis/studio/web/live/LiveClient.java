package com.alphatica.alis.studio.web.live;

/**
 * One open GUI tab. Messages may come from many threads.
 */
public interface LiveClient {

	void send(LiveMessage message);

	boolean isOpen();
}
