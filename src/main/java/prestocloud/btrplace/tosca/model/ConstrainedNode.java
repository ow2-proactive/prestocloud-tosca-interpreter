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
public class ConstrainedNode {

    @Getter @Setter
    public String name;
    @Getter @Setter
    public String type;
    @Getter @Setter
    public List<String> derivedTypes;
    @Getter @Setter
    public List<NodeConstraints> constraints;

    public ConstrainedNode(String name, String type, List<String> derivedTypes) {
        this.name = name;
        this.type = type;
        this.derivedTypes = derivedTypes;
        this.constraints = new ArrayList<>();
    }

    public void addConstraints(NodeConstraints nodeConstraints) {
        constraints.add(nodeConstraints);
    }
}
