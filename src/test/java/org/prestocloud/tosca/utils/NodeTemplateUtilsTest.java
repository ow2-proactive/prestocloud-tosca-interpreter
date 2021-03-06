package org.prestocloud.tosca.utils;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.prestocloud.tosca.utils.NodeTemplateUtils.getCapabilityByType;
import static org.prestocloud.tosca.utils.NodeTemplateUtils.getCapabilityByTypeOrFail;

import java.util.Set;

import javax.annotation.Resource;

import org.assertj.core.util.Maps;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.prestocloud.tosca.model.CSARDependency;
import org.prestocloud.tosca.model.templates.Capability;
import org.prestocloud.tosca.model.templates.NodeTemplate;
import org.prestocloud.tosca.model.types.CapabilityType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.hateoas.HypermediaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Profile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import prestocloud.component.ICSARRepositorySearchService;
import prestocloud.exceptions.NotFoundException;
import prestocloud.tosca.context.ToscaContextualAspect;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
@ActiveProfiles("org.prestocloud.tosca.utils.NodeTemplateUtilsTest")
@DirtiesContext
public class NodeTemplateUtilsTest {
    @Configuration
    @Profile("org.prestocloud.tosca.utils.NodeTemplateUtilsTest")
    @EnableAutoConfiguration(exclude = { HypermediaAutoConfiguration.class })
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    @ComponentScan(basePackages = { "prestocloud.tosca.context" })
    static class ContextConfiguration {
        @Bean
        public ICSARRepositorySearchService csarRepositorySearchService() {
            return Mockito.mock(ICSARRepositorySearchService.class);
        }

        @Bean
        public ToscaContextualAspect toscaContextualAspect() {
            return new ToscaContextualAspect();
        }
    }

    @Resource
    private ICSARRepositorySearchService csarRepositorySearchService;
    @Resource
    private ToscaContextualAspect toscaContextualAspect;

    @Test
    public void getCapabilityByTypeTest() {
        NodeTemplate nodeTemplate = new NodeTemplate();
        Capability nodeCapability = new Capability("org.prestocloud.capabilities.SampleCapability", null);
        nodeTemplate.setCapabilities(Maps.newHashMap("test", nodeCapability));
        // if the capability type exactly equals then no tosca context and request is required
        Capability capability = getCapabilityByType(nodeTemplate, "org.prestocloud.capabilities.SampleCapability");
        assertSame(nodeCapability, capability);

        // if the capability derives from parent type then a TOSCA context and query is required to fetch the type.
        CapabilityType capabilityType = new CapabilityType();
        capabilityType.setElementId("org.prestocloud.capabilities.SampleCapability");
        capabilityType.setDerivedFrom(Lists.newArrayList("org.prestocloud.capabilities.TestCapability"));
        Mockito.reset(csarRepositorySearchService);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class),
                Mockito.eq("org.prestocloud.capabilities.SampleCapability"), Mockito.any(Set.class))).thenReturn(capabilityType);

        capability = toscaContextualAspect.execInToscaContext(() -> getCapabilityByType(nodeTemplate, "org.prestocloud.capabilities.TestCapability"), false,
                Sets.newHashSet(new CSARDependency("org.prestocloud.testArchive", "1.0.0-SNAPSHOT")));
        assertSame(nodeCapability, capability);
    }

    @Test
    public void getMissingCapabilityByTypeTest() {
        NodeTemplate nodeTemplate = new NodeTemplate();
        Capability nodeCapability = new Capability("org.prestocloud.capabilities.SampleCapability", null);
        nodeTemplate.setCapabilities(Maps.newHashMap("test", nodeCapability));
        // if the capability derives from parent type then a TOSCA context and query is required to fetch the type.
        CapabilityType capabilityType = new CapabilityType();
        capabilityType.setElementId("org.prestocloud.capabilities.SampleCapability");
        capabilityType.setDerivedFrom(Lists.newArrayList("org.prestocloud.capabilities.TestCapability"));
        Mockito.reset(csarRepositorySearchService);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class),
                Mockito.eq("org.prestocloud.capabilities.SampleCapability"), Mockito.any(Set.class))).thenReturn(capabilityType);

        Capability capability = toscaContextualAspect.execInToscaContext(() -> getCapabilityByType(nodeTemplate, "org.prestocloud.capabilities.Unknown"), false,
                Sets.newHashSet(new CSARDependency("org.prestocloud.testArchive", "1.0.0-SNAPSHOT")));
        assertNull(capability);
    }

    @Test
    public void getCapabilityByTypeOrFailTest() {
        NodeTemplate nodeTemplate = new NodeTemplate();
        Capability nodeCapability = new Capability("org.prestocloud.capabilities.SampleCapability", null);
        nodeTemplate.setCapabilities(Maps.newHashMap("test", nodeCapability));
        // if the capability type exactly equals then no tosca context and request is required
        Capability capability = getCapabilityByTypeOrFail(nodeTemplate, "org.prestocloud.capabilities.SampleCapability");
        assertSame(nodeCapability, capability);

        // if the capability derives from parent type then a TOSCA context and query is required to fetch the type.
        CapabilityType capabilityType = new CapabilityType();
        capabilityType.setElementId("org.prestocloud.capabilities.SampleCapability");
        capabilityType.setDerivedFrom(Lists.newArrayList("org.prestocloud.capabilities.TestCapability"));
        Mockito.reset(csarRepositorySearchService);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class),
                Mockito.eq("org.prestocloud.capabilities.SampleCapability"), Mockito.any(Set.class))).thenReturn(capabilityType);

        capability = toscaContextualAspect.execInToscaContext(() -> getCapabilityByTypeOrFail(nodeTemplate, "org.prestocloud.capabilities.TestCapability"),
                false, Sets.newHashSet(new CSARDependency("org.prestocloud.testArchive", "1.0.0-SNAPSHOT")));
        assertSame(nodeCapability, capability);
    }

    @Test(expected = NotFoundException.class)
    public void getMissingCapabilityByTypeOrFailTest() {
        NodeTemplate nodeTemplate = new NodeTemplate();
        Capability nodeCapability = new Capability("org.prestocloud.capabilities.SampleCapability", null);
        nodeTemplate.setCapabilities(Maps.newHashMap("test", nodeCapability));
        // if the capability derives from parent type then a TOSCA context and query is required to fetch the type.
        CapabilityType capabilityType = new CapabilityType();
        capabilityType.setElementId("org.prestocloud.capabilities.SampleCapability");
        capabilityType.setDerivedFrom(Lists.newArrayList("org.prestocloud.capabilities.TestCapability"));
        Mockito.reset(csarRepositorySearchService);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class),
                Mockito.eq("org.prestocloud.capabilities.SampleCapability"), Mockito.any(Set.class))).thenReturn(capabilityType);

        Capability capability = toscaContextualAspect.execInToscaContext(() -> getCapabilityByTypeOrFail(nodeTemplate, "org.prestocloud.capabilities.Unknown"),
                false, Sets.newHashSet(new CSARDependency("org.prestocloud.testArchive", "1.0.0-SNAPSHOT")));
        assertNull(capability);
    }
}
