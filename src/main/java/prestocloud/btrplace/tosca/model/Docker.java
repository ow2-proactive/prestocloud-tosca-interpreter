/*
 * ProActive Parallel Suite(TM):
 * The Open Source library for parallel and distributed
 * Workflows & Scheduling, Orchestration, Cloud Automation
 * and Big Data Analysis on Enterprise Grids & Clouds.
 *
 * Copyright (c) 2007 - 2017 ActiveEon
 * Contact: contact@activeeon.com
 *
 * This library is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation: version 3 of
 * the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * If needed, contact us to obtain a release under GPL Version 2 or 3
 * or a different license than the AGPL.
 */
package prestocloud.btrplace.tosca.model;

import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author ActiveEon Team
 * @since 25/09/18
 */
public class Docker {

    @Getter
    public String fragmentName;
    @Getter @Setter
    public String resourceType;
    @Getter @Setter
    public String image;
    @Getter @Setter
    public String registry;
    @Getter @Setter
    public String cmd;
    @Getter @Setter
    public Map<String, String> variables;
    private List<DockerNetworkMapping> mappingList;

    public static final String EMPTY_STRING = "";
    private static final Pattern registryIdentifier = Pattern.compile("^(\\S+)\\/(\\S+)$");
    private static final List<String> AUTHORIZED_PUBLICLY_EXPOSED_PORTS = Arrays.asList("80", "443", "8080", "8888", "4433");

    public Docker(String fragmentName) {
        this.fragmentName = fragmentName;
        variables = new HashMap<>();
        this.mappingList = new ArrayList<>();
    }

    public void addNetworkMapping(String target, String published, String protocol) {
        DockerNetworkMapping tmp = new DockerNetworkMapping(target, published, protocol);
        mappingList.add(tmp);
    }

    public void addVariable(String key, String value) {
        variables.put(key, value);
    }

    public String printCmdline() {
        /*if (cmd != null) {
            return "docker run " + image + " " + cmd;
        }
        else {
            return "docker run " + image;
        }*/
        if (image != null) {
            String registryString = prepareRegistry();
            return String.format("%sdocker pull %s && docker run -d --restart unless-stopped  %s %s %s %s", prepareLogin(), registryString, preparePortsForwarding(), prepareEnvironement(), registryString, prepareCmd());
        } else {
            return EMPTY_STRING;
        }
    }

    private String prepareLogin() {
        if (this.registry == null) {
            return Docker.EMPTY_STRING;
        } else if (this.registry.length() == 0) {
            return Docker.EMPTY_STRING;
        } else {
            return String.format("docker login -u @credentials_prestocloud_%s_username -p @credentials_prestocloud_%s_password %s && ", registry, registry, registry);
        }
    }

    private String prepareRegistry() {
        if (this.registry == null) {
            return image;
        } else if (this.registry.length() == 0) {
            return image;
        } else {
            // We check a registry is not already set in the image specification.
            Matcher matcher = registryIdentifier.matcher(this.image);
            if (matcher.matches()) {
               return registry + "/" + matcher.group(2);
            } else {
                // No problem
                return registry + "/" + image;
            }
        }
    }

    private String prepareEnvironement() {
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String,String> variable : variables.entrySet()) {
           result.append(String.format(" -e %s=%s ",variable.getKey(),variable.getValue()));
        }
        return result.toString();
    }

    private String preparePortsForwarding() {
        if (this.mappingList.stream().anyMatch(dockerNetworkMapping -> dockerNetworkMapping.getPublicPort().equals("-1"))) {
            return " --net=host " + this.mappingList.stream().filter(dockerNetworkMapping -> !dockerNetworkMapping.getPublicPort().equals("-1")).map(DockerNetworkMapping::getDockerCliArg).collect(Collectors.joining(" "));
        } else {
            return this.mappingList.stream().map(DockerNetworkMapping::getDockerCliArg).collect(Collectors.joining(" ")) + "";
        }
    }

    private String prepareCmd() {
        if (cmd == null) {
            return EMPTY_STRING;
        } else if (cmd.length() == 0) {
            return EMPTY_STRING;
        } else {
            return " " + cmd;
        }
    }

    public String getAllExposedPorts() {
        return this.mappingList.stream().map(dockerNetworkMapping -> dockerNetworkMapping.getPublicPort()).collect(Collectors.joining(";"));
    }

    public String getAllPubliclyExposedPorts() {
        return this.mappingList.stream().map(d -> d.getPublicPort()).filter(d -> AUTHORIZED_PUBLICLY_EXPOSED_PORTS.contains(d)).collect(Collectors.joining(";"));
    }

}

