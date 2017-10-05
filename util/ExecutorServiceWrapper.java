package com.americanexpress.smartserviceengine.common.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.smartserviceengine.common.exception.SSEException;


/**
 * Please use this class with caution and ensure to perform proper shutdown in the finally block 
 * to avoid any potential deadlock issues.
 * This has been build to avoid scattering of threading logic and ensure the proper shutdown of executors.
 * @author dkakka
 */
public class ExecutorServiceWrapper {

	public static final int MAX_THREAD_POOL_SIZE = 10;
	private static AmexLogger LOGGER = AmexLogger.create(ExecutorServiceWrapper.class);


	/**
	 * Request a fixed thread pool with the specified size and the policy that if the queue is full,
	 * thread submitting the tasks will run the tasks until there is space to add more to the thread pool.
	 * queueSize is the max number of awaiting tasks which can be hold by the queue. In case queue fills up,
	 * submitting task will be rejected.
	 * Currently allows maximum size of 10 threads.
	 * TODO - Implementation is based on the current research. To discuss more with Sr. Architects and 
	 * research more on the implementation to further refine it for the best performance and to avoid any memory/deadlock issues.
	 * @param size      The size of the thread pool.
	 * @param requestor String identifying the process requesting the thread pool.
	 * @return The thread pool.
	 */
	public static ExecutorService requestExecutor(int size, String requestor) {
		if(size > MAX_THREAD_POOL_SIZE){
			size = MAX_THREAD_POOL_SIZE;
		}
		int queueSize = size * 3;
		ExecutorService executorService = new ThreadPoolExecutor(size, size, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(queueSize), new ThreadPoolExecutor.CallerRunsPolicy());
		LOGGER.info("requestExecutor about to request service from:" + requestor);
		return executorService;
	}


	/**
	 * Shut down a thread pool, waiting for all queued threads to finish before returning.
	 * @param executorService The executor service that was obtained from one of the requestExecutor methods.
	 * @param requestor       String identifying the process requesting the thread pool.
	 */
	public static void shutdownThreadPool(ExecutorService executorService, String requestor) {
		if(executorService != null && !executorService.isShutdown()) {
			try {
				LOGGER.info("shutdownThreadPool requested by: " + requestor);
				executorService.shutdown();
				int hrsWait = 0;
				while(!executorService.isTerminated()) {
					LOGGER.info("shutdownThreadPool termination about to be awaited for : " + requestor);
					boolean termed = executorService.awaitTermination(600, TimeUnit.SECONDS); // 10 minutes
					if(!termed) {
						++hrsWait;
						LOGGER.warn("The executor service tried processing all threads but is not completed after " + (hrsWait * 10)
								+ " minute(s). For requestor: " + requestor);
					}
				}

			} catch(InterruptedException ex) {
				LOGGER.error("InterruptedException while shutdownThreadPool: " + ex.getMessage());
				throw new SSEException("EXECUTORS_SHUTDOWN_FAILED", "InterruptedException while shutdownThreadPool", ex);
			} finally {
				LOGGER.info("shutdownThreadPool service being removed for requestor : " + requestor);
			}
		}
	}

}