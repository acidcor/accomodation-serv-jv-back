package mate.academy.accommodationbookingservice.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mate.academy.accommodationbookingservice.dto.accomnodation.AccommodationPatchRequestDto;
import mate.academy.accommodationbookingservice.dto.accomnodation.AccommodationRequestDto;
import mate.academy.accommodationbookingservice.dto.accomnodation.AccommodationResponseDto;
import mate.academy.accommodationbookingservice.dto.accomnodation.AccommodationShortResponseDto;
import mate.academy.accommodationbookingservice.dto.accomnodation.AccommodationUpdateRequestDto;
import mate.academy.accommodationbookingservice.service.AccommodationService;

@Tag(
        name = "Accommodations",
        description = "Provide all crude methods for accommodation entity")
@RequiredArgsConstructor
@RestController
@RequestMapping("/accommodations")
public class AccommodationController {
    private final AccommodationService accommodationService;

    @Operation(summary = "Create an accommodation")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccommodationResponseDto saveAccommodation(
            @Valid @RequestBody AccommodationRequestDto request) {
        return accommodationService.save(request);
    }

    @Operation(summary = "Select all accommodations")
    @GetMapping
    public Page<AccommodationShortResponseDto> findAllAccommodation(Pageable pageable) {
        return accommodationService.findAll(pageable);
    }

    @Operation(summary = "Select single accommodation by ID")
    @GetMapping("/{id}")
    public AccommodationResponseDto findByIdAccommodation(@PathVariable Long id) {
        return accommodationService.findById(id);
    }

    @Operation(summary = "Update single accommodation by ID")
    @PutMapping("/{id}")
    public AccommodationResponseDto updateByIdAccommodation(
            @PathVariable Long id,
            @RequestBody @Valid AccommodationUpdateRequestDto request) {
        return accommodationService.updateById(id, request);
    }

    @Operation(summary = "Patch single accommodation by ID")
    @PatchMapping("/{id}")
    public AccommodationResponseDto patchByIdAccommodation(
            @PathVariable Long id,
            @RequestBody @Valid AccommodationPatchRequestDto request) {
        return accommodationService.patchById(id, request);
    }

    @Operation(summary = "Delete single accommodation by ID")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccommodation(@PathVariable Long id) {
        accommodationService.delete(id);
    }
}
