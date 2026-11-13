/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.webapi.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Date;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.audit.AuditOperationType;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataapproval.DataApprovalAuditService;
import org.hisp.dhis.datavalue.DataValueChangelogService;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.fieldfilter.FieldFilterParams;
import org.hisp.dhis.fieldfilter.FieldFilterService;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.node.Node;
import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.trackedentity.TrackedEntityAuditQueryParams;
import org.hisp.dhis.tracker.audit.TrackedEntityAudit;
import org.hisp.dhis.tracker.audit.TrackedEntityAuditService;
import org.hisp.dhis.webapi.service.ContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AuditControllerPagingTest {
  private TrackedEntityAuditService audits;
  private FieldFilterService fields;
  private AuditController controller;

  @BeforeEach
  void setUp() {
    audits = mock(TrackedEntityAuditService.class);
    fields = mock(FieldFilterService.class);
    controller =
        new AuditController(
            mock(IdentifiableObjectManager.class),
            mock(DataValueChangelogService.class),
            mock(DataApprovalAuditService.class),
            audits,
            fields,
            mock(ContextService.class),
            mock(FileResourceService.class),
            mock(DhisConfigurationProvider.class));
    when(audits.getTrackedEntityAuditsCount(any())).thenReturn(95);
    when(audits.getTrackedEntityAudits(any())).thenReturn(List.of());
    when(fields.toCollectionNode(eq(TrackedEntityAudit.class), any(FieldFilterParams.class)))
        .thenReturn(new CollectionNode("trackedEntityAudits", true));
  }

  @Test
  void defaultRequestBoundsTheQueryAndIncludesPager() {
    RootNode response = query(null, null, 1, 50);
    var params = queryParams();
    assertNotNull(params.getPager());
    assertEquals(0, params.getPager().getOffset());
    assertEquals(50, params.getPager().getPageSize());
    assertTrue(hasPager(response));
  }

  @Test
  void requestedPageAndSizeReachTheAuditService() {
    RootNode response = query(null, true, 3, 10);
    var params = queryParams();
    assertNotNull(params.getPager());
    assertEquals(20, params.getPager().getOffset());
    assertEquals(10, params.getPager().getPageSize());
    assertEquals(95, params.getPager().getTotal());
    assertTrue(hasPager(response));
  }

  @Test
  void disablingPagingOmitsPagerAndCountQuery() {
    RootNode response = query(null, false, 1, 50);
    assertNull(queryParams().getPager());
    assertFalse(hasPager(response));
    verify(audits, never()).getTrackedEntityAuditsCount(any());
  }

  @Test
  void skipPagingTrueOverridesPagingTrue() {
    RootNode response = query(true, true, 2, 10);
    assertNull(queryParams().getPager());
    assertFalse(hasPager(response));
    verify(audits, never()).getTrackedEntityAuditsCount(any());
  }

  @Test
  void skipPagingFalseOverridesPagingFalse() {
    RootNode response = query(false, false, 2, 10);
    var params = queryParams();
    assertNotNull(params.getPager());
    assertEquals(10, params.getPager().getOffset());
    assertTrue(hasPager(response));
  }

  @Test
  void pagedReviewPreservesSelectionAndFieldFilters() {
    Date from = new Date(1_000);
    Date to = new Date(2_000);
    controller.getTrackedEntityAudit(
        "created",
        Set.of(UID.of("Entity12345")),
        List.of("reviewer"),
        List.of(AuditOperationType.READ),
        from,
        to,
        null,
        true,
        10,
        1);
    var params = queryParams();
    assertEquals(List.of("Entity12345"), params.getTrackedEntities());
    assertEquals(List.of("reviewer"), params.getUsers());
    assertEquals(List.of(AuditOperationType.READ), params.getAuditTypes());
    assertEquals(from, params.getStartDate());
    assertEquals(to, params.getEndDate());
    verify(audits).getTrackedEntityAuditsCount(params);
    var fieldParams = ArgumentCaptor.forClass(FieldFilterParams.class);
    verify(fields).toCollectionNode(eq(TrackedEntityAudit.class), fieldParams.capture());
    assertEquals("created", fieldParams.getValue().getFields());
  }

  @Test
  void omittedOptionalEntityFilterDoesNotFailAuditReview() {
    RootNode response =
        controller.getTrackedEntityAudit(null, null, null, null, null, null, null, true, 50, 1);
    assertTrue(queryParams().getTrackedEntities().isEmpty());
    assertTrue(hasPager(response));
  }

  private RootNode query(Boolean skipPaging, Boolean paging, int page, int pageSize) {
    return controller.getTrackedEntityAudit(
        null, Set.of(), null, null, null, null, skipPaging, paging, pageSize, page);
  }

  private TrackedEntityAuditQueryParams queryParams() {
    var params = ArgumentCaptor.forClass(TrackedEntityAuditQueryParams.class);
    verify(audits).getTrackedEntityAudits(params.capture());
    return params.getValue();
  }

  private boolean hasPager(RootNode response) {
    return response.getChildren().stream().map(Node::getName).anyMatch("pager"::equals);
  }
}
