package prestocloud.tosca.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.prestocloud.tosca.model.CSARDependency;
import org.prestocloud.tosca.model.Csar;
import org.prestocloud.tosca.model.definitions.AbstractPropertyValue;
import org.prestocloud.tosca.model.definitions.AttributeDefinition;
import org.prestocloud.tosca.model.definitions.CapabilityDefinition;
import org.prestocloud.tosca.model.definitions.ComplexPropertyValue;
import org.prestocloud.tosca.model.definitions.ConcatPropertyValue;
import org.prestocloud.tosca.model.definitions.DeploymentArtifact;
import org.prestocloud.tosca.model.definitions.FunctionPropertyValue;
import org.prestocloud.tosca.model.definitions.IValue;
import org.prestocloud.tosca.model.definitions.ImplementationArtifact;
import org.prestocloud.tosca.model.definitions.Interface;
import org.prestocloud.tosca.model.definitions.ListPropertyValue;
import org.prestocloud.tosca.model.definitions.Operation;
import org.prestocloud.tosca.model.definitions.PropertyConstraint;
import org.prestocloud.tosca.model.definitions.PropertyDefinition;
import org.prestocloud.tosca.model.definitions.PropertyValue;
import org.prestocloud.tosca.model.definitions.RequirementDefinition;
import org.prestocloud.tosca.model.definitions.ScalarPropertyValue;
import org.prestocloud.tosca.model.definitions.constraints.GreaterThanConstraint;
import org.prestocloud.tosca.model.definitions.constraints.LessThanConstraint;
import org.prestocloud.tosca.model.definitions.constraints.MaxLengthConstraint;
import org.prestocloud.tosca.model.definitions.constraints.MinLengthConstraint;
import org.prestocloud.tosca.model.templates.Capability;
import org.prestocloud.tosca.model.templates.NodeTemplate;
import org.prestocloud.tosca.model.types.AbstractInstantiableToscaType;
import org.prestocloud.tosca.model.types.ArtifactType;
import org.prestocloud.tosca.model.types.CapabilityType;
import org.prestocloud.tosca.model.types.DataType;
import org.prestocloud.tosca.model.types.NodeType;
import org.prestocloud.tosca.model.types.PolicyType;
import org.prestocloud.tosca.model.types.RelationshipType;
import org.prestocloud.tosca.normative.constants.NormativeCapabilityTypes;
import org.prestocloud.tosca.normative.constants.NormativeCredentialConstant;
import org.prestocloud.tosca.normative.constants.NormativeTypesConstant;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import prestocloud.component.ICSARRepositorySearchService;
import prestocloud.paas.plan.ToscaNodeLifecycleConstants;
import prestocloud.tosca.model.ArchiveRoot;
import prestocloud.tosca.parser.impl.ErrorCode;
import prestocloud.utils.MapUtil;

public class ToscaParserTest extends AbstractToscaParserSimpleProfileTest {

    @Override
    protected String getRootDirectory() {
        return "src/test/resources/parser/";
    }

    @Override
    protected String getToscaVersion() {
        return "tosca_prestocloud_mapping_1_2";
    }

    @Resource
    private ICSARRepositorySearchService repositorySearchService;

