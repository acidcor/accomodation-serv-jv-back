package mate.academy.accommodationbooking.service.accommodation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import mate.academy.accommodationbooking.service.dto.accomnodation.AccommodationResponseDto;
import mate.academy.accommodationbooking.service.dto.accomnodation.AccommodationShortResponseDto;
import mate.academy.accommodationbooking.service.dto.accomnodation.address.AddressRequestDto;
import mate.academy.accommodationbooking.service.dto.accomnodation.amenity.AmenityRequestDto;
import mate.academy.accommodationbooking.service.dto.accomnodation.crud.AccommodationPatchRequestDto;
import mate.academy.accommodationbooking.service.dto.accomnodation.crud.AccommodationRequestDto;
import mate.academy.accommodationbooking.service.dto.accomnodation.crud.AccommodationUpdateRequestDto;
import mate.academy.accommodationbooking.service.model.Accommodation;
import mate.academy.accommodationbooking.service.model.Amenity;
import mate.academy.accommodationbooking.service.repository.AccommodationRepository;
import mate.academy.accommodationbooking.service.repository.AmenityRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccommodationControllerTest {
    private static final Accommodation.Type ACCOMMODATION_TYPE =
            Accommodation.Type.CHALET;

    private static final String ACCOMMODATION_SIZE = "120";
    private static final String UPDATED_SIZE = "121";
    private static final Long AMENITY_ID = 1L;
    private static final Long DEFAULT_ID = 1L;
    private static final Long NONEXISTED_ID= 999999L;
    private static final BigDecimal DAILY_RATE = new BigDecimal("150.00");
    private static final BigDecimal UPDATED_DAILY_RATE = new BigDecimal("250.00");
    private static final Integer AVAILABILITY = 5;

    private static final String STREET_LINE_ONE = "Main Street 10";
    private static final String STREET_LINE_TWO = "Building A";
    private static final String POSTAL_CODE = "00-001";
    private static final String CITY = "Warsaw";
    private static final String REGION = "Masovian";
    private static final String ROOM_NUMBER = "101";
    private static final double LATITUDE = 52.2297;
    private static final double LONGITUDE = 21.0122;

    protected static MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AmenityRepository amenityRepository;

    @Autowired
    private AccommodationRepository accommodationRepository;

    @BeforeAll
    static void beforeAll(
            @Autowired WebApplicationContext applicationContext
    ) {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();
    }

    @Sql(scripts = {
            "classpath:database/delete-all.sql",
            "classpath:database/accommodation/add-amenity.sql"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @WithMockUser(username = "admin", authorities = {"ADMIN"})
    @Test
    @DisplayName("Create category - valid request, returns response DTO")
    void createAccommodation_ValidRequest_Success() throws Exception {
        // Given
        AccommodationRequestDto expected = getAccommodationRequest();
        String jsonRequest = objectMapper.writeValueAsString(expected);

        Amenity expectedAmenity = amenityRepository
                .findById(expected.getAmenities().getFirst())
                .get();

        // When
        MvcResult result = mockMvc.perform(
                        post("/accommodations")
                                .content(jsonRequest)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isCreated())
                .andReturn();

        // Then
        AccommodationResponseDto actual = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                AccommodationResponseDto.class
        );

        assertNotNull(actual.amenities());
        assertEquals(1, actual.amenities().size());

        assertEquals(expectedAmenity.getId(),
                actual.amenities().getFirst().id());
        assertEquals(expectedAmenity.getName(),
                actual.amenities().getFirst().name());
        assertEquals(expectedAmenity.getDescription(),
                actual.amenities().getFirst().description());

        assertEquals(expected.getLocation().getStreetLineOne(),
                actual.location().streetLineOne());
        assertEquals(expected.getLocation().getStreetLineTwo(),
                actual.location().streetLineTwo());
        assertEquals(expected.getLocation().getPostalCode(),
                actual.location().postalCode());
        assertEquals(expected.getLocation().getCity(),
                actual.location().city());
        assertEquals(expected.getLocation().getRegion(),
                actual.location().region());
        assertEquals(expected.getLocation().getRoomNumber(),
                actual.location().roomNumber());
        assertEquals(expected.getLocation().getLatitude(),
                actual.location().latitude());
        assertEquals(expected.getLocation().getLongitude(),
                actual.location().longitude());

        assertEquals(expected.getDailyRate(), actual.dailyRate());
        assertEquals(expected.getType(), actual.type());
        assertEquals(expected.getSize(), actual.size());
        assertEquals(expected.getAvailability(), actual.availability());
    }

    @Sql(
            scripts = {
                    "classpath:database/delete-all.sql",
                    "classpath:database/accommodation/add-amenity.sql",
                    "classpath:database/accommodation/add-address.sql",
                    "classpath:database/accommodation/add-accommodation.sql"
            },
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @WithMockUser(username = "admin", authorities = {"ADMIN"})
    @Test
    @DisplayName("Find all accommodations - returns paginated accommodations")
    void findAll_ValidRequest_ReturnsPage() throws Exception {
        // When
        MvcResult result = mockMvc.perform(
                        get("/accommodations")
                                .param("page", "0")
                                .param("size", "10")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.number").value(0))
                .andReturn();

        // Then
        JsonNode response = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );

        AccommodationShortResponseDto actual = objectMapper.treeToValue(
                response.get("content").get(0),
                AccommodationShortResponseDto.class
        );

        assertNotNull(actual);
        assertEquals(ACCOMMODATION_TYPE, actual.type());
        assertEquals(ACCOMMODATION_SIZE, actual.size());
        assertEquals(0, DAILY_RATE.compareTo(actual.dailyRate()));
    }

    @Sql(
            scripts = {
                    "classpath:database/delete-all.sql",
                    "classpath:database/accommodation/add-amenity.sql",
                    "classpath:database/accommodation/add-address.sql",
                    "classpath:database/accommodation/add-accommodation.sql"
            },
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @WithMockUser(username = "admin", authorities = {"ADMIN"})
    @Test
    @DisplayName("Find accommodation by ID - existing ID, returns accommodation")
    void findById_ExistingId_ReturnsAccommodation() throws Exception {
        // When & Then
        mockMvc.perform(get("/accommodations/{id}", DEFAULT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(DEFAULT_ID))
                .andExpect(jsonPath("$.type").value(ACCOMMODATION_TYPE.name()))
                .andExpect(jsonPath("$.size").value(ACCOMMODATION_SIZE))
                .andExpect(jsonPath("$.dailyRate").value(DAILY_RATE.doubleValue()))
                .andExpect(jsonPath("$.availability").value(AVAILABILITY));
    }

    @WithMockUser(username = "admin", authorities = {"ADMIN"})
    @Test
    @DisplayName("Find accommodation by ID - non-existing ID, returns not found")
    void findById_NonExistingId_ThrowsEntityNotFoundException() throws Exception {
        // When & Then
        mockMvc.perform(get("/accommodations/{id}", NONEXISTED_ID))
                .andExpect(status().isNotFound());
    }

    @Sql(
            scripts = {
                    "classpath:database/delete-all.sql",
                    "classpath:database/accommodation/add-amenity.sql",
                    "classpath:database/accommodation/add-address.sql",
                    "classpath:database/accommodation/add-accommodation.sql"
            },
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @WithMockUser(username = "admin", authorities = {"ADMIN"})
    @Test
    @DisplayName("Update accommodation - valid request, returns updated accommodation")
    void updateById_ValidRequest_ReturnsUpdatedAccommodation() throws Exception {
        // Given
        AccommodationUpdateRequestDto request = getAccommodationUpdateRequest();
        String jsonRequest = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(
                        put("/accommodations/{id}", DEFAULT_ID)
                                .content(jsonRequest)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(DEFAULT_ID))
                .andExpect(jsonPath("$.type").value(request.getType().name()))
                .andExpect(jsonPath("$.size").value(request.getSize()))
                .andExpect(jsonPath("$.dailyRate")
                        .value(request.getDailyRate().doubleValue()))
                .andExpect(jsonPath("$.availability")
                        .value(request.getAvailability()))
                .andExpect(jsonPath("$.amenities.length()")
                        .value(request.getAmenities().size()))
                .andExpect(jsonPath("$.amenities[0].id")
                        .value(request.getAmenities().getFirst()));
    }

    @WithMockUser(username = "admin", authorities = {"ADMIN"})
    @Test
    @DisplayName("Update accommodation - non-existing ID, returns not found")
    void updateById_NonExistingId_ReturnsNotFound() throws Exception {
        // Given
        AccommodationUpdateRequestDto request = getAccommodationUpdateRequest();
        String jsonRequest = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(
                        put("/accommodations/{id}", NONEXISTED_ID)
                                .content(jsonRequest)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNotFound());
    }

    @WithMockUser(username = "operator", authorities = {"OPERATOR"})
    @Test
    @DisplayName("Update accommodation - non-admin user, returns forbidden")
    void updateById_NonAdminUser_ReturnsForbidden() throws Exception {
        // Given
        AccommodationUpdateRequestDto request = getAccommodationUpdateRequest();
        String jsonRequest = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(
                        put("/accommodations/{id}", DEFAULT_ID)
                                .content(jsonRequest)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isForbidden());
    }

    @Sql(
            scripts = {
                    "classpath:database/delete-all.sql",
                    "classpath:database/accommodation/add-amenity.sql",
                    "classpath:database/accommodation/add-address.sql",
                    "classpath:database/accommodation/add-accommodation.sql"
            },
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @WithMockUser(username = "admin", authorities = {"ADMIN"})
    @Test
    @DisplayName("Patch accommodation - partial update, preserves other fields")
    void patchById_PartialRequest_UpdatesOnlyProvidedFields() throws Exception {
        // Given
        AccommodationPatchRequestDto request = new AccommodationPatchRequestDto();
        BigDecimal expectedDailyRate = BigDecimal.TEN;
        int expectedAvailability = 3;
        request.setDailyRate(expectedDailyRate);
        request.setAvailability(expectedAvailability);

        String jsonRequest = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(
                        patch("/accommodations/{id}", DEFAULT_ID)
                                .content(jsonRequest)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(DEFAULT_ID))
                .andExpect(jsonPath("$.dailyRate").value(expectedDailyRate))
                .andExpect(jsonPath("$.availability").value(expectedAvailability))
                .andExpect(jsonPath("$.type").value(ACCOMMODATION_TYPE.name()))
                .andExpect(jsonPath("$.size").value(ACCOMMODATION_SIZE));
    }

    @WithMockUser(username = "admin", authorities = {"ADMIN"})
    @Test
    @DisplayName("Patch accommodation - non-existing ID, returns not found")
    void patchById_NonExistingId_ReturnsNotFound() throws Exception {
        // Given

        AccommodationPatchRequestDto request = new AccommodationPatchRequestDto();
        request.setDailyRate(DAILY_RATE);

        String jsonRequest = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(
                        patch("/accommodations/{id}", NONEXISTED_ID)
                                .content(jsonRequest)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNotFound());
    }

    @WithMockUser(username = "operator", authorities = {"OPERATOR"})
    @Test
    @DisplayName("Patch accommodation - non-admin user, returns forbidden")
    void patchById_NonAdminUser_ReturnsForbidden() throws Exception {
        // Given
        AccommodationPatchRequestDto request = new AccommodationPatchRequestDto();
        request.setDailyRate(DAILY_RATE);

        String jsonRequest = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(
                        patch("/accommodations/{id}", DEFAULT_ID)
                                .content(jsonRequest)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isForbidden());
    }

    @Sql(
            scripts = {
                    "classpath:database/delete-all.sql",
                    "classpath:database/accommodation/add-amenity.sql",
                    "classpath:database/accommodation/add-address.sql",
                    "classpath:database/accommodation/add-accommodation.sql"
            },
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @WithMockUser(username = "admin", authorities = {"ADMIN"})
    @Test
    @DisplayName("Delete accommodation - existing ID, deletes accommodation")
    void delete_ExistingId_NoContent() throws Exception {
        // When & Then
        mockMvc.perform(delete("/accommodations/{id}", DEFAULT_ID))
                .andExpect(status().isNoContent());

        assertTrue(accommodationRepository.findById(DEFAULT_ID).isEmpty());
    }

    @WithMockUser(username = "admin", authorities = {"ADMIN"})
    @Test
    @DisplayName("Delete accommodation - non-existing ID, returns not found")
    void delete_NonExistingId_NotFound() throws Exception {
        // When & Then
        mockMvc.perform(delete("/accommodations/{id}", NONEXISTED_ID))
                .andExpect(status().isNotFound());
    }

    @WithMockUser(username = "operator", authorities = {"OPERATOR"})
    @Test
    @DisplayName("Delete accommodation - non-admin user, returns forbidden")
    void delete_NonAdminUser_Forbidden() throws Exception {
        // When & Then
        mockMvc.perform(delete("/accommodations/{id}", NONEXISTED_ID))
                .andExpect(status().isForbidden());
    }

    private AccommodationUpdateRequestDto getAccommodationUpdateRequest() {
        AccommodationUpdateRequestDto request = new AccommodationUpdateRequestDto();
        request.setType(Accommodation.Type.APARTMENT);
        request.setLocation(getAddressRequest());
        request.setSize(UPDATED_SIZE);
        request.setAmenities(List.of(AMENITY_ID));
        request.setDailyRate(UPDATED_DAILY_RATE);
        request.setAvailability(3);
        return request;
    }

    private AccommodationRequestDto getAccommodationRequest() {
        AccommodationRequestDto request = new AccommodationRequestDto();
        request.setType(ACCOMMODATION_TYPE);
        request.setLocation(getAddressRequest());
        request.setSize(ACCOMMODATION_SIZE);
        request.setAmenities(List.of(AMENITY_ID));
        request.setDailyRate(DAILY_RATE);
        request.setAvailability(AVAILABILITY);
        return request;
    }

    private AddressRequestDto getAddressRequest() {
        AddressRequestDto request = new AddressRequestDto();
        request.setStreetLineOne(STREET_LINE_ONE);
        request.setStreetLineTwo(STREET_LINE_TWO);
        request.setPostalCode(POSTAL_CODE);
        request.setCity(CITY);
        request.setRegion(REGION);
        request.setRoomNumber(ROOM_NUMBER);
        request.setLatitude(LATITUDE);
        request.setLongitude(LONGITUDE);
        return request;
    }

}