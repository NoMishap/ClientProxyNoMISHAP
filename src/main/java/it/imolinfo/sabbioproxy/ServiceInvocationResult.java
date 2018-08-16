package it.imolinfo.sabbioproxy;

import javax.ws.rs.core.Response;

public class ServiceInvocationResult {
	private Response response;
	private long executionTime;
	
	public ServiceInvocationResult(Response response, long executionTime) {
		super();
		this.response = response;
		this.executionTime = executionTime;
	}
	
	public Response getResponse() {
		return response;
	}
	public void setResponse(Response response) {
		this.response = response;
	}
	public long getExecutionTime() {
		return executionTime;
	}
	public void setExecutionTime(long executionTime) {
		this.executionTime = executionTime;
	}

}