    @SuppressWarnings("unchecked")
    @Test
    public void testBadOccurrence() throws FileNotFoundException, ParsingException {
        NodeType mockedResult = Mockito.mock(NodeType.class);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.SoftwareComponent"),
                Mockito.any(Set.class))).thenReturn(mockedResult);
        Mockito.when(mockedResult.getDerivedFrom()).thenReturn(Lists.newArrayList("tosca.nodes.Root"));
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Root"), Mockito.any(Set.class)))
                .thenReturn(mockedResult);

        Mockito.when(
                csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Compute"), Mockito.any(Set.class)))
                .thenReturn(mockedResult);
        RelationshipType hostedOn = new RelationshipType();
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(RelationshipType.class), Mockito.eq("tosca.relationships.HostedOn"),
                Mockito.any(Set.class))).thenReturn(hostedOn);
        CapabilityType mockedCapabilityResult = Mockito.mock(CapabilityType.class);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class), Mockito.eq("tosca.capabilities.Endpoint"),
                Mockito.any(Set.class))).thenReturn(mockedCapabilityResult);
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-node-type-invalid-occurrence.yml"));

        Assert.assertEquals(2, countErrorByLevelAndCode(parsingResult, ParsingErrorLevel.ERROR, ErrorCode.SYNTAX_ERROR));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRelationshipType() throws FileNotFoundException, ParsingException {
        RelationshipType mockedResult = Mockito.mock(RelationshipType.class);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(RelationshipType.class), Mockito.eq("tosca.relationships.Relationship"),
                Mockito.any(Set.class))).thenReturn(mockedResult);
        Mockito.when(mockedResult.getDerivedFrom()).thenReturn(Lists.newArrayList("tosca.capabilities.Root"));

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-relationship-type.yml"));
        ParserTestUtil.displayErrors(parsingResult);
        assertNoBlocker(parsingResult);
        ArchiveRoot archiveRoot = parsingResult.getResult();
        Assert.assertNotNull(archiveRoot.getArchive());
        Assert.assertEquals(getToscaVersion(), archiveRoot.getArchive().getToscaDefinitionsVersion());
        Assert.assertEquals(1, archiveRoot.getRelationshipTypes().size());
        Map.Entry<String, RelationshipType> entry = archiveRoot.getRelationshipTypes().entrySet().iterator().next();
        Assert.assertEquals("mycompany.mytypes.myapplication.MyRelationship", entry.getKey());
        RelationshipType relationship = entry.getValue();
        Assert.assertEquals(Lists.newArrayList("tosca.relationships.Relationship", "tosca.capabilities.Root"), relationship.getDerivedFrom());
        Assert.assertEquals("a custom relationship", relationship.getDescription());

        // properties
        Assert.assertEquals(2, relationship.getProperties().size());
        Assert.assertTrue(relationship.getProperties().containsKey("my_feature_setting"));
        PropertyDefinition pd = relationship.getProperties().get("my_feature_setting");
        Assert.assertEquals("string", pd.getType());
        Assert.assertTrue(relationship.getProperties().containsKey("my_feature_value"));
        pd = relationship.getProperties().get("my_feature_value");
        Assert.assertEquals("integer", pd.getType());

        // valid targets
        Assert.assertEquals(2, relationship.getValidTargets().length);
        Assert.assertEquals("tosca.capabilities.Feature1", relationship.getValidTargets()[0]);
        Assert.assertEquals("tosca.capabilities.Feature2", relationship.getValidTargets()[1]);

    }

    @Test
    public void testDataTypesExtendsNative() throws ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-data-types-extends-native.yml"));
        ParserTestUtil.displayErrors(parsingResult);
        Assert.assertEquals(3, parsingResult.getResult().getDataTypes().size());
        Assert.assertEquals(2, parsingResult.getResult().getNodeTypes().size());
        Assert.assertEquals(0, parsingResult.getContext().getParsingErrors().size());
        Assert.assertEquals(1, parsingResult.getResult().getTopology().getNodeTemplates().size());
        NodeTemplate nodeTemplate = parsingResult.getResult().getTopology().getNodeTemplates().values().iterator().next();
        Assert.assertEquals(3, nodeTemplate.getProperties().size());
        // check url property
        Assert.assertTrue(nodeTemplate.getProperties().containsKey("url"));
        AbstractPropertyValue url = nodeTemplate.getProperties().get("url");
        Assert.assertTrue(url instanceof ScalarPropertyValue);
        Assert.assertEquals("https://prestocloud.com", ((ScalarPropertyValue) url).getValue());
        // check ipv6_addresses property
        Assert.assertTrue(nodeTemplate.getProperties().containsKey("ipv6_addresses"));
        AbstractPropertyValue ipv6_addresses = nodeTemplate.getProperties().get("ipv6_addresses");
        Assert.assertTrue(ipv6_addresses instanceof ListPropertyValue);
        List<Object> ipv6_addresses_list = ((ListPropertyValue) ipv6_addresses).getValue();
        Assert.assertEquals(2, ipv6_addresses_list.size());
        Assert.assertEquals("192.168.0.10", ipv6_addresses_list.get(0));
        Assert.assertEquals("10.0.0.10", ipv6_addresses_list.get(1));
        // check passwords property
        Assert.assertTrue(nodeTemplate.getProperties().containsKey("passwords"));
        AbstractPropertyValue passwords = nodeTemplate.getProperties().get("passwords");
        Assert.assertTrue(passwords instanceof ComplexPropertyValue);
        Map<String, Object> passwords_map = ((ComplexPropertyValue) passwords).getValue();
        Assert.assertEquals(2, passwords_map.size());
        Assert.assertEquals("123456789", passwords_map.get("user1"));
        Assert.assertEquals("abcdefghij", passwords_map.get("user2"));
    }

    @Test
    public void testDataTypesExtendsNativeWithError1() throws ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-data-types-extends-native-error1.yml"));
        ParserTestUtil.displayErrors(parsingResult);
        Assert.assertEquals(3, parsingResult.getResult().getDataTypes().size());
        Assert.assertEquals(2, parsingResult.getResult().getNodeTypes().size());
        Assert.assertEquals(1, parsingResult.getContext().getParsingErrors().size());
    }

    @Test
    public void testDataTypesExtendsNativeWithError2() throws ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-data-types-extends-native-error2.yml"));
        ParserTestUtil.displayErrors(parsingResult);
        Assert.assertEquals(3, parsingResult.getResult().getDataTypes().size());
        Assert.assertEquals(2, parsingResult.getResult().getNodeTypes().size());
        Assert.assertEquals(1, parsingResult.getContext().getParsingErrors().size());
    }

    @Test
    public void testDataTypesExtendsNativeWithError3() throws ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-data-types-extends-native-error3.yml"));
        ParserTestUtil.displayErrors(parsingResult);
        Assert.assertEquals(3, parsingResult.getResult().getDataTypes().size());
        Assert.assertEquals(2, parsingResult.getResult().getNodeTypes().size());
        Assert.assertEquals(1, parsingResult.getContext().getParsingErrors().size());
    }

    @Test
    public void testDataTypesComplexWithDefault() throws ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-data-types-complex-default.yml"));
        ParserTestUtil.displayErrors(parsingResult);
        Assert.assertEquals(2, parsingResult.getResult().getDataTypes().size());
        Assert.assertEquals(2, parsingResult.getResult().getNodeTypes().size());
        Assert.assertEquals(0, parsingResult.getContext().getParsingErrors().size());
        NodeType commandType = parsingResult.getResult().getNodeTypes().get("prestocloud.test.Command");
        Assert.assertNotNull(commandType);
        PropertyDefinition pd = commandType.getProperties().get("customer");
        Assert.assertNotNull(pd);
        // check the default value
        Object defaultValue = pd.getDefault();
        Assert.assertNotNull(defaultValue);
        Assert.assertTrue(defaultValue instanceof ComplexPropertyValue);
        ComplexPropertyValue cpv = (ComplexPropertyValue) defaultValue;
        Map<String, Object> valueAsMap = cpv.getValue();
        Assert.assertNotNull(valueAsMap);
        Assert.assertTrue(valueAsMap.containsKey("first_name"));
        Assert.assertEquals("Foo", valueAsMap.get("first_name"));
        Assert.assertTrue(valueAsMap.containsKey("last_name"));
        Assert.assertEquals("Bar", valueAsMap.get("last_name"));
        Assert.assertEquals(1, parsingResult.getResult().getTopology().getNodeTemplates().size());
        NodeTemplate nodeTemplate = parsingResult.getResult().getTopology().getNodeTemplates().values().iterator().next();
        // on the node, the default value should be set
        Assert.assertNotNull(nodeTemplate.getProperties());
        Assert.assertTrue(nodeTemplate.getProperties().containsKey("customer"));
        AbstractPropertyValue apv = nodeTemplate.getProperties().get("customer");
        Assert.assertNotNull(apv);
        Assert.assertTrue(apv instanceof ComplexPropertyValue);
        cpv = (ComplexPropertyValue) apv;
        valueAsMap = cpv.getValue();
        Assert.assertNotNull(valueAsMap);
        Assert.assertTrue(valueAsMap.containsKey("first_name"));
        Assert.assertEquals("Foo", valueAsMap.get("first_name"));
        Assert.assertTrue(valueAsMap.containsKey("last_name"));
        Assert.assertEquals("Bar", valueAsMap.get("last_name"));
    }

    @Test
    public void testDataTypesVeryComplexWithDefault() throws ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-data-types-very-complex-default.yml"));
        ParserTestUtil.displayErrors(parsingResult);
        Assert.assertEquals(3, parsingResult.getResult().getDataTypes().size());
        Assert.assertEquals(2, parsingResult.getResult().getNodeTypes().size());
        Assert.assertEquals(0, parsingResult.getContext().getParsingErrors().size());
        NodeType commandType = parsingResult.getResult().getNodeTypes().get("prestocloud.test.Command");
        Assert.assertNotNull(commandType);
        PropertyDefinition pd = commandType.getProperties().get("customer");
        Assert.assertNotNull(pd);
        // check the default value
        Object defaultValue = pd.getDefault();
        Assert.assertNotNull(defaultValue);
        Assert.assertTrue(defaultValue instanceof ComplexPropertyValue);
        ComplexPropertyValue cpv = (ComplexPropertyValue) defaultValue;
        Map<String, Object> valueAsMap = cpv.getValue();
        Assert.assertNotNull(valueAsMap);
        Assert.assertTrue(valueAsMap.containsKey("first_name"));
        Assert.assertEquals("Foo", valueAsMap.get("first_name"));
        Assert.assertTrue(valueAsMap.containsKey("last_name"));
        Assert.assertEquals("Bar", valueAsMap.get("last_name"));
        Assert.assertTrue(valueAsMap.containsKey("address"));
        Object addressObj = valueAsMap.get("address");
        Assert.assertNotNull(addressObj);
        Assert.assertTrue(addressObj instanceof Map);
        Map<String, Object> addressMap = (Map<String, Object>) addressObj;
        Assert.assertTrue(addressMap.containsKey("street_name"));
        Assert.assertEquals("rue des peupliers", addressMap.get("street_name"));
        Assert.assertTrue(addressMap.containsKey("zipcode"));
        Assert.assertEquals("92130", addressMap.get("zipcode"));
        Assert.assertTrue(addressMap.containsKey("city_name"));
        Assert.assertEquals("ISSY LES MOULES", addressMap.get("city_name"));
        Assert.assertTrue(valueAsMap.containsKey("emails"));
        Object emailsObj = valueAsMap.get("emails");
        Assert.assertNotNull(emailsObj);
        Assert.assertTrue(emailsObj instanceof List);
        List<Object> emailsList = (List<Object>) emailsObj;
        Assert.assertEquals(2, emailsList.size());
        Assert.assertEquals("contact@fastconnect.fr", emailsList.get(0));
        Assert.assertEquals("info@fastconnect.fr", emailsList.get(1));
        Object accountsObj = valueAsMap.get("accounts");
        Assert.assertNotNull(accountsObj);
        Assert.assertTrue(accountsObj instanceof Map);
        Map<String, Object> accountsMap = (Map<String, Object>) accountsObj;
        Assert.assertEquals(2, accountsMap.size());
        Assert.assertTrue(accountsMap.containsKey("main"));
        Assert.assertEquals("root", accountsMap.get("main"));
        Assert.assertTrue(accountsMap.containsKey("secondary"));
        Assert.assertEquals("user", accountsMap.get("secondary"));
        Assert.assertEquals(1, parsingResult.getResult().getTopology().getNodeTemplates().size());
        NodeTemplate nodeTemplate = parsingResult.getResult().getTopology().getNodeTemplates().values().iterator().next();
        // on the node, the default value should be set
        Assert.assertNotNull(nodeTemplate.getProperties());
        Assert.assertTrue(nodeTemplate.getProperties().containsKey("customer"));
        AbstractPropertyValue apv = nodeTemplate.getProperties().get("customer");
        Assert.assertNotNull(apv);
        Assert.assertTrue(apv instanceof ComplexPropertyValue);
        cpv = (ComplexPropertyValue) apv;
        valueAsMap = cpv.getValue();
        Assert.assertNotNull(valueAsMap);
        Assert.assertTrue(valueAsMap.containsKey("first_name"));
        Assert.assertEquals("Foo", valueAsMap.get("first_name"));
        Assert.assertTrue(valueAsMap.containsKey("last_name"));
        Assert.assertEquals("Bar", valueAsMap.get("last_name"));
        Assert.assertTrue(valueAsMap.containsKey("address"));
        addressObj = valueAsMap.get("address");
        Assert.assertNotNull(addressObj);
        Assert.assertTrue(addressObj instanceof Map);
        addressMap = (Map<String, Object>) addressObj;
        Assert.assertTrue(addressMap.containsKey("street_name"));
        Assert.assertEquals("rue des peupliers", addressMap.get("street_name"));
        Assert.assertTrue(addressMap.containsKey("zipcode"));
        Assert.assertEquals("92130", addressMap.get("zipcode"));
        Assert.assertTrue(addressMap.containsKey("city_name"));
        Assert.assertEquals("ISSY LES MOULES", addressMap.get("city_name"));
        Assert.assertTrue(valueAsMap.containsKey("emails"));
        emailsObj = valueAsMap.get("emails");
        Assert.assertNotNull(emailsObj);
        Assert.assertTrue(emailsObj instanceof List);
        emailsList = (List<Object>) emailsObj;
        Assert.assertEquals(2, emailsList.size());
        Assert.assertEquals("contact@fastconnect.fr", emailsList.get(0));
        Assert.assertEquals("info@fastconnect.fr", emailsList.get(1));
        accountsObj = valueAsMap.get("accounts");
        Assert.assertNotNull(accountsObj);
        Assert.assertTrue(accountsObj instanceof Map);
        accountsMap = (Map<String, Object>) accountsObj;
        Assert.assertEquals(2, accountsMap.size());
        Assert.assertTrue(accountsMap.containsKey("main"));
        Assert.assertEquals("root", accountsMap.get("main"));
        Assert.assertTrue(accountsMap.containsKey("secondary"));
        Assert.assertEquals("user", accountsMap.get("secondary"));
    }

    @Test
    public void testDataTypesVeryComplexWithDefaultError1() throws ParsingException, IOException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-data-types-very-complex-default-error1.yml"));
        ParserTestUtil.displayErrors(parsingResult);
        Assert.assertEquals(3, parsingResult.getResult().getDataTypes().size());
        Assert.assertEquals(2, parsingResult.getResult().getNodeTypes().size());
        Assert.assertEquals(1, parsingResult.getContext().getParsingErrors().size());
    }

    @Test
    public void testDataTypesVeryComplexWithDefaultError2() throws ParsingException, IOException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-data-types-very-complex-default-error2.yml"));
        ParserTestUtil.displayErrors(parsingResult);
        Assert.assertEquals(3, parsingResult.getResult().getDataTypes().size());
        Assert.assertEquals(2, parsingResult.getResult().getNodeTypes().size());
        Assert.assertEquals(1, parsingResult.getContext().getParsingErrors().size());
    }

    @Test
    public void testDataTypesVeryComplexWithDefaultError3() throws ParsingException, IOException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-data-types-very-complex-default-error3.yml"));
        ParserTestUtil.displayErrors(parsingResult);
        Assert.assertEquals(3, parsingResult.getResult().getDataTypes().size());
        Assert.assertEquals(2, parsingResult.getResult().getNodeTypes().size());
        Assert.assertEquals(1, parsingResult.getContext().getParsingErrors().size());
    }

    @Test
    public void testDataTypesVeryComplexWithDefaultError4() throws ParsingException, IOException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-data-types-very-complex-default-error4.yml"));
        ParserTestUtil.displayErrors(parsingResult);
        Assert.assertEquals(3, parsingResult.getResult().getDataTypes().size());
        Assert.assertEquals(2, parsingResult.getResult().getNodeTypes().size());
        Assert.assertEquals(2, parsingResult.getContext().getParsingErrors().size());
    }

    /********************************/

    @Test(expected = ParsingException.class)
    public void testDefinitionVersionInvalidYaml() throws FileNotFoundException, ParsingException {
        parser.parseFile(Paths.get(getRootDirectory(), "tosca-definition-version-invalid.yml"));
    }

    @Test(expected = ParsingException.class)
    public void testDefinitionVersionUnknown() throws FileNotFoundException, ParsingException {
        parser.parseFile(Paths.get(getRootDirectory(), "tosca-definition-version-unknown.yml"));
    }


    @Test
    public void testDescriptionSingleLine() throws FileNotFoundException, ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "description-single-line.yml"));
        Assert.assertEquals(0, parsingResult.getContext().getParsingErrors().size());
        ArchiveRoot archiveRoot = parsingResult.getResult();
        assertNotNull(archiveRoot.getArchive());
        Assert.assertEquals(getToscaVersion(), archiveRoot.getArchive().getToscaDefinitionsVersion());
        assertNotNull(archiveRoot.getArchive().getDescription());
        Assert.assertEquals("This is an example of a single line description (no folding).", archiveRoot.getArchive().getDescription());
    }

    @Test
    public void testDescriptionMultiLine() throws FileNotFoundException, ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "description-multi-line.yml"));
        Assert.assertEquals(0, parsingResult.getContext().getParsingErrors().size());
        ArchiveRoot archiveRoot = parsingResult.getResult();
        assertNotNull(archiveRoot.getArchive());
        Assert.assertEquals(getToscaVersion(), archiveRoot.getArchive().getToscaDefinitionsVersion());
        assertNotNull(archiveRoot.getArchive().getDescription());
        Assert.assertEquals(
                "This is an example of a multi-line description using YAML. It permits for line breaks for easier readability...\nif needed.  However, (multiple) line breaks are folded into a single space character when processed into a single string value.",
                archiveRoot.getArchive().getDescription());
    }

    @Test
    public void testRootCategories() throws FileNotFoundException, ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-root-categories.yml"));
        assertNoBlocker(parsingResult);
        ArchiveRoot archiveRoot = parsingResult.getResult();
        assertNotNull(archiveRoot.getArchive());
        Assert.assertEquals(getToscaVersion(), archiveRoot.getArchive().getToscaDefinitionsVersion());
        Assert.assertEquals("Tosca default namespace value", archiveRoot.getArchive().getToscaDefaultNamespace());
        Assert.assertEquals("Template name value", archiveRoot.getArchive().getName());
        Assert.assertEquals("Temlate author value", archiveRoot.getArchive().getTemplateAuthor());
        Assert.assertEquals("1.0.0-SNAPSHOT", archiveRoot.getArchive().getVersion());
        Assert.assertEquals("This is an example of a single line description (no folding).", archiveRoot.getArchive().getDescription());
    }

    @Test
    public void testImportDependency() throws FileNotFoundException, ParsingException {
        Csar csar = new Csar("tosca-normative-types", "1.2");
        Mockito.when(csarRepositorySearchService.getArchive(csar.getName(), csar.getVersion())).thenReturn(csar);

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-import-dependency.yml"));

        Mockito.verify(csarRepositorySearchService).getArchive(csar.getName(), csar.getVersion());

        assertNoBlocker(parsingResult);
        ArchiveRoot archiveRoot = parsingResult.getResult();
        assertNotNull(archiveRoot.getArchive());
        assertNotNull(archiveRoot.getArchive().getDependencies());
        Assert.assertEquals(1, archiveRoot.getArchive().getDependencies().size());
        Assert.assertEquals(new CSARDependency(csar.getName(), csar.getVersion()), archiveRoot.getArchive().getDependencies().iterator().next());
    }

    @Test
    public void testImportDependencyMissing() throws FileNotFoundException, ParsingException {
        Csar csar = new Csar("tosca-normative-types", "1.2");
        Mockito.when(csarRepositorySearchService.getArchive(csar.getName(), csar.getVersion())).thenReturn(null);

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-import-dependency.yml"));

        Mockito.verify(csarRepositorySearchService).getArchive(csar.getName(), csar.getVersion());

        assertNoBlocker(parsingResult);
        ArchiveRoot archiveRoot = parsingResult.getResult();
        assertNotNull(archiveRoot.getArchive());
        assertNotNull(archiveRoot.getArchive().getDependencies());
        Assert.assertEquals(0, archiveRoot.getArchive().getDependencies().size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testArtifactType() throws FileNotFoundException, ParsingException {
        ArtifactType mockedResult = Mockito.mock(ArtifactType.class);
        Mockito.when(
                csarRepositorySearchService.getElementInDependencies(Mockito.eq(ArtifactType.class), Mockito.eq("tosca.artifact.Root"), Mockito.any(Set.class)))
                .thenReturn(mockedResult);
        List<String> derivedFromSet = Lists.newArrayList();
        Mockito.when(mockedResult.getDerivedFrom()).thenReturn(derivedFromSet);

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-artifact-type.yml"));

        assertNoBlocker(parsingResult);
        ArchiveRoot archiveRoot = parsingResult.getResult();
        assertNotNull(archiveRoot.getArchive());
        Assert.assertEquals(getToscaVersion(), archiveRoot.getArchive().getToscaDefinitionsVersion());
        Assert.assertEquals(1, archiveRoot.getArtifactTypes().size());
        Map.Entry<String, ArtifactType> entry = archiveRoot.getArtifactTypes().entrySet().iterator().next();
        Assert.assertEquals("my_artifact_type", entry.getKey());
        ArtifactType artifact = entry.getValue();
        Assert.assertEquals(Lists.newArrayList("tosca.artifact.Root"), artifact.getDerivedFrom());
        Assert.assertEquals("Java Archive artifact type", artifact.getDescription());
        Assert.assertEquals("application/java-archive", artifact.getMimeType());
        Assert.assertEquals(Lists.newArrayList("jar"), artifact.getFileExt());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCapabilityType() throws FileNotFoundException, ParsingException {
        CapabilityType mockedResult = Mockito.mock(CapabilityType.class);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class), Mockito.eq("tosca.capabilities.Feature"),
                Mockito.any(Set.class))).thenReturn(mockedResult);
        Mockito.when(mockedResult.getDerivedFrom()).thenReturn(Lists.newArrayList("tosca.capabilities.Root"));

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-capability-type.yml"));
        assertNoBlocker(parsingResult);
        ArchiveRoot archiveRoot = parsingResult.getResult();
        assertNotNull(archiveRoot.getArchive());
        Assert.assertEquals(getToscaVersion(), archiveRoot.getArchive().getToscaDefinitionsVersion());
        Assert.assertEquals(1, archiveRoot.getCapabilityTypes().size());
        Map.Entry<String, CapabilityType> entry = archiveRoot.getCapabilityTypes().entrySet().iterator().next();
        Assert.assertEquals("mycompany.mytypes.myapplication.MyFeature", entry.getKey());
        CapabilityType capability = entry.getValue();
        Assert.assertEquals(Lists.newArrayList("tosca.capabilities.Feature", "tosca.capabilities.Root"), capability.getDerivedFrom());
        Assert.assertEquals("a custom feature of my company???s application", capability.getDescription());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testNodeType() throws FileNotFoundException, ParsingException {
        NodeType mockedResult = Mockito.mock(NodeType.class);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.SoftwareComponent"),
                Mockito.any(Set.class))).thenReturn(mockedResult);
        Mockito.when(mockedResult.getDerivedFrom()).thenReturn(Lists.newArrayList("tosca.nodes.Root"));
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Root"), Mockito.any(Set.class)))
                .thenReturn(mockedResult);

        Mockito.when(
                csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Compute"), Mockito.any(Set.class)))
                .thenReturn(mockedResult);
        CapabilityType mockedCapabilityResult = Mockito.mock(CapabilityType.class);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class),
                Mockito.eq("mytypes.mycapabilities.MyCapabilityTypeName"), Mockito.any(Set.class))).thenReturn(mockedCapabilityResult);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class),
                Mockito.eq("mytypes.mycapabilities.MyCapabilityTypeName"), Mockito.any(Set.class))).thenReturn(mockedCapabilityResult);

        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class), Mockito.eq("tosca.capabilities.Endpoint"),
                Mockito.any(Set.class))).thenReturn(mockedCapabilityResult);
        RelationshipType hostedOn = new RelationshipType();
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(RelationshipType.class), Mockito.eq("tosca.relationships.HostedOn"),
                Mockito.any(Set.class))).thenReturn(hostedOn);

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-node-type.yml"));
        ParserTestUtil.displayErrors(parsingResult);
        assertNoBlocker(parsingResult);
        ArchiveRoot archiveRoot = parsingResult.getResult();
        assertNotNull(archiveRoot.getArchive());
        Assert.assertEquals(getToscaVersion(), archiveRoot.getArchive().getToscaDefinitionsVersion());
        Assert.assertEquals(1, archiveRoot.getNodeTypes().size());
        // check node type.
        Map.Entry<String, NodeType> entry = archiveRoot.getNodeTypes().entrySet().iterator().next();

        Assert.assertEquals("my_company.my_types.MyAppNodeType", entry.getKey());
        NodeType nodeType = entry.getValue();

        Assert.assertEquals(Lists.newArrayList("tosca.nodes.SoftwareComponent", "tosca.nodes.Root"), nodeType.getDerivedFrom());
        Assert.assertEquals("My company's custom applicaton", nodeType.getDescription());

        // validate properties parsing
        Assert.assertEquals(4, nodeType.getProperties().size());

        PropertyDefinition def1 = new PropertyDefinition();
        def1.setType("string");
        def1.setDefault(new ScalarPropertyValue("default"));
        def1.setDescription("application password");
        List<PropertyConstraint> constraints = Lists.newArrayList();
        constraints.add(new MinLengthConstraint(6));
        constraints.add(new MaxLengthConstraint(10));
        def1.setConstraints(constraints);

        PropertyDefinition def2 = new PropertyDefinition();
        def2.setType("integer");
        def2.setDescription("application port number");

        PropertyDefinition def3 = new PropertyDefinition();
        def3.setType("scalar-unit.size");
        def3.setDefault(new ScalarPropertyValue("1 GB"));
        LessThanConstraint ltConstraint = new LessThanConstraint();
        ltConstraint.setLessThan("1 TB");
        constraints = Lists.<PropertyConstraint> newArrayList(ltConstraint);
        def3.setConstraints(constraints);

        PropertyDefinition def4 = new PropertyDefinition();
        def4.setType("scalar-unit.time");
        def4.setDefault(new ScalarPropertyValue("1 d"));
        GreaterThanConstraint gtConstraint = new GreaterThanConstraint();
        gtConstraint.setGreaterThan("1 h");
        constraints = Lists.<PropertyConstraint> newArrayList(gtConstraint);
        def4.setConstraints(constraints);

        Assert.assertEquals(MapUtil.newHashMap(new String[] { "my_app_password", "my_app_duration", "my_app_size", "my_app_port" },
                new PropertyDefinition[] { def1, def4, def3, def2 }), nodeType.getProperties());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testNodeTypeWithCutomInterface() throws FileNotFoundException, ParsingException {
        NodeType mockedResult = Mockito.mock(NodeType.class);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.SoftwareComponent"),
                Mockito.any(Set.class))).thenReturn(mockedResult);
        Mockito.when(mockedResult.getDerivedFrom()).thenReturn(Lists.newArrayList("tosca.nodes.Root"));
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Root"), Mockito.any(Set.class)))
                .thenReturn(mockedResult);

        Mockito.when(
                csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Compute"), Mockito.any(Set.class)))
                .thenReturn(mockedResult);
        CapabilityType mockedCapabilityResult = Mockito.mock(CapabilityType.class);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class),
                Mockito.eq("mytypes.mycapabilities.MyCapabilityTypeName"), Mockito.any(Set.class))).thenReturn(mockedCapabilityResult);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class), Mockito.eq("tosca.capabilities.Container"),
                Mockito.any(Set.class))).thenReturn(mockedCapabilityResult);

        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class), Mockito.eq("tosca.capabilities.Endpoint"),
                Mockito.any(Set.class))).thenReturn(mockedCapabilityResult);
        RelationshipType hostedOn = new RelationshipType();
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(RelationshipType.class), Mockito.eq("tosca.relationships.HostedOn"),
                Mockito.any(Set.class))).thenReturn(hostedOn);

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-node-type-interface-operations.yml"));

        assertNoBlocker(parsingResult);
        ArchiveRoot archiveRoot = parsingResult.getResult();
        assertNotNull(archiveRoot.getArchive());
        Assert.assertEquals(getToscaVersion(), archiveRoot.getArchive().getToscaDefinitionsVersion());
        Assert.assertEquals(1, archiveRoot.getNodeTypes().size());
        // check node type.
        Map.Entry<String, NodeType> entry = archiveRoot.getNodeTypes().entrySet().iterator().next();

        Assert.assertEquals("my_company.my_types.MyAppNodeType", entry.getKey());
        NodeType nodeType = entry.getValue();

        assertNotNull(nodeType.getInterfaces());
        Assert.assertEquals(2, nodeType.getInterfaces().size());
        assertNotNull(nodeType.getInterfaces().get(ToscaNodeLifecycleConstants.STANDARD));
        Interface customInterface = nodeType.getInterfaces().get("custom");
        assertNotNull(customInterface);
        Assert.assertEquals("this is a sample interface used to execute custom operations.", customInterface.getDescription());
        Assert.assertEquals(1, customInterface.getOperations().size());
        Operation operation = customInterface.getOperations().get("do_something");
        assertNotNull(operation);
        Assert.assertEquals(3, operation.getInputParameters().size());
        Assert.assertEquals(ScalarPropertyValue.class, operation.getInputParameters().get("value_input").getClass());
        Assert.assertEquals(PropertyDefinition.class, operation.getInputParameters().get("definition_input").getClass());
        Assert.assertEquals(FunctionPropertyValue.class, operation.getInputParameters().get("function_input").getClass());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAttributesConcatValid() throws Throwable {
        Csar csar = new Csar("tosca-normative-types", "1.2");
        // Mockito.when(csarRepositorySearchService.getArchive(csar.getId())).thenReturn(csar);

        NodeType mockedResult = Mockito.mock(NodeType.class);
        Mockito.when(
                csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Compute"), Mockito.any(Set.class)))
                .thenReturn(mockedResult);
        Mockito.when(mockedResult.getId()).thenReturn("tosca.nodes.Compute:1.0");

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-node-type-inputs.yml"));

        Mockito.verify(csarRepositorySearchService).getArchive(csar.getName(), csar.getVersion());

        assertNoBlocker(parsingResult);
        ArchiveRoot archiveRoot = parsingResult.getResult();
        assertNotNull(archiveRoot.getArchive());
        Assert.assertEquals(getToscaVersion(), archiveRoot.getArchive().getToscaDefinitionsVersion());

        // check nodetype elements
        Map.Entry<String, NodeType> entry = archiveRoot.getNodeTypes().entrySet().iterator().next();
        Assert.assertEquals("prestocloud.test.TestComputeConcat", entry.getKey());
        NodeType nodeType = entry.getValue();
        nodeType.setWorkspace("GLOBAL_WORKSPACE");
        Map<String, IValue> attributes = nodeType.getAttributes();

        IValue simpleDefinition = attributes.get("simple_definition");
        IValue ipAddressDefinition = attributes.get("ip_address");
        IValue simpleConcat = attributes.get("simple_concat");
        IValue complexConcat = attributes.get("complex_concat");

        // check attributes types
        assertTrue(simpleDefinition.getClass().equals(AttributeDefinition.class));
        assertTrue(ipAddressDefinition.getClass().equals(AttributeDefinition.class));
        assertTrue(simpleConcat.getClass().equals(ConcatPropertyValue.class));
        assertTrue(complexConcat.getClass().equals(ConcatPropertyValue.class));
    }

    @Test
    public void testCapabilities() throws ParsingException {
        NodeType mockedResult = Mockito.mock(NodeType.class);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.SoftwareComponent"),
                Mockito.any(Set.class))).thenReturn(mockedResult);
        Mockito.when(mockedResult.getDerivedFrom()).thenReturn(Lists.newArrayList("tosca.nodes.Root"));
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Root"), Mockito.any(Set.class)))
                .thenReturn(mockedResult);

        Mockito.when(
                csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Compute"), Mockito.any(Set.class)))
                .thenReturn(mockedResult);
        CapabilityType mockedCapabilityResult = Mockito.mock(CapabilityType.class);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class), Mockito.eq("tosca.capabilities.Endpoint"),
                Mockito.any(Set.class))).thenReturn(mockedCapabilityResult);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class), Mockito.eq("tosca.capabilities.Container"),
                Mockito.any(Set.class))).thenReturn(mockedCapabilityResult);

        RelationshipType connectsTo = new RelationshipType();
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(RelationshipType.class), Mockito.eq("tosca.relationships.ConnectsTo"),
                Mockito.any(Set.class))).thenReturn(connectsTo);

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "requirement_capabilities.yaml"));
        ParserTestUtil.displayErrors(parsingResult);
        parsingResult.getResult().getNodeTypes().values().forEach(nodeType -> {
            nodeType.getRequirements().forEach(requirementDefinition -> {
                switch (requirementDefinition.getId()) {
                    case "host":
                        Assert.assertEquals("tosca.capabilities.Container", requirementDefinition.getType());
                        break;
                    case "endpoint":
                    case "another_endpoint":
                        Assert.assertEquals("tosca.capabilities.Endpoint", requirementDefinition.getType());
                        Assert.assertEquals(0, requirementDefinition.getLowerBound());
                        Assert.assertEquals(Integer.MAX_VALUE, requirementDefinition.getUpperBound());
                        Assert.assertEquals("tosca.relationships.ConnectsTo", requirementDefinition.getRelationshipType());
                        break;
                }
            });
            nodeType.getCapabilities().forEach(capabilityDefinition -> {
                switch (capabilityDefinition.getId()) {
                    case "host":
                        Assert.assertEquals("tosca.capabilities.Container", capabilityDefinition.getType());
                        break;
                    case "endpoint":
                    case "another_endpoint":
                        Assert.assertEquals("tosca.capabilities.Endpoint", capabilityDefinition.getType());
                        assertNotNull(capabilityDefinition.getDescription());
                }
            });
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetOperationOutputFunction() throws Throwable {
        Csar csar = new Csar("tosca-normative-types", "1.2");
        // Mockito.when(csarRepositorySearchService.getArchive(csar.getId())).thenReturn(csar);

        NodeType mockedResult = Mockito.mock(NodeType.class);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.SoftwareComponent"),
                Mockito.any(Set.class))).thenReturn(mockedResult);
        Mockito.when(mockedResult.getDerivedFrom()).thenReturn(Lists.newArrayList("tosca.nodes.Root"));
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Root"), Mockito.any(Set.class)))
                .thenReturn(mockedResult);

        Mockito.when(
                csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Compute"), Mockito.any(Set.class)))
                .thenReturn(mockedResult);
        CapabilityType mockedCapabilityResult = Mockito.mock(CapabilityType.class);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class),
                Mockito.eq("mytypes.mycapabilities.MyCapabilityTypeName"), Mockito.any(Set.class))).thenReturn(mockedCapabilityResult);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class), Mockito.eq("tosca.capabilities.Container"),
                Mockito.any(Set.class))).thenReturn(mockedCapabilityResult);

        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class), Mockito.eq("tosca.capabilities.Endpoint"),
                Mockito.any(Set.class))).thenReturn(mockedCapabilityResult);
        RelationshipType hostedOn = new RelationshipType();
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(RelationshipType.class), Mockito.eq("tosca.relationships.HostedOn"),
                Mockito.any(Set.class))).thenReturn(hostedOn);

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-functions.yml"));

        Mockito.verify(csarRepositorySearchService).getArchive(csar.getName(), csar.getVersion());

        assertNoBlocker(parsingResult);
        ArchiveRoot archiveRoot = parsingResult.getResult();
        assertNotNull(archiveRoot.getArchive());
        Assert.assertEquals(getToscaVersion(), archiveRoot.getArchive().getToscaDefinitionsVersion());

        // check nodetype elements
        Map.Entry<String, NodeType> entry = archiveRoot.getNodeTypes().entrySet().iterator().next();
        Assert.assertEquals("my_company.my_types.MyAppNodeType", entry.getKey());
        NodeType nodeType = entry.getValue();

        // on input level
        Map<String, Interface> interfaces = nodeType.getInterfaces();
        Interface customInterface = interfaces.get("custom");
        Map<String, IValue> doSomethingInputs = customInterface.getOperations().get("do_something").getInputParameters();
        assertNotNull(doSomethingInputs);
        Assert.assertFalse(doSomethingInputs.isEmpty());
        IValue operationOutput_input = doSomethingInputs.get("operationOutput_input");
        assertTrue(operationOutput_input instanceof FunctionPropertyValue);
        FunctionPropertyValue function = (FunctionPropertyValue) operationOutput_input;
        Assert.assertEquals("get_operation_output", function.getFunction());
        Assert.assertEquals(4, function.getParameters().size());

        Map<String, IValue> attributes = nodeType.getAttributes();

        IValue operationOutputAttr = attributes.get("url");

        // check attributes types
        assertTrue(operationOutputAttr instanceof FunctionPropertyValue);
        function = (FunctionPropertyValue) operationOutputAttr;
        Assert.assertEquals("get_operation_output", function.getFunction());
        Assert.assertEquals(4, function.getParameters().size());
    }

    @Test
    public void parseTopologyTemplateWithGetInputErrors() throws ParsingException, IOException {
        // parse the node define with node_filter
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-topology-template-badinputs.yml"));
        // there are 2 MISSING INPUT errors
        Assert.assertEquals(2, countErrorByLevelAndCode(parsingResult, ParsingErrorLevel.ERROR, ErrorCode.MISSING_TOPOLOGY_INPUT));
        // check 2 errors content
        List<ParsingError> errors = parsingResult.getContext().getParsingErrors();
        for (Iterator iterator = errors.iterator(); iterator.hasNext();) {
            ParsingError parsingError = (ParsingError) iterator.next();
            if (parsingError.getErrorLevel().equals(ParsingErrorLevel.ERROR) && parsingError.getErrorCode().equals(ErrorCode.MISSING_TOPOLOGY_INPUT)) {
                if (parsingError.getProblem().equals("toto")) {
                    Assert.assertEquals("os_distribution", parsingError.getNote());
                }
                if (parsingError.getProblem().equals("greatsize")) {
                    Assert.assertEquals("size", parsingError.getNote());
                }
            }
        }
    }

    @Test
    public void testDataTypes() throws ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-data-types.yml"));
        ParserTestUtil.displayErrors(parsingResult);
        Assert.assertEquals(4, parsingResult.getResult().getDataTypes().size());
        Assert.assertEquals(2, parsingResult.getResult().getNodeTypes().size());
        Assert.assertEquals(0, parsingResult.getContext().getParsingErrors().size());
    }

    @Test
    public void testDataTypesWithError1() throws ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-data-types-error1.yml"));
        ParserTestUtil.displayErrors(parsingResult);
        Assert.assertEquals(3, parsingResult.getContext().getParsingErrors().size());
    }

    @Test
    public void testDataTypesWithError2() throws ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-data-types-error2.yml"));
        ParserTestUtil.displayErrors(parsingResult);
        Assert.assertEquals(3, parsingResult.getContext().getParsingErrors().size());
    }

    @Test
    public void testDataTypesWithError3() throws ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-data-types-error3.yml"));
        ParserTestUtil.displayErrors(parsingResult);
        Assert.assertEquals(3, parsingResult.getContext().getParsingErrors().size());
    }

    @Test
    public void testDataTypesWithError4() throws ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-data-types-error4.yml"));
        ParserTestUtil.displayErrors(parsingResult);
        Assert.assertEquals(3, parsingResult.getContext().getParsingErrors().size());
    }

    private NodeType getMockedCompute() {
        NodeType mockedCompute = new NodeType();
        mockedCompute.setArchiveName("tosca-normative-types");
        mockedCompute.setArchiveVersion("1.2");
        CapabilityDefinition capabilityDefinition = new CapabilityDefinition("host", "tosca.capabilities.Container", Integer.MAX_VALUE);
        mockedCompute.setCapabilities(Lists.newArrayList(capabilityDefinition));
        mockedCompute.setElementId("tosca.nodes.Compute");
        return mockedCompute;
    }

    private NodeType getMockedSoftwareComponent() {
        NodeType mockedSoftware = new NodeType();
        mockedSoftware.setArchiveName("tosca-normative-types");
        mockedSoftware.setArchiveVersion("1.2");
        RequirementDefinition hostRequirement = new RequirementDefinition("host", "tosca.capabilities.Container", null, "", "tosca.relationships.HostedOn",
                "host", 1, Integer.MAX_VALUE, null);
        mockedSoftware.setRequirements(Lists.<RequirementDefinition> newArrayList(hostRequirement));
        mockedSoftware.setElementId("tosca.nodes.SoftwareComponent");
        return mockedSoftware;
    }

    @Test
    public void testParseTopologyReferenceToNormative() throws FileNotFoundException, ParsingException {
        Csar csar = new Csar("tosca-normative-types", "1.2");

        NodeType mockedCompute = getMockedCompute();
        NodeType mockedSoftware = getMockedSoftwareComponent();

        CapabilityType mockedContainer = Mockito.mock(CapabilityType.class);
        Mockito.when(mockedContainer.getElementId()).thenReturn("tosca.capabilities.Container");

        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.SoftwareComponent"),
                Mockito.any(Set.class))).thenReturn(mockedSoftware);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class), Mockito.eq("tosca.capabilities.Container"),
                Mockito.any(Set.class))).thenReturn(mockedContainer);
        Mockito.when(
                csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Compute"), Mockito.any(Set.class)))
                .thenReturn(mockedCompute);
        RelationshipType hostedOn = new RelationshipType();
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(RelationshipType.class), Mockito.eq("tosca.relationships.HostedOn"),
                Mockito.any(Set.class))).thenReturn(hostedOn);

        ParsingResult<ArchiveRoot> parsingResult = parser
                .parseFile(Paths.get(getRootDirectory(), "tosca-topology-template-node-from-derived-type-from-import.yml"));

        Mockito.verify(csarRepositorySearchService).getArchive(csar.getName(), csar.getVersion());

        assertNoBlocker(parsingResult);
    }

    @Test
    public void testDerivedFromNothing() throws ParsingException {
        ParserTestUtil.mockNormativeTypes(csarRepositorySearchService);
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "derived_from_nothing/template.yml"));
        List<ParsingError> errors = parsingResult.getContext().getParsingErrors();
        Assert.assertEquals(5, errors.size());
        assertTrue(errors.stream()
                .allMatch(error -> error.getErrorLevel() == ParsingErrorLevel.WARNING && error.getErrorCode() == ErrorCode.DERIVED_FROM_NOTHING));
        assertTrue(parsingResult.getResult().getNodeTypes().values().stream()
                .allMatch(nodeType -> nodeType.getDerivedFrom() != null && nodeType.getDerivedFrom().contains(NormativeTypesConstant.ROOT_NODE_TYPE)));
        assertTrue(parsingResult.getResult().getDataTypes().values().stream()
                .allMatch(dataType -> dataType.getDerivedFrom() != null && dataType.getDerivedFrom().contains(NormativeTypesConstant.ROOT_DATA_TYPE)));
        assertTrue(parsingResult.getResult().getCapabilityTypes().values().stream().allMatch(
                capabilityType -> capabilityType.getDerivedFrom() != null && capabilityType.getDerivedFrom().contains(NormativeCapabilityTypes.ROOT)));
        assertTrue(parsingResult.getResult().getRelationshipTypes().values().stream().allMatch(relationshipType -> relationshipType.getDerivedFrom() != null
                && relationshipType.getDerivedFrom().contains(NormativeTypesConstant.ROOT_RELATIONSHIP_TYPE)));
        assertTrue(parsingResult.getResult().getArtifactTypes().values().stream().allMatch(
                artifactType -> artifactType.getDerivedFrom() != null && artifactType.getDerivedFrom().contains(NormativeTypesConstant.ROOT_ARTIFACT_TYPE)));
    }

    @Test
    public void testNodeTypeMissingRequirementType() throws ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-node-type-missing-requirement-type.yml"));
        Assert.assertEquals(2, parsingResult.getContext().getParsingErrors().size());
        Assert.assertEquals(ErrorCode.TYPE_NOT_FOUND, parsingResult.getContext().getParsingErrors().get(0).getErrorCode());
        Assert.assertEquals(ErrorCode.TYPE_NOT_FOUND, parsingResult.getContext().getParsingErrors().get(1).getErrorCode());
    }

    @Test
    public void testNodeTypeMissingCapabilityType() throws ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-node-type-missing-capability-type.yml"));
        Assert.assertEquals(1, parsingResult.getContext().getParsingErrors().size());
        Assert.assertEquals(ErrorCode.TYPE_NOT_FOUND, parsingResult.getContext().getParsingErrors().get(0).getErrorCode());
    }

    private void validateHttpArtifact(NodeType httpComponent) {
        ImplementationArtifact httpComponentCreateArtifact = getImplementationArtifact(httpComponent, "create");
        Assert.assertEquals("https://otherCompany/script/short_notation.sh", httpComponentCreateArtifact.getArtifactRef());
        Assert.assertEquals("tosca.artifacts.Implementation.Bash", httpComponentCreateArtifact.getArtifactType());
        assertNull(httpComponentCreateArtifact.getRepositoryCredential());
        assertNull(httpComponentCreateArtifact.getRepositoryName());
        assertNull(httpComponentCreateArtifact.getArtifactRepository());
        assertNull(httpComponentCreateArtifact.getRepositoryURL());

        ImplementationArtifact httpComponentStartArtifact = getImplementationArtifact(httpComponent, "start");
        Assert.assertEquals("myScript.abc", httpComponentStartArtifact.getArtifactRef());
        Assert.assertEquals("tosca.artifacts.Implementation.Bash", httpComponentStartArtifact.getArtifactType());
        Assert.assertEquals(
                ImmutableMap.<String, Object> builder().put(NormativeCredentialConstant.USER_KEY, "good_user")
                        .put(NormativeCredentialConstant.TOKEN_KEY, "real_secured_password").put(NormativeCredentialConstant.TOKEN_TYPE, "password").build(),
                httpComponentStartArtifact.getRepositoryCredential());
        Assert.assertEquals("script_repo", httpComponentStartArtifact.getRepositoryName());
        assertNull(httpComponentStartArtifact.getArtifactRepository());
        Assert.assertEquals("https://myCompany/script", httpComponentStartArtifact.getRepositoryURL());
    }

    @Test
    public void testParseImplementationArtifactWithRepository() throws ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "implementation_artifact.yml"));
        ParserTestUtil.displayErrors(parsingResult);
        assertTrue(parsingResult.getContext().getParsingErrors().isEmpty());
        ArchiveRoot archiveRoot = parsingResult.getResult();
        assertNotNull(archiveRoot.getArchive());
        Assert.assertEquals(getToscaVersion(), archiveRoot.getArchive().getToscaDefinitionsVersion());
        Assert.assertEquals(2, archiveRoot.getArtifactTypes().size());
        Assert.assertEquals(4, archiveRoot.getNodeTypes().size());
        Assert.assertEquals(3, archiveRoot.getRepositories().size());
        Assert.assertEquals(3, archiveRoot.getRelationshipTypes().size());

        NodeType httpComponent = archiveRoot.getNodeTypes().get("my.http.component");
        validateHttpArtifact(httpComponent);

        NodeType httpComponentExtended = archiveRoot.getNodeTypes().get("my.http.component.extended");
        validateHttpArtifact(httpComponentExtended);

        NodeType gitComponent = archiveRoot.getNodeTypes().get("my.git.component");
        ImplementationArtifact gitComponentCreateArtifact = getImplementationArtifact(gitComponent, "create");
        Assert.assertEquals("master:myGitScript.xyz", gitComponentCreateArtifact.getArtifactRef());
        Assert.assertEquals("tosca.artifacts.Implementation.Bash", gitComponentCreateArtifact.getArtifactType());
        assertNull(gitComponentCreateArtifact.getRepositoryCredential());
        Assert.assertEquals("git_repo", gitComponentCreateArtifact.getRepositoryName());
        Assert.assertEquals("git", gitComponentCreateArtifact.getArtifactRepository());
        Assert.assertEquals("https://github.com/myId/myRepo.git", gitComponentCreateArtifact.getRepositoryURL());

        RelationshipType httpRelationship = archiveRoot.getRelationshipTypes().get("my.http.relationship");
        ImplementationArtifact httpRelationshipCreateArtifact = getImplementationArtifact(httpRelationship, "create");
        Assert.assertEquals("https://otherCompany/script/short_notation.sh", httpRelationshipCreateArtifact.getArtifactRef());
        Assert.assertEquals("tosca.artifacts.Implementation.Bash", httpRelationshipCreateArtifact.getArtifactType());
        assertNull(httpRelationshipCreateArtifact.getRepositoryCredential());
        assertNull(httpRelationshipCreateArtifact.getRepositoryName());
        assertNull(httpRelationshipCreateArtifact.getArtifactRepository());
        assertNull(httpRelationshipCreateArtifact.getRepositoryURL());

        ImplementationArtifact httpRelationshipStartArtifact = getImplementationArtifact(httpRelationship, "start");
        Assert.assertEquals("myScript.abc", httpRelationshipStartArtifact.getArtifactRef());
        Assert.assertEquals("tosca.artifacts.Implementation.Bash", httpRelationshipStartArtifact.getArtifactType());
        Assert.assertEquals(
                ImmutableMap.<String, Object> builder().put(NormativeCredentialConstant.USER_KEY, "good_user")
                        .put(NormativeCredentialConstant.TOKEN_KEY, "real_secured_password").put(NormativeCredentialConstant.TOKEN_TYPE, "password").build(),
                httpRelationshipStartArtifact.getRepositoryCredential());
        Assert.assertEquals("script_repo", httpRelationshipStartArtifact.getRepositoryName());
        assertNull(httpRelationshipStartArtifact.getArtifactRepository());
        Assert.assertEquals("https://myCompany/script", httpRelationshipStartArtifact.getRepositoryURL());
    }

    private void validateSimpleWar(DeploymentArtifact artifact) {
        Assert.assertEquals("binary/myWar.war", artifact.getArtifactRef());
        Assert.assertEquals("tosca.artifacts.Deployment.War", artifact.getArtifactType());
        assertNull(artifact.getRepositoryCredential());
        assertNull(artifact.getRepositoryName());
        assertNull(artifact.getArtifactRepository());
        assertNull(artifact.getRepositoryURL());
    }

    private void validateRemoteWar(DeploymentArtifact repositoryArtifact) {
        Assert.assertEquals("prestocloud:prestocloud-ui:1.3.0-SM3", repositoryArtifact.getArtifactRef());
        Assert.assertEquals("tosca.artifacts.Deployment.War", repositoryArtifact.getArtifactType());
        Assert.assertEquals(
                ImmutableMap.<String, Object> builder().put(NormativeCredentialConstant.USER_KEY, "good_user")
                        .put(NormativeCredentialConstant.TOKEN_KEY, "real_secured_password").put(NormativeCredentialConstant.TOKEN_TYPE, "password").build(),
                repositoryArtifact.getRepositoryCredential());
        Assert.assertEquals("maven_repo", repositoryArtifact.getRepositoryName());
        Assert.assertEquals("maven", repositoryArtifact.getArtifactRepository());
        Assert.assertEquals("https://fastconnect.org/maven/content/repositories/fastconnect", repositoryArtifact.getRepositoryURL());
    }

    private void validateMavenDeploymentArtifact(NodeType mavenComponent) {
        DeploymentArtifact artifact = getDeploymentArtifact(mavenComponent, "simple_war");
        validateSimpleWar(artifact);
        DeploymentArtifact repositoryArtifact = getDeploymentArtifact(mavenComponent, "remote_war");
        validateRemoteWar(repositoryArtifact);
    }

    @Test
    public void testParseDeploymentArtifactWithRepository() throws ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "deployment_artifact.yml"));
        ParserTestUtil.displayErrors(parsingResult);
        assertTrue(parsingResult.getContext().getParsingErrors().isEmpty());
        ArchiveRoot archiveRoot = parsingResult.getResult();
        assertNotNull(archiveRoot.getArchive());
        Assert.assertEquals(getToscaVersion(), archiveRoot.getArchive().getToscaDefinitionsVersion());
        Assert.assertEquals(1, archiveRoot.getRepositories().size());
        Assert.assertEquals(2, archiveRoot.getArtifactTypes().size());
        Assert.assertEquals(3, archiveRoot.getNodeTypes().size());
        Assert.assertEquals(3, archiveRoot.getNodeTypes().size());
        Assert.assertEquals(3, archiveRoot.getRelationshipTypes().size());

        NodeType mavenComponent = archiveRoot.getNodeTypes().get("my.maven.component");
        validateMavenDeploymentArtifact(mavenComponent);

        NodeType mavenExtendedComponent = archiveRoot.getNodeTypes().get("my.maven.component.extended");
        validateMavenDeploymentArtifact(mavenExtendedComponent);

        DeploymentArtifact artifact = archiveRoot.getTopology().getInputArtifacts().get("simple_war");
        validateSimpleWar(artifact);

        DeploymentArtifact repositoryArtifact = archiveRoot.getTopology().getInputArtifacts().get("remote_war");
        validateRemoteWar(repositoryArtifact);

        artifact = archiveRoot.getTopology().getNodeTemplates().get("my_node").getArtifacts().get("simple_war");
        Assert.assertEquals("binary/myWar.war", artifact.getArtifactRef());
        Assert.assertEquals("tosca.artifacts.Deployment.War", artifact.getArtifactType());
        assertNull(artifact.getRepositoryCredential());
        assertNull(artifact.getRepositoryName());
        assertNull(artifact.getArtifactRepository());
        assertNull(artifact.getRepositoryURL());

        repositoryArtifact = archiveRoot.getTopology().getNodeTemplates().get("my_node").getArtifacts().get("remote_war");
        Assert.assertEquals("prestocloud:prestocloud-ui:1.3.0-SM3", repositoryArtifact.getArtifactRef());
        Assert.assertEquals("tosca.artifacts.Deployment.War", repositoryArtifact.getArtifactType());
        Assert.assertEquals(
                ImmutableMap.<String, Object> builder().put(NormativeCredentialConstant.USER_KEY, "good_user")
                        .put(NormativeCredentialConstant.TOKEN_KEY, "real_secured_password").put(NormativeCredentialConstant.TOKEN_TYPE, "password").build(),
                repositoryArtifact.getRepositoryCredential());
        Assert.assertEquals("maven_repo", repositoryArtifact.getRepositoryName());
        Assert.assertEquals("maven", repositoryArtifact.getArtifactRepository());
        Assert.assertEquals("https://fastconnect.org/maven/content/repositories/fastconnect", repositoryArtifact.getRepositoryURL());
    }

    private ImplementationArtifact getImplementationArtifact(AbstractInstantiableToscaType component, String operation) {
        return component.getInterfaces().values().iterator().next().getOperations().get(operation).getImplementationArtifact();
    }

    private DeploymentArtifact getDeploymentArtifact(AbstractInstantiableToscaType component, String artifactName) {
        return component.getArtifacts().get(artifactName);
    }

    @Test
    public void testRangeType() throws ParsingException {
        ParserTestUtil.mockNormativeTypes(csarRepositorySearchService);
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "range_type.yml"));
        List<ParsingError> errors = parsingResult.getContext().getParsingErrors();
        Assert.assertEquals(0, errors.size());
    }

    @Test
    public void testRangeTypeConstraint() throws ParsingException {
        ParserTestUtil.mockNormativeTypes(csarRepositorySearchService);
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "range_type_constraint.yml"));
        List<ParsingError> errors = parsingResult.getContext().getParsingErrors();
        Assert.assertEquals(0, errors.size());
    }

    @Test
    public void testRangeTypeConstraintFailMin() throws ParsingException {
        ParserTestUtil.mockNormativeTypes(csarRepositorySearchService);
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "range_type_constraint_fail_min.yml"));
        List<ParsingError> errors = parsingResult.getContext().getParsingErrors();
        Assert.assertEquals(0, errors.size());
    }

    @Test
    public void testRangeTypeConstraintFailMax() throws ParsingException {
        ParserTestUtil.mockNormativeTypes(csarRepositorySearchService);
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "range_type_constraint_fail_max.yml"));
        List<ParsingError> errors = parsingResult.getContext().getParsingErrors();
        Assert.assertEquals(0, errors.size());
    }

    @Test
    public void testServiceRelationshipSubstitution() throws FileNotFoundException, ParsingException {
        Mockito.reset(csarRepositorySearchService);
        Mockito.when(csarRepositorySearchService.getArchive("tosca-normative-types", "1.2")).thenReturn(Mockito.mock(Csar.class));
        NodeType mockRoot = Mockito.mock(NodeType.class);
        Mockito.when(mockRoot.isAbstract()).thenReturn(true);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Root"), Mockito.any(Set.class)))
                .thenReturn(mockRoot);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class), Mockito.eq("tosca.capabilities.Root"),
                Mockito.any(Set.class))).thenReturn(Mockito.mock(CapabilityType.class));
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(RelationshipType.class), Mockito.eq("tosca.relationships.Root"),
                Mockito.any(Set.class))).thenReturn(Mockito.mock(RelationshipType.class));

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "substitution_mapping_service_relationship.yml"));
        assertEquals(0, parsingResult.getContext().getParsingErrors().size());

        ArchiveRoot archiveRoot = parsingResult.getResult();
        Assert.assertNotNull(archiveRoot.getArchive());
        Assert.assertEquals(getToscaVersion(), archiveRoot.getArchive().getToscaDefinitionsVersion());
        Assert.assertEquals("org.prestocloud.relationships.test.MyRelationship",
                archiveRoot.getTopology().getSubstitutionMapping().getCapabilities().get("subst_capa").getServiceRelationshipType());
    }

    @Test
    public void testCapabilitiesComplexProperty() throws ParsingException {
        Mockito.reset(csarRepositorySearchService);

        Csar csar = new Csar("tosca-normative-types", "1.2");
        Mockito.when(csarRepositorySearchService.getArchive(csar.getName(), csar.getVersion())).thenReturn(csar);
        NodeType mockedResult = Mockito.mock(NodeType.class);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Root"), Mockito.any(Set.class)))
                .thenReturn(mockedResult);
        CapabilityType mockedCapabilityResult = Mockito.mock(CapabilityType.class);

        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class), Mockito.eq("tosca.capabilities.Root"),
                Mockito.any(Set.class))).thenReturn(mockedCapabilityResult);

        DataType mockedDataType = new DataType();
        Mockito.when(
                csarRepositorySearchService.getElementInDependencies(Mockito.eq(DataType.class), Mockito.eq("tosca.datatypes.Root"), Mockito.any(Set.class)))
                .thenReturn(mockedDataType);

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "capa_complex_props.yml"));
        assertEquals(0, parsingResult.getContext().getParsingErrors().size());

        ArchiveRoot archiveRoot = parsingResult.getResult();

        // check the capabilityType
        //////////////
        CapabilityType capaType = archiveRoot.getCapabilityTypes().values().stream().findFirst().get();
        assertNotNull(capaType.getProperties());
        Assert.assertEquals(3, capaType.getProperties().size());

        // map property
        String map = "map";
        PropertyDefinition propertyDefinition = capaType.getProperties().get(map);

        assertNotNull(propertyDefinition.getDefault());
        assertTrue(propertyDefinition.getDefault() instanceof ComplexPropertyValue);

        Map<String, Object> propertyMapValue = ((ComplexPropertyValue) propertyDefinition.getDefault()).getValue();
        assertNotNull(propertyMapValue);
        Assert.assertEquals(2, propertyMapValue.size());
        Assert.assertEquals("toto_value", propertyMapValue.get("toto"));
        Assert.assertEquals("tata_value", propertyMapValue.get("tata"));

        // custom property
        String custom = "custom";
        propertyDefinition = capaType.getProperties().get(custom);

        assertEquals("prestocloud.test.datatypes.Custom", propertyDefinition.getType());
        assertNull(propertyDefinition.getDefault());

        // custom_with_default property
        String custom_with_default = "custom_with_default";
        propertyDefinition = capaType.getProperties().get(custom_with_default);
        assertNotNull(propertyDefinition.getDefault());
        assertTrue(propertyDefinition.getDefault() instanceof ComplexPropertyValue);

        propertyMapValue = ((ComplexPropertyValue) propertyDefinition.getDefault()).getValue();
        assertNotNull(propertyMapValue);
        assertEquals(2, propertyMapValue.size());
        assertEquals("defaultName", propertyMapValue.get("name"));

        Object list = propertyMapValue.get("groups");
        assertTrue(list instanceof List);
        assertEquals(2, ((List) list).size());
        assertTrue(CollectionUtils.containsAll((List) list, Lists.newArrayList("prestocloud", "fastconnect")));

        // check the node template capability
        //////////////

        NodeTemplate nodeTemplate = archiveRoot.getTopology().getNodeTemplates().values().stream().findFirst().get();
        Capability capability = nodeTemplate.getCapabilities().values().stream().findFirst().get();
        assertNotNull(capability);
        Assert.assertEquals(3, capability.getProperties().size());

        // map property
        AbstractPropertyValue propertyValue = capability.getProperties().get(map);
        assertNotNull(propertyValue);
        assertTrue(propertyValue instanceof ComplexPropertyValue);

        propertyMapValue = ((ComplexPropertyValue) propertyValue).getValue();
        assertNotNull(propertyMapValue);
        Assert.assertEquals(2, propertyMapValue.size());
        Assert.assertEquals("toto_value", propertyMapValue.get("toto"));
        Assert.assertEquals("tata_value", propertyMapValue.get("tata"));

        // custom property
        propertyValue = capability.getProperties().get(custom);
        assertNotNull(propertyValue);
        assertTrue(propertyValue instanceof ComplexPropertyValue);
        propertyMapValue = ((ComplexPropertyValue) propertyValue).getValue();
        assertNotNull(propertyMapValue);
        assertEquals(2, propertyMapValue.size());
        assertEquals("manual", propertyMapValue.get("name"));

        list = propertyMapValue.get("groups");
        assertTrue(list instanceof List);
        assertEquals(2, ((List) list).size());
        assertTrue(CollectionUtils.containsAll((List) list, Lists.newArrayList("manual_prestocloud", "manual_fastconnect")));

        // custom_with_default property
        propertyValue = capability.getProperties().get(custom_with_default);
        assertNotNull(propertyValue);
        assertTrue(propertyValue instanceof ComplexPropertyValue);

        propertyMapValue = ((ComplexPropertyValue) propertyValue).getValue();
        assertNotNull(propertyMapValue);
        assertEquals(2, propertyMapValue.size());
        assertEquals("defaultName", propertyMapValue.get("name"));

        list = propertyMapValue.get("groups");
        assertTrue(list instanceof List);
        assertEquals(2, ((List) list).size());
        assertTrue(CollectionUtils.containsAll((List) list, Lists.newArrayList("prestocloud", "fastconnect")));

    }

    @Test
    public void testInterfaceInputs() throws ParsingException {
        Mockito.reset(csarRepositorySearchService);
        Mockito.when(csarRepositorySearchService.getArchive("tosca-normative-types", "1.2")).thenReturn(Mockito.mock(Csar.class));
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Root"), Mockito.any(Set.class)))
                .thenReturn(Mockito.mock(NodeType.class));

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "interface-inputs.yml"));
        assertEquals(0, parsingResult.getContext().getParsingErrors().size());
        Map<String, IValue> createInputs = parsingResult.getResult().getNodeTypes().get("org.prestocloud.test.parsing.InterfaceInputsTestNode").getInterfaces()
                .get(ToscaNodeLifecycleConstants.STANDARD).getOperations().get("create").getInputParameters();
        assertNotNull(createInputs);
        assertEquals(2, createInputs.size());
        assertNotNull(createInputs.get("prop_definition"));
        assertTrue(createInputs.get("prop_definition") instanceof PropertyDefinition);
        assertNotNull(createInputs.get("prop_assignment"));
        assertTrue(createInputs.get("prop_assignment") instanceof FunctionPropertyValue);

        Map<String, IValue> startInputs = parsingResult.getResult().getNodeTypes().get("org.prestocloud.test.parsing.InterfaceInputsTestNode").getInterfaces()
                .get(ToscaNodeLifecycleConstants.STANDARD).getOperations().get("start").getInputParameters();
        assertNotNull(startInputs);
        assertEquals(3, startInputs.size());
        assertNotNull(startInputs.get("prop_definition"));
        assertTrue(startInputs.get("prop_definition") instanceof PropertyDefinition);
        assertNotNull(startInputs.get("prop_assignment"));
        assertTrue(startInputs.get("prop_assignment") instanceof PropertyValue);
        assertNotNull(startInputs.get("new_input"));
        assertTrue(startInputs.get("new_input") instanceof PropertyValue);
    }

    @Test
    public void testDuplicateNodeTemplate() throws ParsingException {
        Mockito.reset(csarRepositorySearchService);
        Mockito.when(csarRepositorySearchService.getArchive("tosca-normative-types", "1.2")).thenReturn(Mockito.mock(Csar.class));
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Root"), Mockito.any(Set.class)))
                .thenReturn(Mockito.mock(NodeType.class));

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "topo-duplicate-node-template.yml"));
        assertEquals(0, parsingResult.getContext().getParsingErrors().size());

    }

    @Test
    public void testPolicyTypeParsing() throws ParsingException {
        Mockito.reset(csarRepositorySearchService);
        Mockito.when(csarRepositorySearchService.getArchive("tosca-normative-types", "1.2")).thenReturn(Mockito.mock(Csar.class));
        PolicyType mockRoot = Mockito.mock(PolicyType.class);
        Mockito.when(mockRoot.isAbstract()).thenReturn(true);
        Mockito.when(
                csarRepositorySearchService.getElementInDependencies(Mockito.eq(PolicyType.class), Mockito.eq("tosca.policies.Root"), Mockito.any(Set.class)))
                .thenReturn(mockRoot);

        NodeType mockType = Mockito.mock(NodeType.class);
        Mockito.when(mockType.isAbstract()).thenReturn(true);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Root"), Mockito.any(Set.class)))
                .thenReturn(mockType);
        Mockito.when(
                csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Compute"), Mockito.any(Set.class)))
                .thenReturn(mockType);

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-policy-type.yml"));
        assertEquals(0, parsingResult.getContext().getParsingErrors().size());

        ArchiveRoot archiveRoot = parsingResult.getResult();
        assertNotNull(archiveRoot.getArchive());
        assertEquals(getToscaVersion(), archiveRoot.getArchive().getToscaDefinitionsVersion());
        assertEquals(3, archiveRoot.getPolicyTypes().size());

        PolicyType minPolicyType = archiveRoot.getPolicyTypes().get("org.prestocloud.sample.MinimalPolicyType");
        assertNotNull(minPolicyType);
        assertEquals("org.prestocloud.test.policies.PolicyTypes", minPolicyType.getArchiveName());
        assertEquals("1.0.0-SNAPSHOT", minPolicyType.getArchiveVersion());
        assertEquals("org.prestocloud.sample.MinimalPolicyType", minPolicyType.getElementId());
        assertEquals("This is a sample policy type with minimal definition", minPolicyType.getDescription());
        assertEquals(1, minPolicyType.getDerivedFrom().size());
        assertEquals("tosca.policies.Root", minPolicyType.getDerivedFrom().get(0));

        PolicyType simplePolicyType = archiveRoot.getPolicyTypes().get("org.prestocloud.sample.SimpleConditionPolicyType");
        assertNotNull(simplePolicyType);
        assertEquals("org.prestocloud.test.policies.PolicyTypes", simplePolicyType.getArchiveName());
        assertEquals("1.0.0-SNAPSHOT", simplePolicyType.getArchiveVersion());
        assertEquals("org.prestocloud.sample.SimpleConditionPolicyType", simplePolicyType.getElementId());
        assertEquals("This is a sample policy type with simple definition", simplePolicyType.getDescription());
        assertEquals(1, simplePolicyType.getDerivedFrom().size());
        assertEquals("tosca.policies.Root", simplePolicyType.getDerivedFrom().get(0));
        assertEquals(2, simplePolicyType.getTags().size());
        assertEquals("sample_meta", simplePolicyType.getTags().get(0).getName());
        assertEquals("a meta data", simplePolicyType.getTags().get(0).getValue());
        assertEquals("anoter_meta", simplePolicyType.getTags().get(1).getName());
        assertEquals("another meta data", simplePolicyType.getTags().get(1).getValue());
        assertEquals(1, simplePolicyType.getProperties().size());
        assertNotNull(simplePolicyType.getProperties().get("sample_property"));
        assertEquals("string", simplePolicyType.getProperties().get("sample_property").getType());
        assertEquals(2, simplePolicyType.getTargets().size());
        assertTrue(simplePolicyType.getTargets().contains("tosca.nodes.Compute"));
        assertTrue(simplePolicyType.getTargets().contains("tosca.nodes.Root"));

        PolicyType policyType = archiveRoot.getPolicyTypes().get("org.prestocloud.sample.PolicyType");
        assertNotNull(policyType);
        assertEquals("org.prestocloud.test.policies.PolicyTypes", policyType.getArchiveName());
        assertEquals("1.0.0-SNAPSHOT", policyType.getArchiveVersion());
        assertEquals("org.prestocloud.sample.PolicyType", policyType.getElementId());
        assertEquals("This is a sample policy type", policyType.getDescription());
        assertEquals(1, policyType.getDerivedFrom().size());
        assertEquals("tosca.policies.Root", policyType.getDerivedFrom().get(0));
        assertEquals(2, policyType.getTags().size());
        assertEquals("sample_meta", policyType.getTags().get(0).getName());
        assertEquals("a meta data", policyType.getTags().get(0).getValue());
        assertEquals("anoter_meta", policyType.getTags().get(1).getName());
        assertEquals("another meta data", policyType.getTags().get(1).getValue());
        assertEquals(1, policyType.getProperties().size());
        assertNotNull(policyType.getProperties().get("sample_property"));
        assertEquals("string", policyType.getProperties().get("sample_property").getType());
        assertEquals(2, policyType.getTargets().size());
        assertTrue(simplePolicyType.getTargets().contains("tosca.nodes.Compute"));
        assertTrue(simplePolicyType.getTargets().contains("tosca.nodes.Root"));
    }

    @Test
    public void policyParsingWithUnknownTargetTypeShouldFail() throws ParsingException {
        Mockito.reset(csarRepositorySearchService);
        Mockito.when(csarRepositorySearchService.getArchive("tosca-normative-types", "1.2")).thenReturn(Mockito.mock(Csar.class));
        PolicyType mockRoot = Mockito.mock(PolicyType.class);
        Mockito.when(mockRoot.isAbstract()).thenReturn(true);
        Mockito.when(
                csarRepositorySearchService.getElementInDependencies(Mockito.eq(PolicyType.class), Mockito.eq("tosca.policies.Root"), Mockito.any(Set.class)))
                .thenReturn(mockRoot);

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-policy-type.yml"));
        assertEquals(4, parsingResult.getContext().getParsingErrors().size());
    }

    @Test
    public void testPolicyTemplateParsing() throws ParsingException {
        Mockito.reset(csarRepositorySearchService);
        Mockito.when(csarRepositorySearchService.getArchive("tosca-normative-types", "1.2")).thenReturn(Mockito.mock(Csar.class));

        NodeType nodeType = new NodeType();
        nodeType.setElementId("tosca.nodes.Root");
        nodeType.setArchiveName("tosca-normative-types");
        nodeType.setArchiveVersion("1.2");
        nodeType.setDerivedFrom(Lists.newArrayList("tosca.nodes.Root"));
        Mockito.when(
                csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Compute"), Mockito.any(Set.class)))
                .thenReturn(nodeType);

        nodeType = new NodeType();
        nodeType.setElementId("tosca.nodes.Root");
        nodeType.setArchiveName("tosca-normative-types");
        nodeType.setArchiveVersion("1.2");
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Root"), Mockito.any(Set.class)))
                .thenReturn(nodeType);

        PolicyType policyType = Mockito.mock(PolicyType.class);
        policyType.setElementId("tosca.nodes.Root");
        policyType.setArchiveName("tosca-normative-types");
        policyType.setArchiveVersion("1.2");
        Mockito.when(policyType.isAbstract()).thenReturn(true);
        Mockito.when(
                csarRepositorySearchService.getElementInDependencies(Mockito.eq(PolicyType.class), Mockito.eq("tosca.policies.Root"), Mockito.any(Set.class)))
                .thenReturn(policyType);

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-policy-template.yml"));
        assertEquals(0, parsingResult.getContext().getParsingErrors().size());

        ArchiveRoot archiveRoot = parsingResult.getResult();
        assertNotNull(archiveRoot.getArchive());
        assertEquals(getToscaVersion(), archiveRoot.getArchive().getToscaDefinitionsVersion());
        assertEquals(1, archiveRoot.getPolicyTypes().size());

        PolicyType simplePolicyType = archiveRoot.getPolicyTypes().get("org.prestocloud.sample.SamplePolicy");
        assertNotNull(simplePolicyType);
        assertEquals("org.prestocloud.sample.SamplePolicy", simplePolicyType.getElementId());
        assertEquals("This is a sample policy type with simple definition", simplePolicyType.getDescription());
        assertEquals(1, simplePolicyType.getDerivedFrom().size());
        assertEquals("tosca.policies.Root", simplePolicyType.getDerivedFrom().get(0));
        assertEquals(2, simplePolicyType.getTags().size());
        assertEquals("sample_meta", simplePolicyType.getTags().get(0).getName());
        assertEquals("a meta data", simplePolicyType.getTags().get(0).getValue());
        assertEquals("anoter_meta", simplePolicyType.getTags().get(1).getName());
        assertEquals("another meta data", simplePolicyType.getTags().get(1).getValue());
        assertEquals(1, simplePolicyType.getProperties().size());
        assertNotNull(simplePolicyType.getProperties().get("sample_property"));
        assertEquals("string", simplePolicyType.getProperties().get("sample_property").getType());
        assertEquals(2, simplePolicyType.getTargets().size());
        assertTrue(simplePolicyType.getTargets().contains("tosca.nodes.Compute"));
        assertTrue(simplePolicyType.getTargets().contains("tosca.nodes.Root"));

        // Test that the template is correctly parsed
        assertNotNull(archiveRoot.getTopology());
        assertEquals(1, archiveRoot.getTopology().getPolicies().size());
        assertNotNull(archiveRoot.getTopology().getPolicies().get("anti_affinity_policy"));
        assertEquals("org.prestocloud.sample.SamplePolicy", archiveRoot.getTopology().getPolicies().get("anti_affinity_policy").getType());
        assertEquals(1, archiveRoot.getTopology().getPolicies().get("anti_affinity_policy").getProperties().size());
        assertNotNull(archiveRoot.getTopology().getPolicies().get("anti_affinity_policy").getProperties().get("sample_property"));
        assertEquals(2, archiveRoot.getTopology().getPolicies().get("anti_affinity_policy").getTags().size());
    }

    @Test
    public void policyTemplateParsingWithUnknownTypesShouldFail() throws ParsingException {
        Mockito.reset(csarRepositorySearchService);
        Mockito.when(csarRepositorySearchService.getArchive("tosca-normative-types", "1.2")).thenReturn(Mockito.mock(Csar.class));

        NodeType nodeType = new NodeType();
        nodeType.setElementId("tosca.nodes.Compute");
        nodeType.setArchiveName("tosca-normative-types");
        nodeType.setArchiveVersion("1.2");
        nodeType.setDerivedFrom(Lists.newArrayList("tosca.nodes.Root"));
        Mockito.when(
                csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Compute"), Mockito.any(Set.class)))
                .thenReturn(nodeType);

        nodeType = new NodeType();
        nodeType.setElementId("tosca.nodes.Root");
        nodeType.setArchiveName("tosca-normative-types");
        nodeType.setArchiveVersion("1.2");
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Root"), Mockito.any(Set.class)))
                .thenReturn(nodeType);

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-policy-template-fail.yml"));
        assertEquals(1, parsingResult.getContext().getParsingErrors().size());
        assertEquals(1, countErrorByLevelAndCode(parsingResult, ParsingErrorLevel.ERROR, ErrorCode.POLICY_TARGET_NOT_FOUND));
    }

    @Test
    public void policyTemplateParsingWithUnknownTargetShouldFail() throws ParsingException {
        Mockito.reset(csarRepositorySearchService);
        Mockito.when(csarRepositorySearchService.getArchive("tosca-normative-types", "1.2")).thenReturn(Mockito.mock(Csar.class));

        NodeType nodeType = new NodeType();
        nodeType.setElementId("tosca.nodes.Compute");
        nodeType.setArchiveName("tosca-normative-types");
        nodeType.setArchiveVersion("1.2");
        nodeType.setDerivedFrom(Lists.newArrayList("tosca.nodes.Root"));
        Mockito.when(
                csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Compute"), Mockito.any(Set.class)))
                .thenReturn(nodeType);

        nodeType = new NodeType();
        nodeType.setElementId("tosca.nodes.Root");
        nodeType.setArchiveName("tosca-normative-types");
        nodeType.setArchiveVersion("1.2");
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Root"), Mockito.any(Set.class)))
                .thenReturn(nodeType);

        PolicyType policyType = new PolicyType();
        policyType.setAbstract(true);
        policyType.setElementId("org.prestocloud.sample.SamplePolicy");
        policyType.setArchiveName("org.prestocloud.test.policies.PolicyTemplate");
        policyType.setArchiveVersion("1.0.0-SNAPSHOT");
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(PolicyType.class), Mockito.eq("org.prestocloud.sample.SamplePolicy"),
                Mockito.any(Set.class))).thenReturn(policyType);

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-policy-template-fail.yml"));
        assertEquals(1, parsingResult.getContext().getParsingErrors().size());
        assertEquals(1, countErrorByLevelAndCode(parsingResult, ParsingErrorLevel.ERROR, ErrorCode.POLICY_TARGET_NOT_FOUND));
    }
}