package org.macgyver.mercator.docker;

import org.lendingclub.mercator.core.ScannerBuilder;

import jersey.repackaged.com.google.common.collect.Maps;

public class DockerScannerBuilder extends ScannerBuilder<DockerScanner> {

	@Override
	public DockerScanner build() {		
		return new DockerScanner(this,getProjector().getProperties());
	}

	
	
}
