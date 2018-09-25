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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * @author ActiveEon Team
 * @since 25/09/18
 */
public class Relationship {
    @Getter
    @Setter
    public String fragment;
    @Getter @Setter
    public String jppf;
    @Getter @Setter
    public String host;
    @Getter @Setter
    public String master;
    @Getter @Setter
    public Map<String, List<String>> hostingConstraints;
    @Getter @Setter
    public Map<String, List<String>> resourceConstraints;
    @Getter @Setter
    public Map<String, List<String>> osConstraints;

    public Relationship(String fragment, String jppf, String host) {
        this.fragment = fragment;
        this.jppf = jppf;
        this.host = host;
        this.master = null;
        hostingConstraints = new HashMap<>();
        resourceConstraints = new HashMap<>();
        osConstraints = new HashMap<>();
    }

    public void addHostingConstraint(String name, List<String> hostingConstraint) {
        hostingConstraints.put(name, hostingConstraint);
    }

    public void addResourceConstraint(String name, List<String> resourceConstraint) {
        resourceConstraints.put(name, resourceConstraint);
    }

    public void addOSConstraint(String name, List<String> osConstraint) {
        osConstraints.put(name, osConstraint);
    }
}
