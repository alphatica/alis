package com.alphatica.alis.studio.web.live;

/**
 * Topic a client subscribes to explicitly, e.g. the output of one job.
 */
public interface TopicSource {

	/**
	 * Called once per subscription. A source may first send what was produced so far.
	 */
	void subscribe(LiveClient client);

	void unsubscribe(LiveClient client);
}
