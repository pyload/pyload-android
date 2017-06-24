package org.pyload.android.client.module;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;

import org.pyload.android.client.pyLoadApp;

import android.os.Handler;
import android.util.Log;

/**
 * Task Queue to ensure single threadedness but provide async taks, to avoid
 * blocking the GUI Thread Will catch all exceptions and post them to the
 * handler if found in exception map.
 * 
 * @author RaNaN
 * 
 */

public class TaskQueue {
	private final LinkedList<GuiTask> tasks = new LinkedList<GuiTask>();
	private HashMap<Throwable, Runnable> exceptionMap;
	private Handler mHandler;
	private pyLoadApp app;

	private boolean running;
	private Runnable internalRunnable;

	private class InternalRunnable implements Runnable {
		public void run() {
			internalRun();
		}
	}

	public TaskQueue(pyLoadApp app, Handler mHandler, HashMap<Throwable, Runnable> exceptionMap) {
		this.app = app;
		this.mHandler = mHandler;
		this.exceptionMap = exceptionMap;

		internalRunnable = new InternalRunnable();
	}

	public void start() {
		if (!running) {
			Thread thread = new Thread(internalRunnable);
			thread.setDaemon(true);
			running = true;
			thread.start();
		}
	}

	public void stop() {
		running = false;
	}

	public void clear() {
		tasks.clear();
	}

	public void addTask(GuiTask task) {
		synchronized (tasks) {
			tasks.addFirst(task);
			tasks.notify(); // notify any waiting threads
		}
	}

	private GuiTask getNextTask() {
		synchronized (tasks) {
			while (tasks.isEmpty()) {
				try {
					tasks.wait();
				} catch (InterruptedException e) {
					Log.e("pyLoad", "Task interrupted", e);
					stop();
				}
			}
			return tasks.removeLast();
		}
	}

	private void internalRun() {
		while (running) {
			GuiTask task = getNextTask();
			
			// TODO: unusable atm
			if (task.tries <= 0){
				Log.d("pyLoad", task.toString()+ " has reached retry limit");
				continue;
			}
			
			try {
				task.getTask().run();
				mHandler.post(task.getSuccess());

			} catch (Throwable t) {
				Log.e("pyLoad", "Task threw an exception", t);
				app.setLastException(t);
				
				if (task.hasCritical()){
					mHandler.post(task.getCritical());
				}
				
				if (task.hasExceptionMap()) {
					for (Entry<Throwable, Runnable> set : task
							.getExceptionMap().entrySet()) {
						if (t.getClass() == set.getKey().getClass()) {
							mHandler.post(set.getValue());
						}

					}
				}

				for (Entry<Throwable, Runnable> set : exceptionMap.entrySet()) {
					if (t.getClass() == set.getKey().getClass()) {
						mHandler.post(set.getValue());
					}

				}

			}
		}
	}
}
