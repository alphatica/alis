package com.alphatica.alis.studio.web.jobs;

@FunctionalInterface
public interface JobWork {
	void run(Job job) throws Exception;
}
