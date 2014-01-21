package com.thibaudperso.sonycamera.sdk.model;

import java.util.Comparator;

public class Device {

	private int mId;
	private String mModel;
	private String mWebService;
	private boolean mNeedInit;

	public Device(int id, String model, String webService, boolean needInit) {
		this.mId = id;
		this.mModel = model;
		this.mWebService = webService;
		this.mNeedInit = needInit;
	}

	public int getId() {
		return mId;
	}

	public void setId(int id) {
		this.mId = id;
	}

	public String getModel() {
		return mModel;
	}

	public void setModel(String model) {
		this.mModel = model;
	}

	public String getWebService() {
		return mWebService;
	}

	public void setWebService(String webService) {
		this.mWebService = webService;
	}
	
	public boolean needInit() {
		return mNeedInit;
	}

	public void setNeedInit(boolean needInit) {
		this.mNeedInit = needInit;
	}

	@Override
	public String toString() {
		return mModel;
	}

	public static Comparator<Device> COMPARE_BY_DEVICEMODEL = new Comparator<Device>() {
		public int compare(Device one, Device other) {
			return one.mModel.compareTo(other.mModel);
		}
	};
	
	@Override
	public boolean equals(Object o) {
		if(!(o instanceof Device)) {
			return false;
		}
		return ((Device) o).mId == mId;
	}

}
