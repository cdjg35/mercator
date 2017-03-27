package org.macgyver.mercator.ucs;

import org.jdom2.Element;

public class UCSRemoteException extends UCSException {

	String code;
	String description;
	
	public UCSRemoteException(String code, String description) {
		super("code="+code+" description="+description);
		this.code = code;
		this.description = description;
	}
	
	
	public static UCSRemoteException fromResponse(Element element) {
		String code = element.getAttributeValue("errorCode");
		String desc = element.getAttributeValue("errorDescr");
		UCSRemoteException e = new UCSRemoteException(code, desc);
		return e;
	}
}
