package org.pyload.android.client.module;

import java.util.HashMap;

public class GuiTask {

	private HashMap<Throwable, Runnable> exceptionMap;
	private final Runnable task;
	private final Runnable success;
	
	//how often the task can be called
	public int tries = 2;
	
	// called when anything goes wrong (optional)
	private Runnable critical;
	
	public GuiTask(Runnable task){
		this.task = task;
		// Nop
		this.success = new Runnable() {
			
			
			public void run() {				
			}
		};
	}
	
	public GuiTask(Runnable task, Runnable success) {
		this.task = task;
		this.success = success;
	}
	
	public GuiTask(Runnable task, Runnable success, HashMap<Throwable, Runnable> excHashMap) {
		this.task = task;
		this.success = success;
		this.exceptionMap = excHashMap;
	}
	
	public Runnable getTask(){
		return task;
	}
	
	public Runnable getSuccess(){
		return success;
	}
	
	public boolean hasExceptionMap(){
		return (exceptionMap != null && !exceptionMap.isEmpty());
	}
	
	public HashMap<Throwable, Runnable> getExceptionMap(){
		return exceptionMap;
	}
	
	public void putException(Throwable t, Runnable r){
		if(exceptionMap == null) exceptionMap = new HashMap<Throwable, Runnable>();
		
		exceptionMap.put(t, r);
	}
	
	public boolean hasCritical(){
		return (critical != null);
	}

	public void setCritical(Runnable critical) {
		this.critical = critical;
	}

	public Runnable getCritical() {
		return critical;
	}
	
}
