package com.remotejob.planservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.remotejob.planservice.dto.PlanDto;
import com.remotejob.planservice.utils.TestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 * Updated tests aligned with the migrated Plan API (DTO-based, secured endpoints).
 */
class PlanServiceAPIApplicationTests extends TestUtils {

    @Test
    void shouldCreatePlan() throws Exception {
        String jwt = this.registerUserAndGetJWT();
        String body = convertToJson(buildTestPlanDto(null));

        MvcResult responseCreated = performPostRequest(body, "/api/v1/plan", jwt)
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        PlanDto created = getDataResponse(responseCreated, PlanDto.class);
        assert created != null;
        assert created.id != null;
        assert created.userId != null;
        assert created.invoiceId != null;
    }

    @Test
    void shouldGetByIdPublic() throws Exception {
        String jwt = this.registerUserAndGetJWT();
        PlanDto newPlan = buildTestPlanDto(null);
        String body = convertToJson(newPlan);

        MvcResult createdRes = performPostRequest(body, "/api/v1/plan", jwt)
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        PlanDto created = getDataResponse(createdRes, PlanDto.class);
        String url = "/api/v1/plan/" + created.id;
        MvcResult getRes = performGetRequest(url, "")
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        PlanDto got = getDataResponse(getRes, PlanDto.class);
        assert got != null;
        assert created.id.equals(got.id);
    }

    @Test
    void shouldGetByUserIdAuthenticated() throws Exception {
        String jwt = this.registerUserAndGetJWT();
        PlanDto newPlan = buildTestPlanDto(null);
        String body = convertToJson(newPlan);

        performPostRequest(body, "/api/v1/plan", jwt)
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk());

        MvcResult listRes = performGetRequest("/api/v1/plan/user/" + newPlan.userId, jwt)
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        List<PlanDto> plans = getDataListResponse(listRes, PlanDto.class);
        assert plans != null && !plans.isEmpty();
        assert plans.stream().anyMatch(p -> newPlan.invoiceId.equals(p.invoiceId));
    }

    @Test
    void shouldUpdatePlan() throws Exception {
        String jwt = this.registerUserAndGetJWT();
        PlanDto newPlan = buildTestPlanDto(null);
        String body = convertToJson(newPlan);

        MvcResult createdRes = performPostRequest(body, "/api/v1/plan", jwt)
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();
        PlanDto created = getDataResponse(createdRes, PlanDto.class);

        // Update a couple of fields
        created.description = "Updated description";
        created.isActive = Boolean.FALSE;
        String updatedBody = convertToJson(created);

        MvcResult updatedRes = performPutRequest(updatedBody, "/api/v1/plan", jwt)
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        PlanDto updated = getDataResponse(updatedRes, PlanDto.class);
        assert updated != null;
        assert "Updated description".equals(updated.description);
        assert Boolean.FALSE.equals(updated.isActive);
    }

    @Test
    void shouldDeletePlan() throws Exception {
        String jwt = this.registerUserAndGetJWT();
        PlanDto newPlan = buildTestPlanDto(null);
        String body = convertToJson(newPlan);

        MvcResult createdRes = performPostRequest(body, "/api/v1/plan", jwt)
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();
        PlanDto created = getDataResponse(createdRes, PlanDto.class);

        MvcResult deleteRes = performDeleteRequest("", "/api/v1/plan/" + created.id, jwt)
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        String message = getMessageResponse(deleteRes, PlanDto.class);
        assert "Plan deleted successfully".equals(message);
    }

    @Test
    void shouldPatchPlanPartialUpdate() throws Exception {
        String jwt = this.registerUserAndGetJWT();
        // Create initial plan
        PlanDto newPlan = buildTestPlanDto(null);
        String body = convertToJson(newPlan);

        MvcResult createdRes = performPostRequest(body, "/api/v1/plan", jwt)
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();
        PlanDto created = getDataResponse(createdRes, PlanDto.class);

        // Build a PATCH payload to modify only description and isActive
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode patch = mapper.createObjectNode();
        patch.put("description", "Patched description");
        patch.put("isActive", false);
        String patchBody = mapper.writeValueAsString(patch);

        // Execute PATCH
        MvcResult patchedRes = performPatchRequest(patchBody, "/api/v1/plan/" + created.id, jwt)
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        PlanDto patched = getDataResponse(patchedRes, PlanDto.class);
        assert patched != null;
        assert created.id.equals(patched.id);
        assert "Patched description".equals(patched.description);
        assert Boolean.FALSE.equals(patched.isActive);
        // Ensure a field we didn't set remains unchanged (durationInDays)
        assert created.durationInDays.equals(patched.durationInDays);
        // And invoiceId unchanged
        assert created.invoiceId.equals(patched.invoiceId);
    }

    private PlanDto buildTestPlanDto(UUID id) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode items = mapper.createObjectNode();
        items.put("planName", "basic");
        items.put("seats", 1);
        items.put("features", "none");

        PlanDto dto = new PlanDto();
        dto.id = id;
        dto.userId = "user-" + generateRandomString(6);
        dto.invoiceId = UUID.randomUUID();
        dto.description = "Test plan";
        dto.isActive = Boolean.TRUE;
        dto.items = items;
        dto.status = "CREATED";
        dto.durationInDays = 30;
        dto.expiresAt = Instant.now().plusSeconds(30L * 24 * 3600);
        return dto;
    }
}