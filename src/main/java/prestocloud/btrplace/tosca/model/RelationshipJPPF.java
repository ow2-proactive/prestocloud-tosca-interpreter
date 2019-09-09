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

import java.util.ArrayList;
import java.util.List;

/**
 * @author ActiveEon Team
 * @since 25/09/18
 */
public class RelationshipJPPF implements Relationship {
    @Getter
    @Setter
    public String fragment;
    @Getter @Setter
    public String node;
    @Getter @Setter
    public String master;
    @Setter
    public ConstrainedNode hostingNode;
    @Getter @Setter
    public ConstrainedNode hostingMaster;

    public RelationshipJPPF(String fragment, String node, String master) {
        this.fragment = fragment;
        this.node = node;
        this.master = master;
    }

    public RelationshipJPPF(String fragment, String node, String master, ConstrainedNode hostingNode, ConstrainedNode hostingMaster) {
        this.fragment = fragment;
        this.node = node;
        this.master = master;
        this.hostingNode = hostingNode;
        this.hostingMaster = hostingMaster;
    }

    public String getFragmentName() {
        return fragment;
    }

    public ConstrainedNode getHostingNode() {
        return hostingNode;
    }

    public List<ConstrainedNode> getAllConstrainedNodes() {
        List<ConstrainedNode> constrainedNodes = new ArrayList<>();
        constrainedNodes.add(hostingNode);
        constrainedNodes.add(hostingMaster);
        return constrainedNodes;
    }
}
