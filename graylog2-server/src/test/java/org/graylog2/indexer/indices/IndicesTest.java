/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.indexer.indices;

import com.google.common.eventbus.EventBus;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.indexer.IgnoreIndexTemplate;
import org.graylog2.indexer.IndexMappingFactory;
import org.graylog2.indexer.IndexMappingTemplate;
import org.graylog2.indexer.IndexTemplateNotFoundException;
import org.graylog2.indexer.TestIndexSet;
import org.graylog2.indexer.indexset.CustomFieldMapping;
import org.graylog2.indexer.indexset.CustomFieldMappings;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetMappingTemplate;
import org.graylog2.indexer.indexset.profile.IndexFieldTypeProfile;
import org.graylog2.indexer.indexset.profile.IndexFieldTypeProfileService;
import org.graylog2.indexer.indices.blocks.IndicesBlockStatus;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategy;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategyConfig;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategy;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategyConfig;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.plugin.system.SimpleNodeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndicesTest {

    private Indices underTest;

    @Mock
    private IndexMappingFactory indexMappingFactory;

    private final NodeId nodeId = new SimpleNodeId("5ca1ab1e-0000-4000-a000-000000000000");

    @Mock
    private AuditEventSender auditEventSender;

    @Mock
    private EventBus eventBus;

    @Mock
    private IndicesAdapter indicesAdapter;
    @Mock
    private IndexFieldTypeProfileService profileService;

    @BeforeEach
    public void setup() {
        underTest = new Indices(
                indexMappingFactory,
                nodeId,
                auditEventSender,
                eventBus,
                indicesAdapter,
                profileService
        );
    }

    @Test
    public void ensureIndexTemplate_IfIndexTemplateExistsOnIgnoreIndexTemplate_thenNoExceptionThrown() {
        when(indexMappingFactory.createIndexMapping(any()))
                .thenThrow(new IgnoreIndexTemplate(true,
                        "Reasom", "test", "test-template", null));

        when(indicesAdapter.indexTemplateExists("test-template")).thenReturn(true);

        assertThatCode(() -> underTest.ensureIndexTemplate(
                indexSetConfig("test", "test-template", "custom")))
                .doesNotThrowAnyException();
    }

    @Test
    public void ensureIndexTemplate_IfIndexTemplateDoesntExistOnIgnoreIndexTemplateAndFailOnMissingTemplateIsTrue_thenExceptionThrown() {
        when(indexMappingFactory.createIndexMapping(any()))
                .thenThrow(new IgnoreIndexTemplate(true,
                        "Reasom", "test", "test-template", null));

        when(indicesAdapter.indexTemplateExists("test-template")).thenReturn(false);

        assertThatCode(() -> underTest.ensureIndexTemplate(indexSetConfig("test",
                "test-template", "custom")))
                .isExactlyInstanceOf(IndexTemplateNotFoundException.class)
                .hasMessage("No index template with name 'test-template' (type - 'custom') found in Elasticsearch");
    }

    @Test
    public void ensureIndexTemplate_IfIndexTemplateDoesntExistOnIgnoreIndexTemplateAndFailOnMissingTemplateIsFalse_thenNoExceptionThrown() {
        when(indexMappingFactory.createIndexMapping(any()))
                .thenThrow(new IgnoreIndexTemplate(false,
                        "Reasom", "test", "test-template", null));

        assertThatCode(() -> underTest.ensureIndexTemplate(indexSetConfig("test",
                "test-template", "custom")))
                .doesNotThrowAnyException();
    }

    @Test
    public void testGetIndicesBlocksStatusReturnsNoBlocksOnNullIndicesList() {
        final IndicesBlockStatus indicesBlocksStatus = underTest.getIndicesBlocksStatus(null);
        assertNotNull(indicesBlocksStatus);
        assertEquals(0, indicesBlocksStatus.countBlockedIndices());
    }

    @Test
    public void testGetIndicesBlocksStatusReturnsNoBlocksOnEmptyIndicesList() {
        final IndicesBlockStatus indicesBlocksStatus = underTest.getIndicesBlocksStatus(Collections.emptyList());
        assertNotNull(indicesBlocksStatus);
        assertEquals(0, indicesBlocksStatus.countBlockedIndices());
    }

    @Test
    void testUsesCustomMappingsAndProfileWhileGettingTemplate() {
        final TestIndexSet testIndexSet = indexSetConfig("test",
                "test-template-profiles",
                "custom",
                "000000000000000000000013",
                new CustomFieldMappings(List.of(
                        new CustomFieldMapping("f1", "string"),
                        new CustomFieldMapping("f2", "long")
                )));
        doReturn(Optional.of(new IndexFieldTypeProfile(
                "000000000000000000000013",
                "test_profile",
                "Test profile",
                new CustomFieldMappings(List.of(
                        new CustomFieldMapping("f1", "ip"),
                        new CustomFieldMapping("f3", "ip")
                )))
        )).when(profileService).get("000000000000000000000013");
        IndexMappingTemplate indexMappingTemplateMock = mock(IndexMappingTemplate.class);
        doReturn(indexMappingTemplateMock).when(indexMappingFactory).createIndexMapping(testIndexSet.getConfig());
        underTest.getIndexTemplate(testIndexSet);

        verify(indexMappingTemplateMock).toTemplate(
                new IndexSetMappingTemplate("standard", "test_*",
                        new CustomFieldMappings(List.of(
                                new CustomFieldMapping("f1", "string"), //from individual custom mapping
                                new CustomFieldMapping("f2", "long"), //from individual custom mapping
                                new CustomFieldMapping("f3", "ip") //from profile
                        )))
        );
    }

    @Test
    void testUsesCustomMappingsWhileGettingTemplateWhenProfileIsNull() {
        final CustomFieldMappings individualCustomFieldMappings = new CustomFieldMappings(List.of(
                new CustomFieldMapping("f1", "string"),
                new CustomFieldMapping("f2", "long")
        ));
        final TestIndexSet testIndexSet = indexSetConfig("test",
                "test-template-profiles",
                "custom",
                "000000000000000000000013",
                individualCustomFieldMappings);
        doReturn(Optional.of(new IndexFieldTypeProfile(
                "000000000000000000000013",
                "empty_test_profile",
                "Empty test profile",
                new CustomFieldMappings(List.of()))
        )).when(profileService).get("000000000000000000000013");
        IndexMappingTemplate indexMappingTemplateMock = mock(IndexMappingTemplate.class);
        doReturn(indexMappingTemplateMock).when(indexMappingFactory).createIndexMapping(testIndexSet.getConfig());
        underTest.getIndexTemplate(testIndexSet);

        verify(indexMappingTemplateMock).toTemplate(
                new IndexSetMappingTemplate("standard", "test_*",
                        individualCustomFieldMappings)
        );
    }

    @Test
    void testUsesCustomMappingsWhileGettingTemplateWhenProfileHasNoOwnMappings() {
        final CustomFieldMappings individualCustomFieldMappings = new CustomFieldMappings(List.of(
                new CustomFieldMapping("f1", "string"),
                new CustomFieldMapping("f2", "long")
        ));
        final TestIndexSet testIndexSet = indexSetConfig("test",
                "test-template-profiles",
                "custom",
                "000000000000000000000013",
                individualCustomFieldMappings);
        IndexMappingTemplate indexMappingTemplateMock = mock(IndexMappingTemplate.class);
        doReturn(indexMappingTemplateMock).when(indexMappingFactory).createIndexMapping(testIndexSet.getConfig());
        underTest.getIndexTemplate(testIndexSet);

        verify(indexMappingTemplateMock).toTemplate(
                new IndexSetMappingTemplate("standard", "test_*",
                        individualCustomFieldMappings)
        );
    }

    @Test
    void testUsesCustomMappingsAndProfileWhileBuildingTemplate() {
        final TestIndexSet testIndexSet = indexSetConfig("test",
                "test-template-profiles",
                "custom",
                "000000000000000000000013",
                new CustomFieldMappings(List.of(
                        new CustomFieldMapping("f1", "string"),
                        new CustomFieldMapping("f2", "long")
                )));
        doReturn(Optional.of(new IndexFieldTypeProfile(
                "000000000000000000000013",
                "test_profile",
                "Test profile",
                new CustomFieldMappings(List.of(
                        new CustomFieldMapping("f1", "ip"),
                        new CustomFieldMapping("f3", "ip")
                )))
        )).when(profileService).get("000000000000000000000013");
        IndexMappingTemplate indexMappingTemplateMock = mock(IndexMappingTemplate.class);
        doReturn(indexMappingTemplateMock).when(indexMappingFactory).createIndexMapping(testIndexSet.getConfig());
        underTest.buildTemplate(testIndexSet, testIndexSet.getConfig());

        verify(indexMappingTemplateMock).toTemplate(
                new IndexSetMappingTemplate("standard", "test_*",
                        new CustomFieldMappings(List.of(
                                new CustomFieldMapping("f1", "string"), //from individual custom mapping
                                new CustomFieldMapping("f2", "long"), //from individual custom mapping
                                new CustomFieldMapping("f3", "ip") //from profile
                        ))),
                0L
        );
    }

    private TestIndexSet indexSetConfig(final String indexPrefix,
                                        final String indexTemplaNameName,
                                        final String indexTemplateType,
                                        final String profileId,
                                        final CustomFieldMappings customFieldMappings) {
        return new TestIndexSet(IndexSetConfig.builder()
                .id("index-set-1")
                .title("Index set 1")
                .description("For testing")
                .indexPrefix(indexPrefix)
                .creationDate(ZonedDateTime.now())
                .shards(1)
                .replicas(0)
                .rotationStrategyClass(MessageCountRotationStrategy.class.getCanonicalName())
                .rotationStrategyConfig(MessageCountRotationStrategyConfig.createDefault())
                .retentionStrategyClass(DeletionRetentionStrategy.class.getCanonicalName())
                .retentionStrategyConfig(DeletionRetentionStrategyConfig.createDefault())
                .indexAnalyzer("standard")
                .indexTemplateName(indexTemplaNameName)
                .indexTemplateType(indexTemplateType)
                .indexOptimizationMaxNumSegments(1)
                .indexOptimizationDisabled(false)
                .fieldTypeProfile(profileId)
                .customFieldMappings(customFieldMappings)
                .build());
    }

    private TestIndexSet indexSetConfig(String indexPrefix, String indexTemplaNameName, String indexTemplateType) {
        return indexSetConfig(indexPrefix, indexTemplaNameName, indexTemplateType, null, new CustomFieldMappings(List.of()));
    }
}
