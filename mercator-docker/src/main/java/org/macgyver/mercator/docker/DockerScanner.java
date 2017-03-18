package org.macgyver.mercator.docker;

import java.util.Map;

import org.bouncycastle.crypto.RuntimeCryptoException;
import org.lendingclub.mercator.core.AbstractScanner;
import org.lendingclub.mercator.core.Scanner;
import org.lendingclub.mercator.core.ScannerBuilder;
import org.lendingclub.mercator.core.ScannerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.DockerCmdExecFactory;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Info;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.jaxrs.JerseyDockerCmdExecFactory;
import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

public class DockerScanner extends AbstractScanner {

	Logger logger = LoggerFactory.getLogger(DockerScanner.class);

	static ObjectMapper mapper = new ObjectMapper();

	Supplier<DockerClient> supplier;

	public DockerScanner(ScannerBuilder<? extends Scanner> builder, Map<String, String> props) {
		super(builder, props);

		supplier = Suppliers.memoize(new DockerClientSupplier());
	}

	class DockerClientSupplier implements Supplier<DockerClient> {
		public DockerClient get() {
			String host = getConfig().getOrDefault("DOCKER_HOST", "unix:///var/run/docker.sock");
			DefaultDockerClientConfig cc = DefaultDockerClientConfig.createDefaultConfigBuilder().withDockerHost(host)
					.build();

			DockerCmdExecFactory dockerCmdExecFactory = new JerseyDockerCmdExecFactory().withReadTimeout(1000)
					.withConnectTimeout(1000).withMaxTotalConnections(100).withMaxPerRouteConnections(10);

			DockerClient dockerClient = DockerClientBuilder.getInstance(cc)
					.withDockerCmdExecFactory(dockerCmdExecFactory).build();
			return dockerClient;
		}

	}

	public DockerClient getDockerClient() {
		return supplier.get();
	}

	protected void projectContainer(Container c) {
		JsonNode n = containerToJson(c);

		String cypher = "merge (c:DockerContainer {id:{id}}) set c+={props},c.updateTs=timestamp() return c";

		getProjector().getNeoRxClient().execCypher(cypher, "id", c.getId(), "props", n);

		cypher = "merge (x:DockerImage {id:{imageId}}) set x.updateTs=timestamp() return x";

		getProjector().getNeoRxClient().execCypher(cypher, "imageId", c.getImageId(), "name", c.getImage());

		cypher = "match (di:DockerImage {id:{imageId}}), (dc:DockerContainer {id:{id}}) merge (dc)-[r:USES]->(di) set r.updateTs=timestamp()";

		getProjector().getNeoRxClient().execCypher(cypher, "imageId", c.getImageId(), "id", c.getId());

		ScannerContext.getScannerContext().ifPresent(it -> {
			it.incrementEntityCount();
			it.incrementEntityCount();
		});
	}

	public void scanContainers() {
		new ScannerContext().exec(x -> {
			getDockerClient().listContainersCmd().withShowAll(true).exec().forEach(it -> {
				try {
					projectContainer(it);
				} catch (Exception e) {
					ScannerContext.getScannerContext().ifPresent(ctx -> {
						ctx.markException(e);

					});
					logger.warn("", e);
				}

			});
		});
	}

	JsonNode containerToJson(Container c) {
		ObjectNode target = mapper.createObjectNode();
		ObjectNode auto = (ObjectNode) mapper.convertValue(c, JsonNode.class);
		Converter<String, String> caseFormat = CaseFormat.UPPER_CAMEL.converterTo(CaseFormat.LOWER_CAMEL);

		auto.fields().forEachRemaining(it -> {

			String field = caseFormat.convert(it.getKey());
			if (it.getValue().isContainerNode()) {
				if (it.getValue().isArray()) {

					if (field.equals("names")) {
						target.set(field, it.getValue());
					}

				}
			}

			else {
				if (field.equals("imageID")) {
					field = "imageId";
				}
				target.set(field, it.getValue());
			}

		});

		target.put("running", target.path("status").asText().toLowerCase().startsWith("up"));

		return target;
	}

	ObjectNode toJson(Info info) {
		ObjectNode intermediate = (ObjectNode) mapper.convertValue(info, JsonNode.class);
		ObjectNode target = mapper.createObjectNode();
		Converter<String, String> caseFormat = CaseFormat.UPPER_CAMEL.converterTo(CaseFormat.LOWER_CAMEL);

		intermediate.fields().forEachRemaining(it -> {

			if (it.getValue().isContainerNode()) {
				// discard
			} else {
				String key = caseFormat.convert(it.getKey());
				if (key.startsWith("cPU")) {
					key = key.replace("cPU", "cpu");
				} else if (key.equals("imageID")) {
					key = "imageId";
				} else if (key.equals("iD")) {
					key = "id";
				} else if (key.equalsIgnoreCase("ostype")) {
					key = "osType";
				} else if (key.equalsIgnoreCase("ngoroutines")) {
					key = "nGoRoutines";
				} else if (key.equalsIgnoreCase("neventsListener")) {
					key = "nEventsListener";
				}
				target.set(key, it.getValue());
			}

		});
		return target;
	}

	public void scanInfo() {
		Info info = getDockerClient().infoCmd().exec();

	}

	public void scan() {
		scanInfo();

		scanContainers();

	}
}
