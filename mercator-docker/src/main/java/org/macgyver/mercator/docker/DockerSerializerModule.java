package org.macgyver.mercator.docker;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.Info;
import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;

public class DockerSerializerModule extends SimpleModule {


	private static final long serialVersionUID = 1L;
	ObjectMapper vanillaObjectMapper = new ObjectMapper();
	
	
	public DockerSerializerModule() {
		addSerializer(Container.class, new ContainerSerializer());
		addSerializer(Info.class,new InfoSerilaizer());
		addSerializer(Image.class,new ImageSerializer());
		addSerializer(InspectContainerResponse.class,new InspectContainerResponseSerializer());
	}
	class InfoSerilaizer extends StdSerializer<Info> {

		private static final long serialVersionUID = 1L;
		protected InfoSerilaizer() {
			super(Info.class);
		}

		@Override
		public void serialize(Info value, JsonGenerator gen, SerializerProvider provider) throws IOException {
		
	
			ObjectNode intermediate = (ObjectNode)  flatten(vanillaObjectMapper.valueToTree(value));
		
			ObjectNode target = vanillaObjectMapper.createObjectNode();
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
			gen.writeTree(target);
			
		}
		
	}
	
	void renameAttribute(ObjectNode x, String key, String key2) {
		JsonNode val = x.get(key);
		if (val!=null) {
			x.remove(key);
			x.set(key2, val);
		}
	}
	public ObjectNode flatten(JsonNode n) {
		ObjectNode out = vanillaObjectMapper.createObjectNode();
		Converter<String, String> caseFormat = CaseFormat.UPPER_CAMEL.converterTo(CaseFormat.LOWER_CAMEL);
		
		n.fields().forEachRemaining(it -> {
			JsonNode val = it.getValue();
			String key = it.getKey();
			key = caseFormat.convert(key);
			if (val.isValueNode()) {
				
				out.set(key, it.getValue());
			}
			else if (val.isArray()) {
				if (val.size()==0) {
					out.set(key, val);
				}
				boolean valid = true;
				Class<? extends Object> type=null;
				ArrayNode an = (ArrayNode) val;
				for (int i=0; valid && i<an.size(); i++) {
					if (!an.get(i).isValueNode()) {
						valid=false;
					}
					if (type!=null && an.get(i).getClass()!=type) {
						valid = false;
					}
				}
				
				
			}
		});
		renameAttribute(out, "oSType","osType");
		renameAttribute(out,"iD","id");
		renameAttribute(out,"neventsListener","nEventsListener");
		renameAttribute(out,"cPUSet","cpuSet");
		renameAttribute(out,"cPUShares","cpuShares");
		renameAttribute(out,"iPv4Forwarding","ipv4Forwarding");
		renameAttribute(out,"oOMKilled","oomKilled");
		renameAttribute(out,"state_oomkilled","state_oomKilled");
		renameAttribute(out,"bridgeNfIptables","bridgeNfIpTables");
		renameAttribute(out,"bridgeNfIp6tables","bridgeNfIp6Tables");
		out.remove("ngoroutines");
		return out;
	}
	
	class InspectContainerResponseSerializer extends StdSerializer<InspectContainerResponse> {
		
		private static final long serialVersionUID = 1L;

		protected InspectContainerResponseSerializer() {
			super(InspectContainerResponse.class);
		}

		@Override
		public void serialize(InspectContainerResponse value, JsonGenerator gen, SerializerProvider provider)
				throws IOException {
			ObjectNode n = (ObjectNode) vanillaObjectMapper.convertValue(value, JsonNode.class);
			
			
			ObjectNode out = flatten(n);
			
			addWithPrefix(out,"config",flatten(n.path("Config")));
			addWithPrefix(out,"state",flatten(n.path("State")));
			gen.writeTree(out);
		}
	}
	class ImageSerializer extends StdSerializer<Image> {
		
		private static final long serialVersionUID = 1L;
		protected ImageSerializer() {
			super(Image.class);
		}

		@Override
		public void serialize(Image img, JsonGenerator gen, SerializerProvider provider) throws IOException {
		
			ObjectNode x = vanillaObjectMapper.createObjectNode();
			x.put("created", img.getCreated());
			x.put("id", img.getId());
			x.put("parentId", img.getParentId());
			ArrayNode an = vanillaObjectMapper.createArrayNode();
			for (int i = 0; img.getRepoTags() != null && i < img.getRepoTags().length; i++) {
				an.add(img.getRepoTags()[i]);
			}
			x.set("repoTags", an);
			x.put("size", img.getSize());
			x.put("virtualSize", img.getVirtualSize());
			
			gen.writeTree(x);
		}
		
		
	}
	class ContainerSerializer extends StdSerializer<Container> {

		private static final long serialVersionUID = 1L;
		protected ContainerSerializer() {
			super(Container.class);
			// TODO Auto-generated constructor stub
		}

		@Override
		public void serialize(Container value, JsonGenerator gen, SerializerProvider provider) throws IOException {
			ObjectNode target = vanillaObjectMapper.createObjectNode();
			ObjectNode auto = (ObjectNode) vanillaObjectMapper.valueToTree(value);
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

		
			gen.writeTree(target);
			
		}
		
	}
	
	protected void addWithPrefix(ObjectNode top, String prefix, JsonNode val) {
		val.fields().forEachRemaining(it -> {
			top.set(prefix+"_"+it.getKey(), it.getValue());
		});
	}
}
