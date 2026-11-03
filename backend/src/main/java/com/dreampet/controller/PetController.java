package com.dreampet.controller;

import com.dreampet.common.ApiResponse;
import com.dreampet.common.UserContext;
import com.dreampet.dto.PetDtos;
import com.dreampet.service.PetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pets")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;

    @PostMapping
    public ApiResponse<PetDtos.PetDTO> create(@Valid @RequestBody PetDtos.CreatePetRequest req) {
        return ApiResponse.ok(petService.create(UserContext.get(), req));
    }

    @GetMapping("/current")
    public ApiResponse<PetDtos.PetDTO> current() {
        return ApiResponse.ok(petService.current(UserContext.get()));
    }

    @GetMapping("/{id}")
    public ApiResponse<PetDtos.PetDTO> getById(@PathVariable Long id) {
        return ApiResponse.ok(petService.getById(UserContext.get(), id));
    }

    @PutMapping("/{id}")
    public ApiResponse<PetDtos.PetDTO> update(@PathVariable Long id,
                                              @Valid @RequestBody PetDtos.UpdatePetRequest req) {
        return ApiResponse.ok(petService.update(UserContext.get(), id, req));
    }
}